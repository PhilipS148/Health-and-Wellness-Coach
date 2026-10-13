package io.github.philips148.wellnesscoach.data.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val wellnessGoals: List<String> = emptyList(),
    val dailyCalorieGoal: Int = 2000,
    val dailyWaterGoalOz: Int = 64,
    val dailyStepGoal: Int = 5000,
    val sleepGoalHours: Double = 8.0
)

data class MealEntry(
    val id: String = "",
    val date: String = "",
    val mealName: String = "",
    val mealType: String = "",
    val calories: Int = 0,
    val mealTime: String = "",
    val createdAt: Long = 0L
)

data class WaterEntry(
    val id: String = "",
    val date: String = "",
    val amountOz: Int = 0,
    val createdAt: Long = 0L
)

data class WorkoutEntry(
    val id: String = "",
    val date: String = "",
    val workoutType: String = "",
    val exerciseName: String = "",
    val numSets: Int? = null,
    val repetitions: Int? = null,
    val weightLbs: Double? = null,
    val distanceMiles: Double? = null,
    val durationMinutes: Int? = null,
    val createdAt: Long = 0L
)

data class SleepEntry(
    val id: String = "",
    val date: String = "",
    val hoursSlept: Double = 0.0,
    val createdAt: Long = 0L
)

// Levels are on a scale of 1-10, defaulted to 5
data class CheckInEntry(
    val id: String = "",
    val date: String = "",
    val stressLevel: Int = 5,
    val mood: Int = 5,
    val energyLevel: Int = 5,
    val createdAt: Long = 0L
)

data class StepTotal(
    val id: String = "",
    val date: String = "",
    val steps: Int = 0
)