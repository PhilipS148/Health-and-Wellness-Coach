package io.github.philips148.wellnesscoach

import io.github.philips148.wellnesscoach.data.model.CheckInEntry
import io.github.philips148.wellnesscoach.data.model.TrackingCategories
import io.github.philips148.wellnesscoach.data.wellness.CheckInValidation
import io.github.philips148.wellnesscoach.ui.checkins.CheckInForm
import org.junit.Assert.*
import org.junit.Test

class CheckInFormTest {
    private val all = setOf(TrackingCategories.STRESS, TrackingCategories.MOOD, TrackingCategories.ENERGY)
    private fun form() = CheckInForm(date = "2026-11-04", stress = "3", mood = "8", energy = "7")

    @Test fun recordsAllThreeIndependentRatings() {
        val entry = form().toEntry(all).getOrThrow()
        assertEquals(3, entry.stressLevel)
        assertEquals(8, entry.mood)
        assertEquals(7, entry.energyLevel)
    }
    @Test fun emptyFormDoesNotInventNeutralRatings() {
        assertTrue(CheckInForm(date = "2026-11-04").toEntry(all).isFailure)
        assertNull(CheckInEntry().stressLevel)
        assertNull(CheckInEntry().mood)
        assertNull(CheckInEntry().energyLevel)
    }
    @Test fun partialCheckInKeepsUnrecordedValuesNull() {
        val entry = CheckInForm(date = "2026-11-04", mood = "8").toEntry(all).getOrThrow()
        assertEquals(8, entry.mood)
        assertNull(entry.stressLevel)
        assertNull(entry.energyLevel)
    }
    @Test fun scaleEndpointsAreAccepted() {
        val entry = form().copy(stress = "1", mood = "10", energy = " 10 ").toEntry(all).getOrThrow()
        assertEquals(1, entry.stressLevel)
        assertEquals(10, entry.mood)
        assertEquals(10, entry.energyLevel)
    }
    @Test fun invalidStressRatingsAreRejected() {
        listOf("0", "11", "-1", "1.5", "NaN", "2147483648", "abc").forEach {
            assertTrue(it, form().copy(stress = it).toEntry(all).isFailure)
        }
    }
    @Test fun invalidMoodAndEnergyAreRejectedIndividually() {
        assertTrue(form().copy(mood = "0").toEntry(all).isFailure)
        assertTrue(form().copy(energy = "11").toEntry(all).isFailure)
    }
    @Test fun newCheckInCollectsOnlySelectedCategories() {
        val entry = form().toEntry(setOf(TrackingCategories.MOOD)).getOrThrow()
        assertEquals(8, entry.mood)
        assertNull(entry.stressLevel)
        assertNull(entry.energyLevel)
    }
    @Test fun noSelectedCategoriesCannotCreateEmptyCheckIn() {
        assertTrue(form().toEntry(emptySet()).isFailure)
    }
    @Test fun editingPreservesUnselectedRatingsAndMetadata() {
        val original = CheckInEntry(id = "saved-id", date = "2026-11-04",
            stressLevel = 2, mood = 7, energyLevel = 9, createdAt = 12345L)
        val updated = CheckInForm.from(original).copy(mood = "10", date = "2026-11-03",
            stress = "", energy = "bad").toEntry(setOf(TrackingCategories.MOOD), original).getOrThrow()
        assertEquals(2, updated.stressLevel)
        assertEquals(9, updated.energyLevel)
        assertEquals(10, updated.mood)
        assertEquals(original.id, updated.id)
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals("2026-11-03", updated.date)
    }
    @Test fun selectedRatingCanBeExplicitlyCleared() {
        val original = CheckInEntry(date = "2026-11-04", stressLevel = 3, mood = 8)
        val updated = CheckInForm.from(original).copy(stress = " ").toEntry(all, original).getOrThrow()
        assertNull(updated.stressLevel)
        assertEquals(8, updated.mood)
    }
    @Test fun clearingLastRatingIsRejected() {
        val original = CheckInEntry(date = "2026-11-04", mood = 8)
        assertTrue(CheckInForm.from(original).copy(mood = "").toEntry(all, original).isFailure)
    }
    @Test fun dateOnlyEditPreservesRatingsWhenAllCategoriesTurnedOff() {
        val original = CheckInEntry(id = "id", date = "2026-11-04", stressLevel = 4, mood = 9)
        val updated = CheckInForm.from(original).copy(date = "2026-11-03")
            .toEntry(emptySet(), original).getOrThrow()
        assertEquals(original.copy(date = "2026-11-03"), updated)
    }
    @Test fun realCalendarDatesAndCanonicalFormatAreRequired() {
        listOf("2026-02-29", "2026-04-31", "2026-1-2", "11/4/2026", "").forEach {
            assertTrue(it, form().copy(date = it).toEntry(all).isFailure)
        }
        assertTrue(form().copy(date = "2024-02-29").toEntry(all).isSuccess)
    }
    @Test fun historyRangeIsInclusiveAndOrdered() {
        assertNull(CheckInValidation.rangeError("2026-11-04", "2026-11-04"))
        assertNull(CheckInValidation.rangeError("2026-10-01", "2026-11-04"))
        assertNotNull(CheckInValidation.rangeError("2026-11-05", "2026-11-04"))
        assertNotNull(CheckInValidation.rangeError("2026-02-30", "2026-11-04"))
    }
    @Test fun dataLayerValidationRejectsEmptyAndOutOfRangeRecords() {
        assertNotNull(CheckInValidation.error(CheckInEntry(date = "2026-11-04")))
        assertNotNull(CheckInValidation.error(CheckInEntry(date = "2026-11-04", stressLevel = 0)))
        assertNotNull(CheckInValidation.error(CheckInEntry(date = "2026-11-04", mood = 11)))
        assertNotNull(CheckInValidation.error(CheckInEntry(date = "2026-11-04", energyLevel = -1)))
        assertNull(CheckInValidation.error(CheckInEntry(date = "2026-11-04", energyLevel = 1)))
    }
}
