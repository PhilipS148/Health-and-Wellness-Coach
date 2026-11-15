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

class DefaultSummaryRepository(
    private val userRepository: UserRepository,
    private val wellnessRepository: WellnessRepository,
    private val stepRepository: StepRepository
) : SummaryRepository {

    override suspend fun getDailySummary(
        date: String
    ): Result<DailySummary> {
        return try {
            LocalDate.parse(date)

            val profile = requireProfile()

            val meals = wellnessRepository
                .getMeals(date, date)
                .getOrThrow()

            val water = wellnessRepository
                .getWaterEntries(date, date)
                .getOrThrow()

            val workouts = wellnessRepository
                .getWorkouts(date, date)
                .getOrThrow()

            val sleep = wellnessRepository
                .getSleepEntries(date, date)
                .getOrThrow()

            val checkIns = wellnessRepository
                .getCheckIns(date, date)
                .getOrThrow()

            val stepTotals = stepRepository
                .getStepTotals(date, date)
                .getOrThrow()

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

            val profile = requireProfile()

            val meals = wellnessRepository
                .getMeals(startDate, endDate)
                .getOrThrow()

            val water = wellnessRepository
                .getWaterEntries(startDate, endDate)
                .getOrThrow()

            val workouts = wellnessRepository
                .getWorkouts(startDate, endDate)
                .getOrThrow()

            val sleep = wellnessRepository
                .getSleepEntries(startDate, endDate)
                .getOrThrow()

            val checkIns = wellnessRepository
                .getCheckIns(startDate, endDate)
                .getOrThrow()

            val stepTotals = stepRepository
                .getStepTotals(startDate, endDate)
                .getOrThrow()

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

        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private suspend fun requireProfile(): UserProfile {
        return userRepository
            .getProfile()
            .getOrThrow()
            ?: throw IllegalStateException(
                "No profile exists for the signed-in user."
            )
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