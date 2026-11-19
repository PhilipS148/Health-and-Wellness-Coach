package io.github.philips148.wellnesscoach.util
import io.github.philips148.wellnesscoach.data.summary.DailySummary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object ProgressPeriods {

    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun shift(date: LocalDate, weekly: Boolean, steps: Long): LocalDate =
        if (weekly) date.plusWeeks(steps) else date.plusDays(steps)

    fun canGoNext(date: LocalDate, weekly: Boolean, today: LocalDate): Boolean =
        if (weekly) weekStart(date).isBefore(weekStart(today)) else date.isBefore(today)

    // True if the day has at least one saved record of any kind.
    fun hasData(day: DailySummary): Boolean =
        day.calories > 0 || day.waterOz > 0 || day.workoutCount > 0 ||
                day.steps != null || day.sleepHours != null ||
                day.stressLevel != null || day.mood != null || day.energyLevel != null
}