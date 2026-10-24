package io.github.philips148.wellnesscoach.ui.goals

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import io.github.philips148.wellnesscoach.data.model.TrackingCategories
import io.github.philips148.wellnesscoach.data.model.UserProfile
import io.github.philips148.wellnesscoach.data.user.UserRepository
import io.github.philips148.wellnesscoach.util.GoalsValidator
import kotlinx.coroutines.launch

data class GoalSettingsUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val calorieGoal: String = "",
    val waterGoal: String = "",
    val stepGoal: String = "",
    val sleepGoal: String = "",
    val selectedCategories: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccessful: Boolean = false
)

class GoalSettingsViewModel(
    private val userRepository: UserRepository): ViewModel() {
    var uiState by mutableStateOf(value= GoalSettingsUiState())
        private set

    private var loadedProfile: UserProfile? = null

    init {
        loadProfile()
    }

    fun loadProfile() {
        uiState = uiState.copy(
            isLoading = true,
            loadError = null
        )


        viewModelScope.launch {
            val result = userRepository.getProfile()
            if (result.isFailure) {
                uiState = uiState.copy(
                    isLoading = false,
                    loadError = "Could not load goals, please try again"
                )
                return@launch
            }
            val profile = result.getOrNull()
            if (profile == null) {
                uiState = uiState.copy(
                    isLoading = false,
                    loadError = "Profile not found"
                )
                return@launch
            }
            loadedProfile = profile
            uiState = uiState.copy(
                isLoading = false,
                calorieGoal = profile.dailyCalorieGoal.toString(),
                waterGoal = profile.dailyWaterGoalOz.toString(),
                stepGoal = profile.dailyStepGoal.toString(),
                sleepGoal = profile.sleepGoalHours.toString(),
                selectedCategories = profile.selectedCategories.toSet()
            )

        }

        fun onCalorieGoalChange(value: String) {
            uiState = uiState.copy(
                calorieGoal = value,
                errorMessage = null,
                saveSuccessful = false
            )
        }
        fun onWaterGoalChange(value: String) {
            uiState = uiState.copy(
                waterGoal = value,
                errorMessage = null,
                saveSuccessful = false
            )
        }
        fun onStepGoalChange(value: String) {
            uiState = uiState.copy(
                stepGoal = value,
                errorMessage = null,
                saveSuccessful = false
            )
        }
        fun onSleepGoalChange(value: String) {
            uiState = uiState.copy(
                sleepGoal = value,
                errorMessage = null,
                saveSuccessful = false
            )
        }

        fun onCategoryToggle(categoryId: String) {
            val updated = mutableSetOf<String>()
            var wasSelected = false

            for (selected in uiState.selectedCategories) {
                if (selected == categoryId) {
                    wasSelected = true
                } else {
                    updated.add(selected)
                }
            }

            if(!wasSelected) {
                updated.add(categoryId)
            }

            uiState = uiState.copy(
                selectedCategories = updated,
                errorMessage = null,
                saveSuccessful = false
            )
        }

        fun save() {
            if (uiState.isLoading || uiState.isSaving) {
                return
            }
            var profile = loadedProfile ?: return

            val validationError =
                GoalsValidator.wholeNumberGoalError(uiState.calorieGoal, "Daily calorie goal")
                    ?: GoalsValidator.wholeNumberGoalError(uiState.waterGoal, "Daily water goal")
                    ?: GoalsValidator.wholeNumberGoalError(uiState.stepGoal, "Daily step goal")
                    ?: GoalsValidator.sleepGoalError(uiState.sleepGoal)

            if (validationError != null) {
                uiState = uiState.copy(errorMessage = validationError)
                return
            }

            val updatedProfile = profile.copy(
                dailyCalorieGoal = uiState.calorieGoal.trim().toInt(),
                sleepGoalHours = uiState.sleepGoal.trim().toDouble(),
                selectedCategories = TrackingCategories.ALL.filter{it in uiState.selectedCategories}
            )

            uiState = uiState.copy(
                isSaving = true,
                errorMessage = null,
                saveSuccessful =  false
            )

            viewModelScope.launch {
                val result = userRepository.saveProfile(updatedProfile)
                if (result.isFailure) {
                    val message = if (result.exceptionOrNull() is FirebaseNetworkException) {
                        "No internet connection."
                    } else {
                        "Couldn't save your goals. Please try again."
                    }
                    uiState = uiState.copy(
                        isSaving = false,
                        errorMessage = message
                    )
                    return@launch

                }
                loadedProfile = updatedProfile
                uiState = uiState.copy(
                    isSaving = false,
                    saveSuccessful = true
                )
            }
        }


    }
}