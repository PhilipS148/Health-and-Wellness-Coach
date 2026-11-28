package io.github.philips148.wellnesscoach

import io.github.philips148.wellnesscoach.data.reminders.*
import org.junit.Assert.*
import org.junit.Test

class ReminderPreferencesTest {
    @Test fun missingSettingsAreDisabledWithAgreedTimes() {
        val settings = ReminderPreferences.fromFields(null)
        assertFalse(settings.hydration.enabled)
        assertFalse(settings.checkIn.enabled)
        assertFalse(settings.workout.enabled)
        assertEquals(10, settings.hydration.hour)
        assertEquals(20, settings.checkIn.hour)
        assertEquals(17, settings.workout.hour)
    }
    @Test fun settingsRoundTripWithoutLosingDisabledTimes() {
        val settings = ReminderPreferences(ReminderPreference(true, 0, 0),
            ReminderPreference(false, 23, 59), ReminderPreference(true, 7, 15))
        assertEquals(settings, ReminderPreferences.fromFields(settings.toFields()))
    }
    @Test fun firestoreLongNumbersAreSupported() {
        val fields = mapOf("hydration" to mapOf("enabled" to true, "hour" to 12L, "minute" to 30L))
        assertEquals(ReminderPreference(true, 12, 30), ReminderPreferences.fromFields(fields).hydration)
    }
    @Test fun categoryChangesDoNotChangeOtherReminders() {
        val original = ReminderPreferences()
        val changed = original.copy(hydration = original.hydration.copy(enabled = true, hour = 15))
        assertEquals(original.checkIn, changed.checkIn)
        assertEquals(original.workout, changed.workout)
        assertEquals(15, changed.hydration.copy(enabled = false).hour)
    }
    @Test fun rejectsInvalidTimeBounds() {
        for ((h, m) in listOf(-1 to 0, 24 to 0, 0 to -1, 0 to 60)) {
            assertThrows(IllegalArgumentException::class.java) { ReminderPreference(false, h, m) }
        }
    }
    @Test fun malformedSavedValuesDoNotBecomeDefaults() {
        for (bad in listOf<Any>("10", 1.5, Double.NaN, Long.MAX_VALUE, 24)) {
            assertThrows(Exception::class.java) {
                ReminderPreferences.fromFields(mapOf("hydration" to mapOf("enabled" to true, "hour" to bad, "minute" to 0)))
            }
        }
        assertThrows(Exception::class.java) { ReminderPreferences.fromFields("broken") }
    }
    @Test fun newCategoriesCanUseDefaultsAlongsideExistingSettings() {
        val settings = ReminderPreferences.fromFields(mapOf("workout" to mapOf("enabled" to true, "hour" to 18L, "minute" to 45L)))
        assertEquals(ReminderPreference(true, 18, 45), settings.workout)
        assertFalse(settings.hydration.enabled)
    }
    @Test fun formatHasOnlyTheAgreedPreferenceFields() {
        val fields = ReminderPreferences().toFields()
        assertEquals(setOf("hydration", "checkIn", "workout"), fields.keys)
        assertEquals(setOf("enabled", "hour", "minute"), (fields["checkIn"] as Map<*, *>).keys)
    }
}
