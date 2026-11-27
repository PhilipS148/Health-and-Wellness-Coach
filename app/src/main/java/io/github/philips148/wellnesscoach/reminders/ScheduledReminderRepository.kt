package io.github.philips148.wellnesscoach.reminders

import io.github.philips148.wellnesscoach.data.reminders.ReminderPreferences
import io.github.philips148.wellnesscoach.data.reminders.ReminderPreferencesRepository

fun ReminderPreferences.deliverySchedules(): List<ReminderSchedule> = listOf(
    ReminderSchedule(ReminderKind.HYDRATION, hydration.enabled, hydration.hour, hydration.minute),
    ReminderSchedule(ReminderKind.CHECK_IN, checkIn.enabled, checkIn.hour, checkIn.minute),
    ReminderSchedule(ReminderKind.WORKOUT, workout.enabled, workout.hour, workout.minute)
)

class ScheduledReminderRepository(
    private val savedSettings: ReminderPreferencesRepository,
    private val sync: (String, ReminderPreferences) -> Unit
) : ReminderPreferencesRepository {
    private var revision = 0
    private var pendingSaves = 0

    override suspend fun load(userId: String): Result<ReminderPreferences> {
        val startedAt = revision
        val startedDuringSave = pendingSaves > 0
        return savedSettings.load(userId).onSuccess {
            // A slow refresh must not replace a newer save's delivery schedule.
            if (startedAt == revision && !startedDuringSave && pendingSaves == 0) sync(userId, it)
        }
    }

    override suspend fun save(userId: String, preferences: ReminderPreferences): Result<Unit> {
        val startedAt = ++revision
        pendingSaves++
        return try {
            savedSettings.save(userId, preferences).onSuccess {
                if (startedAt == revision) sync(userId, preferences)
            }
        } finally { pendingSaves-- }
    }
}
