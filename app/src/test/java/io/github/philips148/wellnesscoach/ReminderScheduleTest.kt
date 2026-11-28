package io.github.philips148.wellnesscoach

import io.github.philips148.wellnesscoach.reminders.*
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class ReminderScheduleTest {
    private val zone = ZoneId.of("America/Los_Angeles")
    @Test fun laterToday() {
        val now = ZonedDateTime.of(2026, 11, 22, 9, 0, 0, 0, zone)
        assertEquals(now.withHour(10), nextReminderTime(now, 10, 0))
    }
    @Test fun pastOrEqualMovesToTomorrow() {
        val now = ZonedDateTime.of(2026, 11, 22, 10, 0, 0, 0, zone)
        assertEquals(now.plusDays(1), nextReminderTime(now, 10, 0))
        assertEquals(now.plusDays(1).withHour(9), nextReminderTime(now, 9, 0))
    }
    @Test fun midnightAndYearBoundary() {
        val now = ZonedDateTime.of(2026, 12, 31, 23, 59, 0, 0, zone)
        assertEquals("2027-01-01T00:00-08:00[America/Los_Angeles]", nextReminderTime(now, 0, 0).toString())
    }
    @Test fun springGapUsesNextValidTime() {
        val now = ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, zone)
        val next = nextReminderTime(now, 2, 30)
        assertEquals(3, next.hour)
        assertEquals(30, next.minute)
        assertTrue(next.isAfter(now))
    }
    @Test fun fallOverlapSchedulesOnlyFirstOccurrence() {
        val now = ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, zone)
        val first = nextReminderTime(now, 1, 30)
        assertEquals(first.toLocalDate().plusDays(1), nextReminderTime(first, 1, 30).toLocalDate())
    }
    @Test fun timezoneDeterminesLocalTime() {
        val now = ZonedDateTime.of(2026, 11, 22, 9, 0, 0, 0, ZoneId.of("Asia/Tokyo"))
        assertEquals(now.withHour(10), nextReminderTime(now, 10, 0))
    }
    @Test fun rejectsInvalidTimes() {
        for ((hour, minute) in listOf(-1 to 0, 24 to 0, 0 to -1, 0 to 60)) {
            assertThrows(IllegalArgumentException::class.java) { ReminderSchedule(ReminderKind.HYDRATION, true, hour, minute) }
        }
    }
}
