package io.github.philips148.wellnesscoach

import io.github.philips148.wellnesscoach.data.model.WorkoutEntry
import io.github.philips148.wellnesscoach.data.model.WorkoutSet
import io.github.philips148.wellnesscoach.data.wellness.WorkoutValidation
import io.github.philips148.wellnesscoach.ui.workouts.WorkoutForm
import io.github.philips148.wellnesscoach.ui.workouts.WorkoutSetInput
import org.junit.Assert.*
import org.junit.Test

class WorkoutFormTest {
    private fun strength() = WorkoutForm(
        date = "2026-11-02", name = " Bench press ",
        sets = listOf(WorkoutSetInput("12", "95"), WorkoutSetInput("8", "115.5"))
    )
    private fun cardio() = WorkoutForm(
        date = "2026-11-02", type = "walk", distanceMiles = "2.5", durationMinutes = "30"
    )

    @Test fun strengthPreservesDifferentValuesForIndividualSets() {
        val entry = strength().toEntry().getOrThrow()
        assertEquals("Bench press", entry.exerciseName)
        assertEquals(listOf(WorkoutSet(12, 95.0), WorkoutSet(8, 115.5)), entry.sets)
        assertNull(entry.distanceMiles)
        assertNull(entry.durationMinutes)
    }
    @Test fun emptyWeightIsOptionalAndZeroIsValid() {
        val entry = strength().copy(sets = listOf(
            WorkoutSetInput("10", ""), WorkoutSetInput("12", "0")
        )).toEntry().getOrThrow()
        assertNull(entry.sets[0].weightLbs)
        assertEquals(0.0, entry.sets[1].weightLbs!!, 0.0)
    }
    @Test fun exerciseAndAtLeastOneSetAreRequired() {
        assertTrue(strength().copy(name = " ").toEntry().isFailure)
        assertTrue(strength().copy(sets = emptyList()).toEntry().isFailure)
    }
    @Test fun repsMustBePositiveWholeNumbersWithinIntRange() {
        listOf("", "0", "-1", "1.5", "2147483648", "NaN").forEach { reps ->
            assertTrue(reps, strength().copy(sets = listOf(WorkoutSetInput(reps, "10"))).toEntry().isFailure)
        }
    }
    @Test fun weightRejectsNegativeNonNumericAndNonFiniteValues() {
        listOf("-1", "NaN", "Infinity", "abc").forEach { weight ->
            assertTrue(weight, strength().copy(sets = listOf(WorkoutSetInput("10", weight))).toEntry().isFailure)
        }
    }
    @Test fun walkAndRunRequireManualDistanceAndDuration() {
        listOf("walk", "run").forEach { type ->
            val entry = cardio().copy(type = type).toEntry().getOrThrow()
            assertEquals(type, entry.workoutType)
            assertEquals(2.5, entry.distanceMiles!!, 0.0)
            assertEquals(30, entry.durationMinutes)
            assertTrue(entry.sets.isEmpty())
        }
    }
    @Test fun distanceRejectsEmptyZeroNegativeNonFiniteValues() {
        listOf("", "0", "-2", "NaN", "Infinity", "abc").forEach { distance ->
            assertTrue(distance, cardio().copy(distanceMiles = distance).toEntry().isFailure)
        }
    }
    @Test fun durationMustBePositiveWholeMinutes() {
        listOf("", "0", "-5", "1.5", "2147483648").forEach { duration ->
            assertTrue(duration, cardio().copy(durationMinutes = duration).toEntry().isFailure)
        }
    }
    @Test fun switchingTypeDoesNotSaveIrrelevantHiddenFields() {
        val run = strength().copy(type = "run", distanceMiles = "1.2", durationMinutes = "12")
            .toEntry().getOrThrow()
        assertTrue(run.sets.isEmpty())
        val strength = strength().copy(distanceMiles = "bad", durationMinutes = "bad")
            .toEntry().getOrThrow()
        assertNull(strength.distanceMiles)
        assertNull(strength.durationMinutes)
    }
    @Test fun dateValidationRejectsImpossibleOrNonCanonicalDates() {
        listOf("2026-02-29", "2026-04-31", "11/2/2026", "2026-1-2", "").forEach {
            assertTrue(it, strength().copy(date = it).toEntry().isFailure)
        }
        assertTrue(strength().copy(date = "2024-02-29").toEntry().isSuccess)
    }
    @Test fun historyRangeMustBeOrderedAndInclusive() {
        assertNull(WorkoutValidation.rangeError("2026-11-02", "2026-11-02"))
        assertNull(WorkoutValidation.rangeError("2026-10-01", "2026-11-02"))
        assertNotNull(WorkoutValidation.rangeError("2026-11-03", "2026-11-02"))
        assertNotNull(WorkoutValidation.rangeError("bad", "2026-11-02"))
    }
    @Test fun editingPreservesIdentityAndCreationTimestamp() {
        val original = strength().toEntry().getOrThrow().copy(id = "saved-id", createdAt = 12345L)
        val updated = WorkoutForm.from(original).copy(date = "2026-11-03", name = "Incline press")
            .toEntry(original).getOrThrow()
        assertEquals(original.id, updated.id)
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals("2026-11-03", updated.date)
        assertEquals(original.sets, updated.sets)
    }
    @Test fun formRoundTripPreservesOptionalWeightsAndAllSets() {
        val original = WorkoutEntry(id = "entry", date = "2026-11-02", workoutType = "strength",
            exerciseName = "Squat", sets = listOf(WorkoutSet(12, null), WorkoutSet(10, 0.0), WorkoutSet(8, 45.5)),
            createdAt = 1234L)
        assertEquals(original, WorkoutForm.from(original).toEntry(original).getOrThrow())
    }
    @Test fun repositoryValidationRejectsUnknownTypesAndMixedFields() {
        val entry = strength().toEntry().getOrThrow()
        assertNotNull(WorkoutValidation.error(entry.copy(workoutType = "swim")))
        assertNotNull(WorkoutValidation.error(entry.copy(distanceMiles = 2.0)))
        assertNotNull(WorkoutValidation.error(cardio().toEntry().getOrThrow().copy(sets = entry.sets)))
    }
}
