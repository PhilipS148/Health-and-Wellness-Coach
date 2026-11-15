package io.github.philips148.wellnesscoach.data.summary

import io.github.philips148.wellnesscoach.data.model.CheckInEntry
import io.github.philips148.wellnesscoach.data.model.MealEntry
import io.github.philips148.wellnesscoach.data.model.SleepEntry
import io.github.philips148.wellnesscoach.data.model.StepTotal
import io.github.philips148.wellnesscoach.data.model.UserProfile
import io.github.philips148.wellnesscoach.data.model.WaterEntry
import io.github.philips148.wellnesscoach.data.model.WorkoutEntry
import io.github.philips148.wellnesscoach.data.steps.StepRepository
import io.github.philips148.wellnesscoach.data.user.UserRepository
import io.github.philips148.wellnesscoach.data.wellness.WellnessRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultSummaryRepositoryTest {

    private val profile = UserProfile(
        uid = "test-user",
        dailyCalorieGoal = 2000,
        dailyWaterGoalOz = 64,
        dailyStepGoal = 5000,
        sleepGoalHours = 8.0,
        selectedCategories = listOf(
            "Meals",
            "Hydration",
            "Steps",
            "Sleep"
        )
    )

    @Test
    fun dailySummary_calculatesTotalsAndProgress() = runBlocking {
        val wellnessRepository = FakeWellnessRepository(
            meals = listOf(
                MealEntry(
                    id = "meal1",
                    date = "2026-11-14",
                    mealName = "Breakfast",
                    calories = 500
                ),
                MealEntry(
                    id = "meal2",
                    date = "2026-11-14",
                    mealName = "Lunch",
                    calories = 300
                )
            ),
            water = listOf(
                WaterEntry(
                    id = "water1",
                    date = "2026-11-14",
                    amountOz = 20
                ),
                WaterEntry(
                    id = "water2",
                    date = "2026-11-14",
                    amountOz = 28
                )
            ),
            workouts = listOf(
                WorkoutEntry(
                    id = "workout1",
                    date = "2026-11-14",
                    workoutType = "Cardio",
                    durationMinutes = 30
                ),
                WorkoutEntry(
                    id = "workout2",
                    date = "2026-11-14",
                    workoutType = "Strength",
                    durationMinutes = 20
                )
            ),
            sleep = listOf(
                SleepEntry(
                    id = "sleep1",
                    date = "2026-11-14",
                    hoursSlept = 7.5
                )
            ),
            checkIns = listOf(
                CheckInEntry(
                    id = "check1",
                    date = "2026-11-14",
                    stressLevel = 4,
                    mood = 8,
                    energyLevel = 7,
                    createdAt = 100L
                )
            )
        )

        val stepRepository = FakeStepRepository(
            totals = listOf(
                StepTotal(
                    id = "2026-11-14",
                    date = "2026-11-14",
                    steps = 4000
                )
            )
        )

        val repository = DefaultSummaryRepository(
            userRepository = FakeUserRepository(profile),
            wellnessRepository = wellnessRepository,
            stepRepository = stepRepository
        )

        val summary = repository
            .getDailySummary("2026-11-14")
            .getOrThrow()

        assertEquals(800, summary.calories)
        assertEquals(48, summary.waterOz)
        assertEquals(4000, summary.steps)
        assertEquals(7.5, summary.sleepHours!!, 0.001)

        assertEquals(2, summary.workoutCount)
        assertEquals(50, summary.workoutMinutes)

        assertEquals(4, summary.stressLevel)
        assertEquals(8, summary.mood)
        assertEquals(7, summary.energyLevel)

        assertEquals(0.4, summary.calorieProgress, 0.001)
        assertEquals(0.75, summary.waterProgress, 0.001)
        assertEquals(0.8, summary.stepProgress!!, 0.001)
        assertEquals(0.9375, summary.sleepProgress!!, 0.001)
    }

    @Test
    fun missingStepRecord_remainsNull() = runBlocking {
        val repository = DefaultSummaryRepository(
            userRepository = FakeUserRepository(profile),
            wellnessRepository = FakeWellnessRepository(),
            stepRepository = FakeStepRepository()
        )

        val summary = repository
            .getDailySummary("2026-11-14")
            .getOrThrow()

        assertNull(summary.steps)
        assertNull(summary.stepProgress)
    }

    @Test
    fun missingSleepAndRatings_remainNull() = runBlocking {
        val repository = DefaultSummaryRepository(
            userRepository = FakeUserRepository(profile),
            wellnessRepository = FakeWellnessRepository(),
            stepRepository = FakeStepRepository()
        )

        val summary = repository
            .getDailySummary("2026-11-14")
            .getOrThrow()

        assertNull(summary.sleepHours)
        assertNull(summary.sleepProgress)
        assertNull(summary.stressLevel)
        assertNull(summary.mood)
        assertNull(summary.energyLevel)
    }

    @Test
    fun weeklySummary_usesSevenDayInclusiveRange() = runBlocking {
        val wellnessRepository = FakeWellnessRepository()
        val stepRepository = FakeStepRepository()

        val repository = DefaultSummaryRepository(
            userRepository = FakeUserRepository(profile),
            wellnessRepository = wellnessRepository,
            stepRepository = stepRepository
        )

        val summary = repository
            .getWeeklySummary("2026-11-10")
            .getOrThrow()

        assertEquals("2026-11-10", summary.startDate)
        assertEquals("2026-11-16", summary.endDate)
        assertEquals(7, summary.days.size)

        assertEquals("2026-11-10", summary.days.first().date)
        assertEquals("2026-11-16", summary.days.last().date)

        assertEquals(
            "2026-11-10",
            wellnessRepository.lastStartDate
        )

        assertEquals(
            "2026-11-16",
            wellnessRepository.lastEndDate
        )

        assertEquals(
            "2026-11-10",
            stepRepository.lastStartDate
        )

        assertEquals(
            "2026-11-16",
            stepRepository.lastEndDate
        )
    }

    @Test
    fun weeklySummary_averagesOnlyRecordedStepsAndSleep() = runBlocking {
        val wellnessRepository = FakeWellnessRepository(
            sleep = listOf(
                SleepEntry(
                    id = "sleep1",
                    date = "2026-11-10",
                    hoursSlept = 6.0
                ),
                SleepEntry(
                    id = "sleep2",
                    date = "2026-11-11",
                    hoursSlept = 8.0
                )
            )
        )

        val stepRepository = FakeStepRepository(
            totals = listOf(
                StepTotal(
                    id = "2026-11-10",
                    date = "2026-11-10",
                    steps = 1000
                ),
                StepTotal(
                    id = "2026-11-11",
                    date = "2026-11-11",
                    steps = 3000
                )
            )
        )

        val repository = DefaultSummaryRepository(
            userRepository = FakeUserRepository(profile),
            wellnessRepository = wellnessRepository,
            stepRepository = stepRepository
        )

        val summary = repository
            .getWeeklySummary("2026-11-10")
            .getOrThrow()

        assertEquals(
            2000.0,
            summary.averageDailySteps!!,
            0.001
        )

        assertEquals(
            7.0,
            summary.averageSleepHours!!,
            0.001
        )
    }

    @Test
    fun repeatedRefresh_doesNotAddStepTotalsTogether() = runBlocking {
        val stepRepository = FakeStepRepository(
            totals = listOf(
                StepTotal(
                    id = "2026-11-14",
                    date = "2026-11-14",
                    steps = 5000
                )
            )
        )

        val repository = DefaultSummaryRepository(
            userRepository = FakeUserRepository(profile),
            wellnessRepository = FakeWellnessRepository(),
            stepRepository = stepRepository
        )

        val first = repository
            .getDailySummary("2026-11-14")
            .getOrThrow()

        val second = repository
            .getDailySummary("2026-11-14")
            .getOrThrow()

        assertEquals(5000, first.steps)
        assertEquals(5000, second.steps)
    }
}

private class FakeUserRepository(
    private val profile: UserProfile?
) : UserRepository {

    override suspend fun getProfile(): Result<UserProfile?> {
        return Result.success(profile)
    }

    override suspend fun saveProfile(
        profile: UserProfile
    ): Result<Unit> {
        return Result.success(Unit)
    }
}

private class FakeStepRepository(
    private val totals: List<StepTotal> = emptyList()
) : StepRepository {

    var lastStartDate: String? = null
    var lastEndDate: String? = null

    override suspend fun saveDailyTotal(
        total: StepTotal
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun getStepTotals(
        startDate: String,
        endDate: String
    ): Result<List<StepTotal>> {
        lastStartDate = startDate
        lastEndDate = endDate

        return Result.success(
            totals.filter {
                it.date >= startDate &&
                        it.date <= endDate
            }
        )
    }
}

private class FakeWellnessRepository(
    private val meals: List<MealEntry> = emptyList(),
    private val water: List<WaterEntry> = emptyList(),
    private val workouts: List<WorkoutEntry> = emptyList(),
    private val sleep: List<SleepEntry> = emptyList(),
    private val checkIns: List<CheckInEntry> = emptyList()
) : WellnessRepository {

    var lastStartDate: String? = null
    var lastEndDate: String? = null

    private fun recordRange(
        startDate: String,
        endDate: String
    ) {
        lastStartDate = startDate
        lastEndDate = endDate
    }

    override suspend fun createMeal(
        entry: MealEntry
    ): Result<String> {
        return Result.success("meal-id")
    }

    override suspend fun getMeals(
        startDate: String,
        endDate: String
    ): Result<List<MealEntry>> {
        recordRange(startDate, endDate)

        return Result.success(
            meals.filter {
                it.date >= startDate &&
                        it.date <= endDate
            }
        )
    }

    override suspend fun updateMeal(
        entry: MealEntry
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun deleteMeal(
        id: String
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun createWaterEntry(
        entry: WaterEntry
    ): Result<String> {
        return Result.success("water-id")
    }

    override suspend fun getWaterEntries(
        startDate: String,
        endDate: String
    ): Result<List<WaterEntry>> {
        recordRange(startDate, endDate)

        return Result.success(
            water.filter {
                it.date >= startDate &&
                        it.date <= endDate
            }
        )
    }

    override suspend fun updateWaterEntry(
        entry: WaterEntry
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun deleteWaterEntry(
        id: String
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun createWorkout(
        entry: WorkoutEntry
    ): Result<String> {
        return Result.success("workout-id")
    }

    override suspend fun getWorkouts(
        startDate: String,
        endDate: String
    ): Result<List<WorkoutEntry>> {
        recordRange(startDate, endDate)

        return Result.success(
            workouts.filter {
                it.date >= startDate &&
                        it.date <= endDate
            }
        )
    }

    override suspend fun updateWorkout(
        entry: WorkoutEntry
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun deleteWorkout(
        id: String
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun createSleepEntry(
        entry: SleepEntry
    ): Result<String> {
        return Result.success("sleep-id")
    }

    override suspend fun getSleepEntries(
        startDate: String,
        endDate: String
    ): Result<List<SleepEntry>> {
        recordRange(startDate, endDate)

        return Result.success(
            sleep.filter {
                it.date >= startDate &&
                        it.date <= endDate
            }
        )
    }

    override suspend fun updateSleepEntry(
        entry: SleepEntry
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun deleteSleepEntry(
        id: String
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun createCheckIn(
        entry: CheckInEntry
    ): Result<String> {
        return Result.success("check-in-id")
    }

    override suspend fun getCheckIns(
        startDate: String,
        endDate: String
    ): Result<List<CheckInEntry>> {
        recordRange(startDate, endDate)

        return Result.success(
            checkIns.filter {
                it.date >= startDate &&
                        it.date <= endDate
            }
        )
    }

    override suspend fun updateCheckIn(
        entry: CheckInEntry
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun deleteCheckIn(
        id: String
    ): Result<Unit> {
        return Result.success(Unit)
    }
}