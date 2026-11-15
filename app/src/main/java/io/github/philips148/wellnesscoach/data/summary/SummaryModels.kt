package io.github.philips148.wellnesscoach.data.summary

data class DailySummary(
    val date: String,

    val selectedCategories: List<String> = emptyList(),

    val calories: Int = 0,
    val calorieGoal: Int = 0,
    val calorieProgress: Double = 0.0,

    val waterOz: Int = 0,
    val waterGoalOz: Int = 0,
    val waterProgress: Double = 0.0,

    // Null means no step record exists for the date.
    val steps: Int? = null,
    val stepGoal: Int = 0,
    val stepProgress: Double? = null,

    // Null means no sleep record exists for the date.
    val sleepHours: Double? = null,
    val sleepGoalHours: Double = 0.0,
    val sleepProgress: Double? = null,

    val workoutCount: Int = 0,
    val workoutMinutes: Int = 0,

    // Null means the user did not record that rating.
    val stressLevel: Int? = null,
    val mood: Int? = null,
    val energyLevel: Int? = null
)

data class WeeklySummary(
    val startDate: String,
    val endDate: String,
    val days: List<DailySummary> = emptyList(),

    val averageDailyCalories: Double = 0.0,
    val averageDailyWaterOz: Double = 0.0,

    // Calculated only from dates that actually contain records.
    val averageDailySteps: Double? = null,
    val averageSleepHours: Double? = null,

    val totalWorkouts: Int = 0,
    val totalWorkoutMinutes: Int = 0,

    val averageStressLevel: Double? = null,
    val averageMood: Double? = null,
    val averageEnergyLevel: Double? = null
)