package io.github.philips148.wellnesscoach

import io.github.philips148.wellnesscoach.data.reminders.*
import io.github.philips148.wellnesscoach.reminders.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ReminderIntegrationTest {
    private class FakeSettings : ReminderPreferencesRepository {
        var value = ReminderPreferences()
        var fail = false
        var waitForLoad: CompletableDeferred<Unit>? = null
        // Simulates waiting for Firebase to finish saving.
        var waitForSave: CompletableDeferred<Unit>? = null
        override suspend fun load(userId: String): Result<ReminderPreferences> {
            val captured = value
            waitForLoad?.await()
            return if (fail) Result.failure(Exception("offline")) else Result.success(captured)
        }
        override suspend fun save(
            userId: String,
            preferences: ReminderPreferences
        ): Result<Unit> {
            // Wait until the test simulates the connection returning.
            waitForSave?.await()

            if (fail) return Result.failure(Exception("offline"))
            value = preferences
            return Result.success(Unit)
        }
    }
    @Test fun mapsEverySettingIncludingDisabledOnes() {
        val settings = ReminderPreferences(ReminderPreference(true, 8, 15),
            ReminderPreference(false, 20, 30), ReminderPreference(true, 17, 45))
        val schedules = settings.deliverySchedules()
        assertEquals(3, schedules.size)
        assertEquals(ReminderSchedule(ReminderKind.HYDRATION, true, 8, 15), schedules[0])
        assertEquals(ReminderSchedule(ReminderKind.CHECK_IN, false, 20, 30), schedules[1])
        assertEquals(ReminderSchedule(ReminderKind.WORKOUT, true, 17, 45), schedules[2])
    }
    @Test fun successfulLoadAndSaveSyncTheCorrectAccount() = runBlocking {
        val saved = FakeSettings()
        val calls = mutableListOf<Pair<String, ReminderPreferences>>()
        val repo = ScheduledReminderRepository(saved, saveScope = this) { uid, settings -> calls.add(uid to settings) }
        repo.load("accountA")
        val updated = ReminderPreferences(hydration = ReminderPreference(true, 9, 30))
        repo.save("accountB", updated)
        assertEquals(listOf("accountA" to ReminderPreferences(), "accountB" to updated), calls)
    }
    @Test fun failedLoadOrSaveDoesNotInstallUnsavedChoices() = runBlocking {
        val saved = FakeSettings().apply { fail = true }
        var calls = 0
        val repo = ScheduledReminderRepository(saved, saveScope = this) { _, _ -> calls++ }
        assertTrue(repo.load("A").isFailure)
        assertTrue(repo.save("A", ReminderPreferences()).isFailure)
        assertEquals(0, calls)
    }
    @Test fun oldLoadCannotReplaceANewerSave() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val saved = FakeSettings().apply { waitForLoad = gate }
        val calls = mutableListOf<ReminderPreferences>()
        val repo = ScheduledReminderRepository(saved, saveScope = this) { _, settings -> calls.add(settings) }
        val slowLoad = launch(start = CoroutineStart.UNDISPATCHED) { repo.load("A") }
        val updated = ReminderPreferences(workout = ReminderPreference(true, 18, 0))
        repo.save("A", updated)
        gate.complete(Unit)
        slowLoad.join()
        assertEquals(listOf(updated), calls)
    }

    // Make sure reminders still update if the user leaves while saving.
    @Test
    fun saveContinuesAfterLeavingScreen() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val saved = FakeSettings().apply { waitForSave = gate }
        val calls = mutableListOf<ReminderPreferences>()

        val repo = ScheduledReminderRepository(
            saved,
            saveScope = this
        ) { _, settings ->
            calls.add(settings)
        }

        val updated = ReminderPreferences(
            hydration = ReminderPreference(true, 18, 49)
        )

        // Start saving while the connection is unavailable.
        val screenJob = launch { repo.save("A", updated) }
        yield()
        yield()

        assertTrue(calls.isEmpty())

        // Simulate the user leaving the reminders screen.
        screenJob.cancelAndJoin()

        // Simulate Firebase reconnecting and finishing the save.
        gate.complete(Unit)

        // Wait for the application-level save to finish.
        coroutineContext[Job]!!.children.toList().joinAll()

        // Verify that the saved preferences and schedule were updated.
        assertEquals(updated, saved.value)
        assertEquals(listOf(updated), calls)
    }
}
