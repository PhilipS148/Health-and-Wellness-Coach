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
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

class DefaultSummaryRepository(
    private val userRepository: UserRepository,
    private val wellnessRepository: WellnessRepository,
    private val stepRepository: StepRepository,
    private val getCurrentUserId: () -> String?
) : SummaryRepository {

    override suspend fun getDailySummary(
        date: String
    ): Result<DailySummary> {
        return try {
            LocalDate.parse(date)

            val userId = requireUserId()

            val profile = requireProfile(userId)

            val meals = wellnessRepository
                .getMeals(date, date)
                .getForUser(userId)

            val water = wellnessRepository
                .getWaterEntries(date, date)
                .getForUser(userId)

            val workouts = wellnessRepository
                .getWorkouts(date, date)
                .getForUser(userId)

            val sleep = wellnessRepository
                .getSleepEntries(date, date)
                .getForUser(userId)

            val checkIns = wellnessRepository
                .getCheckIns(date, date)
                .getForUser(userId)

            val stepTotals = stepRepository
                .getStepTotals(date, date)
                .getForUser(userId)

            checkUserId(userId)

            Result.success(
                buildDailySummary(
                    date = date,
                    profile = profile,
                    meals = meals,
                    water = water,
                    workouts = workouts,
                    sleep = sleep,
                    checkIns = checkIns,
                    stepTotals = stepTotals
                )
            )
        } catch (exception: CancellationException) {
            // Pass cancellation back to the caller so the request can stop.
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun getWeeklySummary(
        startDate: String
    ): Result<WeeklySummary> {
        return try {
            val start = LocalDate.parse(startDate)
            val end = start.plusDays(6)

            val endDate = end.toString()

            val userId = requireUserId()

            val profile = requireProfile(userId)

            val meals = wellnessRepository
                .getMeals(startDate, endDate)
                .getForUser(userId)

            val water = wellnessRepository
                .getWaterEntries(startDate, endDate)
                .getForUser(userId)

            val workouts = wellnessRepository
                .getWorkouts(startDate, endDate)
                .getForUser(userId)

            val sleep = wellnessRepository
                .getSleepEntries(startDate, endDate)
                .getForUser(userId)

            val checkIns = wellnessRepository
                .getCheckIns(startDate, endDate)
                .getForUser(userId)

            val stepTotals = stepRepository
                .getStepTotals(startDate, endDate)
                .getForUser(userId)

            val dailySummaries = (0L..6L).map { offset ->
                val date = start.plusDays(offset).toString()

                buildDailySummary(
                    date = date,
                    profile = profile,
                    meals = meals.filter { it.date == date },
                    water = water.filter { it.date == date },
                    workouts = workouts.filter { it.date == date },
                    sleep = sleep.filter { it.date == date },
                    checkIns = checkIns.filter { it.date == date },
                    stepTotals = stepTotals.filter { it.date == date }
                )
            }

            checkUserId(userId)

            Result.success(
                WeeklySummary(
                    startDate = startDate,
                    endDate = endDate,
                    days = dailySummaries,

                    averageDailyCalories =
                        dailySummaries
                            .map { it.calories }
                            .average(),

                    averageDailyWaterOz =
                        dailySummaries
                            .map { it.waterOz }
                            .average(),

                    averageDailySteps =
                        dailySummaries
                            .mapNotNull { it.steps }
                            .takeIf { it.isNotEmpty() }
                            ?.average(),

                    averageSleepHours =
                        dailySummaries
                            .mapNotNull { it.sleepHours }
                            .takeIf { it.isNotEmpty() }
                            ?.average(),

                    totalWorkouts =
                        dailySummaries.sumOf {
                            it.workoutCount
                        },

                    totalWorkoutMinutes =
                        dailySummaries.sumOf {
                            it.workoutMinutes
                        },

                    averageStressLevel =
                        dailySummaries
                            .mapNotNull { it.stressLevel }
                            .takeIf { it.isNotEmpty() }
                            ?.average(),

                    averageMood =
                        dailySummaries
                            .mapNotNull { it.mood }
                            .takeIf { it.isNotEmpty() }
                            ?.average(),

                    averageEnergyLevel =
                        dailySummaries
                            .mapNotNull { it.energyLevel }
                            .takeIf { it.isNotEmpty() }
                            ?.average()
                )
            )

        } catch (exception: CancellationException) {
            // Pass cancellation back to the caller so the request can stop.
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    // Capture the account this summary request belongs to.
    private fun requireUserId(): String {
        return getCurrentUserId()
            ?: throw IllegalStateException(
                "A signed-in user is required."
            )
    }

    // An account change makes the in-progress summary invalid.
    private fun checkUserId(userId: String) {
        check(getCurrentUserId() == userId) {
            "The signed-in account changed. Reload the summary."
        }
    }

    // Check the account after each completed repository read.
    private fun <T> Result<T>.getForUser(userId: String): T {
        val value = getOrThrow()
        checkUserId(userId)
        return value
    }

    private suspend fun requireProfile(userId: String): UserProfile {
        val profile = userRepository
            .getProfile()
            .getForUser(userId)
            ?: throw IllegalStateException(
                "No profile exists for the signed-in user."
            )

        check(profile.uid == userId) {
            "The profile does not belong to the signed-in user."
        }

        return profile
    }

    private fun buildDailySummary(
        date: String,
        profile: UserProfile,
        meals: List<MealEntry>,
        water: List<WaterEntry>,
        workouts: List<WorkoutEntry>,
        sleep: List<SleepEntry>,
        checkIns: List<CheckInEntry>,
        stepTotals: List<StepTotal>
    ): DailySummary {

        val calories = meals.sumOf {
            it.calories
        }

        val waterOz = water.sumOf {
            it.amountOz
        }

        val workoutMinutes = workouts.sumOf {
            it.durationMinutes ?: 0
        }

        val sleepHours =
            sleep
                .takeIf { it.isNotEmpty() }
                ?.sumOf { it.hoursSlept }

        // Step totals are not added together.
        // A date should have one saved total.
        val steps = stepTotals
            .firstOrNull {
                it.date == date
            }
            ?.steps

        // Use the newest check-in if more than one exists.
        val checkIn = checkIns.maxByOrNull {
            it.createdAt
        }

        return DailySummary(
            date = date,

            selectedCategories =
                profile.selectedCategories,

            calories = calories,
            calorieGoal =
                profile.dailyCalorieGoal,
            calorieProgress = progress(
                calories.toDouble(),
                profile.dailyCalorieGoal.toDouble()
            ) ?: 0.0,

            waterOz = waterOz,
            waterGoalOz =
                profile.dailyWaterGoalOz,
            waterProgress = progress(
                waterOz.toDouble(),
                profile.dailyWaterGoalOz.toDouble()
            ) ?: 0.0,

            steps = steps,
            stepGoal =
                profile.dailyStepGoal,
            stepProgress = progress(
                steps?.toDouble(),
                profile.dailyStepGoal.toDouble()
            ),

            sleepHours = sleepHours,
            sleepGoalHours =
                profile.sleepGoalHours,
            sleepProgress = progress(
                sleepHours,
                profile.sleepGoalHours
            ),

            workoutCount = workouts.size,
            workoutMinutes = workoutMinutes,

            stressLevel =
                checkIn?.stressLevel,

            mood =
                checkIn?.mood,

            energyLevel =
                checkIn?.energyLevel
        )
    }

    // 1.0 means the saved amount equals the goal.
    // Values above 1.0 mean the goal was exceeded.
    private fun progress(
        value: Double?,
        goal: Double
    ): Double? {
        if (value == null || goal <= 0.0) {
            return null
        }

        return value / goal
    }
}