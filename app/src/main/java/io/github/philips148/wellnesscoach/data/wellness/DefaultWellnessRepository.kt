package io.github.philips148.wellnesscoach.data.wellness

import io.github.philips148.wellnesscoach.data.auth.FirebaseAuthDataSource
import io.github.philips148.wellnesscoach.data.model.CheckInEntry
import io.github.philips148.wellnesscoach.data.model.MealEntry
import io.github.philips148.wellnesscoach.data.model.SleepEntry
import io.github.philips148.wellnesscoach.data.model.WaterEntry
import io.github.philips148.wellnesscoach.data.model.WorkoutEntry

class DefaultWellnessRepository(
    private val authDataSource: FirebaseAuthDataSource,
    private val wellnessDataSource: FirestoreWellnessDataSource
) : WellnessRepository {

    override suspend fun createMeal(
        entry: MealEntry
    ): Result<String> = safeCall {
        wellnessDataSource.createMeal(
            requireUserId(),
            entry
        )
    }

    override suspend fun getMeals(
        startDate: String,
        endDate: String
    ): Result<List<MealEntry>> = safeCall {
        wellnessDataSource.getMeals(
            requireUserId(),
            startDate,
            endDate
        )
    }

    override suspend fun updateMeal(
        entry: MealEntry
    ): Result<Unit> = safeCall {
        wellnessDataSource.updateMeal(
            requireUserId(),
            entry
        )
    }

    override suspend fun deleteMeal(
        id: String
    ): Result<Unit> = safeCall {
        wellnessDataSource.deleteMeal(
            requireUserId(),
            id
        )
    }


    override suspend fun createWaterEntry(
        entry: WaterEntry
    ): Result<String> = safeCall {
        wellnessDataSource.createWaterEntry(
            requireUserId(),
            entry
        )
    }

    override suspend fun getWaterEntries(
        startDate: String,
        endDate: String
    ): Result<List<WaterEntry>> = safeCall {
        wellnessDataSource.getWaterEntries(
            requireUserId(),
            startDate,
            endDate
        )
    }

    override suspend fun updateWaterEntry(
        entry: WaterEntry
    ): Result<Unit> = safeCall {
        wellnessDataSource.updateWaterEntry(
            requireUserId(),
            entry
        )
    }

    override suspend fun deleteWaterEntry(
        id: String
    ): Result<Unit> = safeCall {
        wellnessDataSource.deleteWaterEntry(
            requireUserId(),
            id
        )
    }


    override suspend fun createWorkout(
        entry: WorkoutEntry
    ): Result<String> = safeCall {
        wellnessDataSource.createWorkout(
            requireUserId(),
            entry
        )
    }

    override suspend fun getWorkouts(
        startDate: String,
        endDate: String
    ): Result<List<WorkoutEntry>> = safeCall {
        wellnessDataSource.getWorkouts(
            requireUserId(),
            startDate,
            endDate
        )
    }

    override suspend fun updateWorkout(
        entry: WorkoutEntry
    ): Result<Unit> = safeCall {
        wellnessDataSource.updateWorkout(
            requireUserId(),
            entry
        )
    }

    override suspend fun deleteWorkout(
        id: String
    ): Result<Unit> = safeCall {
        wellnessDataSource.deleteWorkout(
            requireUserId(),
            id
        )
    }


    override suspend fun createSleepEntry(
        entry: SleepEntry
    ): Result<String> = safeCall {
        wellnessDataSource.createSleepEntry(
            requireUserId(),
            entry
        )
    }

    override suspend fun getSleepEntries(
        startDate: String,
        endDate: String
    ): Result<List<SleepEntry>> = safeCall {
        wellnessDataSource.getSleepEntries(
            requireUserId(),
            startDate,
            endDate
        )
    }

    override suspend fun updateSleepEntry(
        entry: SleepEntry
    ): Result<Unit> = safeCall {
        wellnessDataSource.updateSleepEntry(
            requireUserId(),
            entry
        )
    }

    override suspend fun deleteSleepEntry(
        id: String
    ): Result<Unit> = safeCall {
        wellnessDataSource.deleteSleepEntry(
            requireUserId(),
            id
        )
    }


    override suspend fun createCheckIn(
        entry: CheckInEntry
    ): Result<String> = safeCall {
        wellnessDataSource.createCheckIn(
            requireUserId(),
            entry
        )
    }

    override suspend fun getCheckIns(
        startDate: String,
        endDate: String
    ): Result<List<CheckInEntry>> = safeCall {
        wellnessDataSource.getCheckIns(
            requireUserId(),
            startDate,
            endDate
        )
    }

    override suspend fun updateCheckIn(
        entry: CheckInEntry
    ): Result<Unit> = safeCall {
        wellnessDataSource.updateCheckIn(
            requireUserId(),
            entry
        )
    }

    override suspend fun deleteCheckIn(
        id: String
    ): Result<Unit> = safeCall {
        wellnessDataSource.deleteCheckIn(
            requireUserId(),
            id
        )
    }


    private fun requireUserId(): String {
        return authDataSource.getCurrentUserId()
            ?: throw IllegalStateException(
                "A signed-in user is required."
            )
    }

    private suspend fun <T> safeCall(
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}