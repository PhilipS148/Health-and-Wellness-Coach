package io.github.philips148.wellnesscoach.reminders

import java.time.ZonedDateTime

// R1 maps its saved preferences to these delivery inputs; this is not cloud storage.
enum class ReminderKind(val label: String, val route: String, val message: String) {
    HYDRATION("Hydration", "hydration", "Take a moment to record your water intake."),
    CHECK_IN("Daily check-in", "check_ins", "Take a moment to check in with yourself."),
    WORKOUT("Workout", "workouts", "Ready to record your movement today?")
}

data class ReminderSchedule(val kind: ReminderKind, val enabled: Boolean, val hour: Int, val minute: Int) {
    init { require(hour in 0..23 && minute in 0..59) { "Use a valid local reminder time." } }
}

fun nextReminderTime(now: ZonedDateTime, hour: Int, minute: Int): ZonedDateTime {
    require(hour in 0..23 && minute in 0..59)
    val today = now.toLocalDate().atTime(hour, minute).atZone(now.zone)
    return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(hour, minute).atZone(now.zone)
}
