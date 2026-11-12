package io.github.philips148.wellnesscoach.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SleepValidatorTest {

    private val today = LocalDate.of(2026, 11, 5)

    // durationError

    @Test
    fun durationError_hoursAndMinutes_isValid() {
        assertNull(SleepValidator.durationError("7", "30"))
    }

    @Test
    fun durationError_blankMinutes_countsAsZero() {
        assertNull(SleepValidator.durationError("8", ""))
    }

    @Test
    fun durationError_blankHours_countsAsZero() {
        assertNull(SleepValidator.durationError("", "45"))
    }

    @Test
    fun durationError_bothBlank_returnsError() {
        assertNotNull(SleepValidator.durationError("", ""))
    }

    @Test
    fun durationError_exactly24Hours_isValid() {
        assertNull(SleepValidator.durationError("24", "0"))
    }

    @Test
    fun durationError_over24Hours_returnsError() {
        assertNotNull(SleepValidator.durationError("24", "30"))
    }

    @Test
    fun durationError_zeroTotal_returnsError() {
        assertNotNull(SleepValidator.durationError("0", "0"))
    }

    @Test
    fun durationError_sixtyMinutes_returnsError() {
        assertNotNull(SleepValidator.durationError("7", "60"))
    }

    @Test
    fun durationError_nonNumberHours_returnsError() {
        assertNotNull(SleepValidator.durationError("abc", "0"))
    }

    @Test
    fun durationError_negativeHours_returnsError() {
        assertNotNull(SleepValidator.durationError("-1", "30"))
    }

    @Test
    fun toHours_sevenThirty_isSevenPointFive() {
        assertEquals(7.5, SleepValidator.toHours("7", "30"), 0.001)
    }

    @Test
    fun toHours_blankMinutes_isWholeHours() {
        assertEquals(8.0, SleepValidator.toHours("8", ""), 0.001)
    }

    @Test
    fun splitHours_sevenPointFive_isSevenThirty() {
        assertEquals(Pair(7, 30), SleepValidator.splitHours(7.5))
    }

    @Test
    fun splitHours_repeatingDecimal_roundsToTwentyMinutes() {
        assertEquals(Pair(7, 20), SleepValidator.splitHours(7.0 + 20.0 / 60.0))
    }

    @Test
    fun splitHours_roundTrip_matchesOriginalFields() {
        val hours = SleepValidator.toHours("6", "45")
        assertEquals(Pair(6, 45), SleepValidator.splitHours(hours))
    }

    @Test
    fun dateError_today_isValid() {
        assertNull(SleepValidator.dateError("2026-11-05", today))
    }

    @Test
    fun dateError_past_isValid() {
        assertNull(SleepValidator.dateError("2026-10-30", today))
    }

    @Test
    fun dateError_future_returnsError() {
        assertNotNull(SleepValidator.dateError("2026-11-06", today))
    }

    @Test
    fun dateError_impossibleDate_returnsError() {
        assertNotNull(SleepValidator.dateError("2026-13-45", today))
    }

    @Test
    fun dateError_wrongFormat_returnsError() {
        assertNotNull(SleepValidator.dateError("11/05/2026", today))
    }
}