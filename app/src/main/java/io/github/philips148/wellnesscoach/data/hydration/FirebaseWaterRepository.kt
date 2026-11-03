package io.github.philips148.wellnesscoach.data.hydration

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import io.github.philips148.wellnesscoach.data.model.WaterEntry
import io.github.philips148.wellnesscoach.data.user.FirestorePaths
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirebaseWaterRepository(
    private val authRepository: AuthRepository,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : WaterRepository {

    private fun waterCollection() = db.collection(FirestorePaths.USERS)
        .document(authRepository.getCurrentUserId()
            ?: throw IllegalStateException("Please sign in to access water records."))
        .collection(FirestorePaths.WATER)

    private fun validate(entry: WaterEntry) {
        require(HydrationValues.validDate(entry.date)) { "Invalid water record date." }
        require(entry.amountOz > 0) { "Water amount must be greater than zero." }
    }

    private fun validateId(id: String) {
        require(id.isNotBlank() && '/' !in id) { "Invalid water record ID." }
    }

    override suspend fun createWaterEntry(entry: WaterEntry): Result<String> = result {
        validate(entry)
        val reference = waterCollection().document()
        reference.set(entry.copy(
            id = reference.id,
            createdAt = System.currentTimeMillis()
        )).await()
        reference.id
    }

    override suspend fun getWaterEntries(date: String): Result<List<WaterEntry>> = result {
        require(HydrationValues.validDate(date)) { "Invalid water record date." }
        waterCollection().whereEqualTo("date", date).get(Source.SERVER).await()
            .documents.map { document ->
                val entry = document.toObject(WaterEntry::class.java)
                    ?: throw IllegalStateException("Couldn't read a water record.")
                validate(entry)
                entry.copy(id = document.id)
            }.sortedWith(compareByDescending<WaterEntry> { it.createdAt }.thenBy { it.id })
    }

    override suspend fun updateWaterEntry(entry: WaterEntry): Result<Unit> = result {
        validate(entry)
        validateId(entry.id)
        val reference = waterCollection().document(entry.id)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            check(snapshot.exists()) { "This water entry no longer exists. Refresh the list." }
            val existing = snapshot.toObject(WaterEntry::class.java)
                ?: throw IllegalStateException("Couldn't read the water entry.")
            // The original creation timestamp is authoritative, not the UI's copy.
            transaction.set(reference, entry.copy(
                id = reference.id,
                createdAt = existing.createdAt
            ))
            null
        }.await()
    }

    override suspend fun deleteWaterEntry(id: String): Result<Unit> = result {
        validateId(id)
        val reference = waterCollection().document(id)
        db.runTransaction { transaction ->
            check(transaction.get(reference).exists()) {
                "This water entry no longer exists. Refresh the list."
            }
            transaction.delete(reference)
            null
        }.await()
    }

    private suspend fun <T> result(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}
