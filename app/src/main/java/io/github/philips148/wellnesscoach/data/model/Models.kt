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
    val sleepGoalHours: Double = 8.0,

    // Categories the user has chosen to track.
    val selectedCategories: List<String> = TrackingCategories.ALL
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

// Information recorded for one exercise set.
data class WorkoutSet(
    val repetitions: Int = 0,
    val weightLbs: Double? = null
)

data class WorkoutEntry(
    val id: String = "",
    val date: String = "",
    val workoutType: String = "",
    val exerciseName: String = "",

    // List order represents set 1, set 2, and so on.
    val sets: List<WorkoutSet> = emptyList(),

    // Used for manually recorded walks or runs.
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

// Recorded ratings use a 1–10 scale.
// Null means the user did not record that category.
data class CheckInEntry(
    val id: String = "",
    val date: String = "",
    val stressLevel: Int? = null,
    val mood: Int? = null,
    val energyLevel: Int? = null,
    val createdAt: Long = 0L
)

data class StepTotal(
    val id: String = "",
    val date: String = "",
    val steps: Int = 0
)