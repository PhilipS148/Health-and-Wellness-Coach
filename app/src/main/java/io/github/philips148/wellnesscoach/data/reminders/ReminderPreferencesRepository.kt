package io.github.philips148.wellnesscoach.data.reminders

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

interface ReminderPreferencesRepository {
    suspend fun load(userId: String): Result<ReminderPreferences>
    suspend fun save(userId: String, preferences: ReminderPreferences): Result<Unit>
}

class FirestoreReminderPreferencesRepository(
    private val auth: AuthRepository,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ReminderPreferencesRepository {
    override suspend fun load(userId: String): Result<ReminderPreferences> = safely {
        requireCurrentAccount(userId)
        val snapshot = db.collection("users").document(userId).get(Source.SERVER).await()
        requireCurrentAccount(userId)
        ReminderPreferences.fromFields(snapshot.get("reminderPreferences"))
    }

    override suspend fun save(userId: String, preferences: ReminderPreferences): Result<Unit> = safely {
        requireCurrentAccount(userId)
        // Update only this field, preserving profile, goals, and tracking categories.
        db.collection("users").document(userId).set(
            mapOf("reminderPreferences" to preferences.toFields()),
            SetOptions.mergeFields("reminderPreferences")
        ).await()
        requireCurrentAccount(userId)
    }

    private fun requireCurrentAccount(userId: String) {
        require(userId.isNotBlank() && auth.getCurrentUserId() == userId) { "Sign in again to manage reminders." }
    }

    private suspend fun <T> safely(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) { throw cancelled }
      catch (error: Exception) { Result.failure(error) }
}
