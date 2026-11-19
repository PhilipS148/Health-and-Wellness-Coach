package io.github.philips148.wellnesscoach.util
import io.github.philips148.wellnesscoach.data.summary.DailySummary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

object ProgressPeriods {

    fun weekStart(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    fun shift(date: LocalDate, weekly: Boolean, steps: Long): LocalDate {
        return if (weekly) date.plusWeeks(steps) else date.plusDays(steps)
    }
    fun canGoNext(date: LocalDate, weekly: Boolean, today: LocalDate): Boolean {
        return if (weekly) weekStart(date).isBefore(weekStart(today)) else date.isBefore(today)
    }
    fun hasData(day: DailySummary): Boolean {
        return day.calories > 0 || day.waterOz > 0 || day.workoutCount > 0 ||
                day.steps != null || day.sleepHours != null ||
                day.stressLevel != null || day.mood != null || day.energyLevel != null
    }

    fun percent(progress: Double?): String {
        return if (progress == null) "No data" else "${(progress * 100).roundToInt()}%"

    }

    fun average(value: Double?): String {
        return if (value == null) "No data" else String.format(Locale.US, "%.1f", value)
    }

    fun periodLabel(date: LocalDate, weekly: Boolean, today: LocalDate): String {
        val format = DateTimeFormatter.ofPattern("MMM d", Locale.US)
        return if (weekly) {
            val start = weekStart(date)
            val prefix = if (start == weekStart(today)) "This week · " else ""
            "$prefix${start.format(format)} – ${start.plusDays(6).format(format)}"
        } else {
            if (date == today) "Today" else date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US))
        }
    }
}