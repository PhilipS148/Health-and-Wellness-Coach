package io.github.philips148.wellnesscoach.data.wellness

import io.github.philips148.wellnesscoach.data.model.CheckInEntry
import io.github.philips148.wellnesscoach.data.model.MealEntry
import io.github.philips148.wellnesscoach.data.model.SleepEntry
import io.github.philips148.wellnesscoach.data.model.WaterEntry
import io.github.philips148.wellnesscoach.data.model.WorkoutEntry

// All operations apply to the currently signed-in user.
// Dates use yyyy-MM-dd. Date ranges include both endpoints.
// Create functions return the new record's ID.
// Update functions require an existing record's ID in the entry.
interface WellnessRepository {

    // Meals
    suspend fun createMeal(entry: MealEntry): Result<String>

    suspend fun getMeals(
        startDate: String,
        endDate: String
    ): Result<List<MealEntry>>

    suspend fun updateMeal(entry: MealEntry): Result<Unit>

    suspend fun deleteMeal(id: String): Result<Unit>

    // Hydration
    suspend fun createWaterEntry(entry: WaterEntry): Result<String>

    suspend fun getWaterEntries(
        startDate: String,
        endDate: String
    ): Result<List<WaterEntry>>

    suspend fun updateWaterEntry(entry: WaterEntry): Result<Unit>

    suspend fun deleteWaterEntry(id: String): Result<Unit>

    // Workouts
    suspend fun createWorkout(entry: WorkoutEntry): Result<String>

    suspend fun getWorkouts(
        startDate: String,
        endDate: String
    ): Result<List<WorkoutEntry>>

    suspend fun updateWorkout(entry: WorkoutEntry): Result<Unit>

    suspend fun deleteWorkout(id: String): Result<Unit>

    // Sleep
    suspend fun createSleepEntry(entry: SleepEntry): Result<String>

    suspend fun getSleepEntries(
        startDate: String,
        endDate: String
    ): Result<List<SleepEntry>>

    suspend fun updateSleepEntry(entry: SleepEntry): Result<Unit>

    suspend fun deleteSleepEntry(id: String): Result<Unit>

    // Daily wellness check-ins
    suspend fun createCheckIn(entry: CheckInEntry): Result<String>

    suspend fun getCheckIns(
        startDate: String,
        endDate: String
    ): Result<List<CheckInEntry>>

    suspend fun updateCheckIn(entry: CheckInEntry): Result<Unit>

    suspend fun deleteCheckIn(id: String): Result<Unit>
}