package io.github.philips148.wellnesscoach.ui.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.model.MealEntry
import io.github.philips148.wellnesscoach.data.wellness.WellnessRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MealTrackingUiState(
    val selectedDate: String = currentDate(),
    val mealName: String = "",
    val calories: String = "",
    val meals: List<MealEntry> = emptyList(),
    val dailyCalories: Int = 0,
    val editingMealId: String? = null,
    val editingCreatedAt: Long = 0L,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

private fun currentDate(): String {
    return SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.US
    ).format(Date())
}

class MealTrackingViewModel(
    private val wellnessRepository: WellnessRepository
) : ViewModel() {

    var uiState = androidx.compose.runtime.mutableStateOf(
        MealTrackingUiState()
    )
        private set

    init {
        loadMeals()
    }

    fun onDateChange(value: String) {
        uiState.value = uiState.value.copy(
            selectedDate = value,
            errorMessage = null
        )
    }

    fun onMealNameChange(value: String) {
        uiState.value = uiState.value.copy(
            mealName = value,
            errorMessage = null
        )
    }

    fun onCaloriesChange(value: String) {
        uiState.value = uiState.value.copy(
            calories = value.filter { it.isDigit() },
            errorMessage = null
        )
    }

    fun loadMeals() {
        viewModelScope.launch {
            refreshMeals()
        }
    }

    fun saveMeal() {
        val state = uiState.value

        if (state.mealName.isBlank()) {
            uiState.value = state.copy(
                errorMessage = "Meal name or description is required."
            )
            return
        }

        val calorieValue = state.calories.toIntOrNull()

        if (calorieValue == null || calorieValue <= 0) {
            uiState.value = state.copy(
                errorMessage = "Enter a valid calorie amount."
            )
            return
        }

        if (!isValidDate(state.selectedDate)) {
            uiState.value = state.copy(
                errorMessage = "Date must use yyyy-MM-dd."
            )
            return
        }

        viewModelScope.launch {
            uiState.value = uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            val editingId = state.editingMealId

            val result =
                if (editingId == null) {
                    wellnessRepository.createMeal(
                        MealEntry(
                            date = state.selectedDate,
                            mealName = state.mealName.trim(),
                            calories = calorieValue,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    wellnessRepository.updateMeal(
                        MealEntry(
                            id = editingId,
                            date = state.selectedDate,
                            mealName = state.mealName.trim(),
                            calories = calorieValue,
                            createdAt = state.editingCreatedAt
                        )
                    )
                }

            result
                .onSuccess {
                    clearForm()
                    refreshMeals()
                }
                .onFailure { error ->
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        errorMessage =
                            error.message ?: "Could not save meal."
                    )
                }
        }
    }

    fun editMeal(meal: MealEntry) {
        uiState.value = uiState.value.copy(
            selectedDate = meal.date,
            mealName = meal.mealName,
            calories = meal.calories.toString(),
            editingMealId = meal.id,
            editingCreatedAt = meal.createdAt,
            errorMessage = null
        )
    }

    fun cancelEdit() {
        clearForm()
    }

    fun deleteMeal(id: String) {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            wellnessRepository.deleteMeal(id)
                .onSuccess {
                    refreshMeals()
                }
                .onFailure { error ->
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        errorMessage =
                            error.message ?: "Could not delete meal."
                    )
                }
        }
    }

    private suspend fun refreshMeals() {
        val date = uiState.value.selectedDate

        if (!isValidDate(date)) {
            uiState.value = uiState.value.copy(
                isLoading = false,
                errorMessage = "Date must use yyyy-MM-dd."
            )
            return
        }

        uiState.value = uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        wellnessRepository.getMeals(
            startDate = date,
            endDate = date
        )
            .onSuccess { meals ->
                uiState.value = uiState.value.copy(
                    meals = meals,
                    dailyCalories = meals.sumOf { it.calories },
                    isLoading = false
                )
            }
            .onFailure { error ->
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    errorMessage =
                        error.message ?: "Could not load meals."
                )
            }
    }

    private fun clearForm() {
        uiState.value = uiState.value.copy(
            mealName = "",
            calories = "",
            editingMealId = null,
            editingCreatedAt = 0L,
            errorMessage = null
        )
    }

    private fun isValidDate(date: String): Boolean {
        return try {
            val formatter = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            )
            formatter.isLenient = false
            formatter.parse(date)
            true
        } catch (_: Exception) {
            false
        }
    }
}