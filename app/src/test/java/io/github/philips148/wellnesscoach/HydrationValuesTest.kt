package io.github.philips148.wellnesscoach

import io.github.philips148.wellnesscoach.data.hydration.HydrationValues
import io.github.philips148.wellnesscoach.data.model.WaterEntry
import org.junit.Assert.*
import org.junit.Test

class HydrationValuesTest {
    @Test fun datesMustBeRealAndUseTheAgreedFormat() {
        assertTrue(HydrationValues.validDate("2024-02-29"))
        listOf("2026-02-29", "2026-04-31", "10/27/2026", "2026-1-2", "").forEach {
            assertFalse(it, HydrationValues.validDate(it))
        }
    }

    @Test fun amountRequiresPositiveWholeOunces() {
        listOf("", "0", "-1", "1.5", "NaN", "2147483648").forEach {
            assertNotNull(it, HydrationValues.inputError("2026-10-27", it))
        }
        assertNull(HydrationValues.inputError("2026-10-27", " 16 "))
    }

    @Test fun invalidEntryDateIsRejectedEvenWhenAmountIsValid() {
        assertNotNull(HydrationValues.inputError("2026-02-30", "16"))
    }

    @Test fun totalIncludesOnlyTheSelectedDayAndEveryEntry() {
        val entries = listOf(
            WaterEntry(id = "a", date = "2026-10-27", amountOz = 8),
            WaterEntry(id = "b", date = "2026-10-27", amountOz = 16),
            WaterEntry(id = "c", date = "2026-10-26", amountOz = 24)
        )
        assertEquals(24L, HydrationValues.totalForDate(entries, "2026-10-27"))
        assertEquals(0L, HydrationValues.totalForDate(entries, "2026-10-28"))
    }

    @Test fun movingAnEntryChangesBothDaysTotals() {
        val old = WaterEntry(id = "a", date = "2026-10-27", amountOz = 16)
        val moved = old.copy(date = "2026-10-28", amountOz = 24)
        assertEquals(0L, HydrationValues.totalForDate(listOf(moved), old.date))
        assertEquals(24L, HydrationValues.totalForDate(listOf(moved), moved.date))
        assertEquals(0L, HydrationValues.totalForDate(emptyList(), moved.date))
    }

    @Test fun totalsDoNotOverflowWhenSummingIntAmounts() {
        val entries = List(2) { WaterEntry(date = "2026-10-27", amountOz = Int.MAX_VALUE) }
        assertEquals(4294967294L, HydrationValues.totalForDate(entries, "2026-10-27"))
    }

    @Test fun progressUsesTheSavedGoalAndCapsOnlyTheBar() {
        assertEquals(0.5f, HydrationValues.progress(32, 64), 0.0001f)
        assertEquals(0.25f, HydrationValues.progress(32, 128), 0.0001f)
        assertEquals(1f, HydrationValues.progress(96, 64), 0f)
        assertEquals(0f, HydrationValues.progress(0, 64), 0f)
        assertEquals(0f, HydrationValues.progress(32, 0), 0f)
    }
}
