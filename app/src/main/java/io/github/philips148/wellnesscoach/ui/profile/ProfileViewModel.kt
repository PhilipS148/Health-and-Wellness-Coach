package io.github.philips148.wellnesscoach.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import io.github.philips148.wellnesscoach.data.model.UserProfile
import io.github.philips148.wellnesscoach.data.user.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    isSetupFlow: Boolean = false
) : ViewModel() {

    private var originalProfile: UserProfile? = null

    var uiState by mutableStateOf(
        ProfileUiState(isSetupFlow = isSetupFlow)
    )
        private set

    init {
        loadProfile()
    }

    fun loadProfile() {
        if (uiState.isLoading || uiState.isSaving) {
            return
        }

        uiState = uiState.copy(
            isLoading = true,
            loadFailed = false,
            isSetupComplete = false,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = userRepository.getProfile()

            result.fold(
                onSuccess = { profile ->
                    originalProfile = profile

                    uiState = uiState.copy(
                        username = profile?.displayName.orEmpty(),
                        heightCm = profile?.heightCm?.toString().orEmpty(),
                        weightKg = profile?.weightKg?.toString().orEmpty(),
                        isNewProfile = profile == null,
                        isLoading = false,
                        loadFailed = false,
                        errorMessage = null,
                        successMessage = null
                    )
                },
                onFailure = {
                    uiState = uiState.copy(
                        isLoading = false,
                        loadFailed = true,
                        errorMessage =
                            "Couldn't load your profile. Please try again.",
                        successMessage = null
                    )
                }
            )
        }
    }

    fun onUsernameChange(value: String) {
        uiState = uiState.copy(
            username = value,
            isSetupComplete = false,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onHeightChange(value: String) {
        uiState = uiState.copy(
            heightCm = value,
            isSetupComplete = false,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onWeightChange(value: String) {
        uiState = uiState.copy(
            weightKg = value,
            isSetupComplete = false,
            errorMessage = null,
            successMessage = null
        )
    }

    fun validateProfile(): Boolean {
        if (uiState.isLoading || uiState.isSaving || uiState.loadFailed) {
            return false
        }

        val username = uiState.username.trim()
        val heightText = uiState.heightCm.trim()
        val weightText = uiState.weightKg.trim()

        val height = heightText.toDoubleOrNull()
        val weight = weightText.toDoubleOrNull()

        val validationError = when {
            username.isBlank() ->
                "Username is required."

            heightText.isNotEmpty() &&
                    (height == null || !height.isFinite() || height <= 0) ->
                "Height must be a positive number in centimeters."

            weightText.isNotEmpty() &&
                    (weight == null || !weight.isFinite() || weight <= 0) ->
                "Weight must be a positive number in kilograms."

            else -> null
        }

        uiState = uiState.copy(
            errorMessage = validationError,
            successMessage = null
        )

        return validationError == null
    }

    fun saveProfile() {
        if (!validateProfile()) {
            return
        }

        val uid = authRepository.getCurrentUserId()

        if (uid.isNullOrBlank()) {
            uiState = uiState.copy(
                errorMessage = "Please sign in again to save your profile.",
                successMessage = null
            )
            return
        }

        val existingProfile = originalProfile

        if (existingProfile != null && existingProfile.uid != uid) {
            uiState = uiState.copy(
                errorMessage =
                    "Your account changed. Please reopen your profile.",
                successMessage = null
            )
            return
        }

        val baseProfile: UserProfile

        if (existingProfile == null) {
            val email = authRepository.getCurrentUserEmail()

            if (email.isNullOrBlank()) {
                uiState = uiState.copy(
                    errorMessage =
                        "Couldn't read your account email. Please sign in again.",
                    successMessage = null
                )
                return
            }

            baseProfile = UserProfile(
                uid = uid,
                email = email
            )
        } else {
            baseProfile = existingProfile
        }

        val creatingProfile = existingProfile == null

        val updatedProfile = baseProfile.copy(
            displayName = uiState.username.trim(),
            heightCm = uiState.heightCm.trim().toDoubleOrNull(),
            weightKg = uiState.weightKg.trim().toDoubleOrNull()
        )

        uiState = uiState.copy(
            isSaving = true,
            isSetupComplete = false,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = userRepository.saveProfile(updatedProfile)

            result.fold(
                onSuccess = {
                    originalProfile = updatedProfile

                    uiState = uiState.copy(
                        username = updatedProfile.displayName,
                        isSaving = false,
                        isNewProfile = false,
                        isSetupComplete = uiState.isSetupFlow,
                        errorMessage = null,
                        successMessage = if (creatingProfile) {
                            "Your profile was created."
                        } else {
                            "Your changes were saved."
                        }
                    )
                },
                onFailure = {
                    uiState = uiState.copy(
                        isSaving = false,
                        isSetupComplete = false,
                        errorMessage =
                            "Couldn't save your profile. Please try again.",
                        successMessage = null
                    )
                }
            )
        }
    }
}