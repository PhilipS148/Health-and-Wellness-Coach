package io.github.philips148.wellnesscoach.data.wellness

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import io.github.philips148.wellnesscoach.data.model.CheckInEntry
import io.github.philips148.wellnesscoach.data.model.MealEntry
import io.github.philips148.wellnesscoach.data.model.SleepEntry
import io.github.philips148.wellnesscoach.data.model.WaterEntry
import io.github.philips148.wellnesscoach.data.model.WorkoutEntry
import io.github.philips148.wellnesscoach.data.user.FirestorePaths
import kotlinx.coroutines.tasks.await

class FirestoreWellnessDataSource(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Meals

    suspend fun createMeal(
        uid: String,
        entry: MealEntry
    ): String {
        val document = userCollection(uid, FirestorePaths.MEALS)
            .document()

        val savedEntry = entry.copy(id = document.id)

        document.set(savedEntry).await()

        return document.id
    }

    suspend fun getMeals(
        uid: String,
        startDate: String,
        endDate: String
    ): List<MealEntry> {
        return userCollection(uid, FirestorePaths.MEALS)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date")
            .get(Source.SERVER)
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(MealEntry::class.java)
                    ?.copy(id = document.id)
            }
    }

    suspend fun updateMeal(
        uid: String,
        entry: MealEntry
    ) {
        require(entry.id.isNotBlank()) {
            "Meal ID is required."
        }

        userCollection(uid, FirestorePaths.MEALS)
            .document(entry.id)
            .set(entry)
            .await()
    }

    suspend fun deleteMeal(
        uid: String,
        id: String
    ) {
        userCollection(uid, FirestorePaths.MEALS)
            .document(id)
            .delete()
            .await()
    }


    // Water

    suspend fun createWaterEntry(
        uid: String,
        entry: WaterEntry
    ): String {
        val document = userCollection(uid, FirestorePaths.WATER)
            .document()

        val savedEntry = entry.copy(id = document.id)

        document.set(savedEntry).await()

        return document.id
    }

    suspend fun getWaterEntries(
        uid: String,
        startDate: String,
        endDate: String
    ): List<WaterEntry> {
        return userCollection(uid, FirestorePaths.WATER)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date")
            .get(Source.SERVER)
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(WaterEntry::class.java)
                    ?.copy(id = document.id)
            }
    }

    suspend fun updateWaterEntry(
        uid: String,
        entry: WaterEntry
    ) {
        require(entry.id.isNotBlank()) {
            "Water entry ID is required."
        }

        userCollection(uid, FirestorePaths.WATER)
            .document(entry.id)
            .set(entry)
            .await()
    }

    suspend fun deleteWaterEntry(
        uid: String,
        id: String
    ) {
        userCollection(uid, FirestorePaths.WATER)
            .document(id)
            .delete()
            .await()
    }


    // Workouts

    suspend fun createWorkout(uid: String, entry: WorkoutEntry): String {
        WorkoutValidation.error(entry)?.let { throw IllegalArgumentException(it) }
        val document = userCollection(uid, FirestorePaths.WORKOUTS).document()
        val savedEntry = entry.copy(
            id = document.id,
            createdAt = System.currentTimeMillis()
        )
        document.set(savedEntry).await()
        return document.id
    }

    suspend fun getWorkouts(
        uid: String,
        startDate: String,
        endDate: String
    ): List<WorkoutEntry> {
        WorkoutValidation.rangeError(startDate, endDate)?.let {
            throw IllegalArgumentException(it)
        }
        return userCollection(uid, FirestorePaths.WORKOUTS)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date")
            .get(Source.SERVER).await().documents.map { document ->
                document.toObject(WorkoutEntry::class.java)?.copy(id = document.id)
                    ?: throw IllegalStateException("Couldn't read a workout record.")
            }.sortedWith(compareByDescending<WorkoutEntry> { it.date }
                .thenByDescending { it.createdAt }.thenBy { it.id })
    }

    suspend fun updateWorkout(uid: String, entry: WorkoutEntry) {
        WorkoutValidation.error(entry)?.let { throw IllegalArgumentException(it) }
        require(entry.id.isNotBlank() && '/' !in entry.id) { "Invalid workout ID." }
        val document = userCollection(uid, FirestorePaths.WORKOUTS).document(entry.id)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(document)
            check(snapshot.exists()) { "This workout no longer exists. Refresh history." }
            val existing = snapshot.toObject(WorkoutEntry::class.java)
                ?: throw IllegalStateException("Couldn't read the saved workout.")
            transaction.set(document, entry.copy(
                id = document.id,
                createdAt = existing.createdAt
            ))
            null
        }.await()
    }

    suspend fun deleteWorkout(uid: String, id: String) {
        require(id.isNotBlank() && '/' !in id) { "Invalid workout ID." }
        val document = userCollection(uid, FirestorePaths.WORKOUTS).document(id)
        db.runTransaction { transaction ->
            check(transaction.get(document).exists()) {
                "This workout no longer exists. Refresh history."
            }
            transaction.delete(document)
            null
        }.await()
    }


    // Sleep

    suspend fun createSleepEntry(
        uid: String,
        entry: SleepEntry
    ): String {
        val document = userCollection(uid, FirestorePaths.SLEEP)
            .document()

        val savedEntry = entry.copy(id = document.id)

        document.set(savedEntry).await()

        return document.id
    }

    suspend fun getSleepEntries(
        uid: String,
        startDate: String,
        endDate: String
    ): List<SleepEntry> {
        return userCollection(uid, FirestorePaths.SLEEP)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date")
            .get(Source.SERVER)
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(SleepEntry::class.java)
                    ?.copy(id = document.id)
            }
    }

    suspend fun updateSleepEntry(
        uid: String,
        entry: SleepEntry
    ) {
        require(entry.id.isNotBlank()) {
            "Sleep entry ID is required."
        }

        userCollection(uid, FirestorePaths.SLEEP)
            .document(entry.id)
            .set(entry)
            .await()
    }

    suspend fun deleteSleepEntry(
        uid: String,
        id: String
    ) {
        userCollection(uid, FirestorePaths.SLEEP)
            .document(id)
            .delete()
            .await()
    }


    // Daily wellness check-ins

    suspend fun createCheckIn(
        uid: String,
        entry: CheckInEntry
    ): String {
        val document = userCollection(uid, FirestorePaths.CHECK_INS)
            .document()

        val savedEntry = entry.copy(id = document.id)

        document.set(savedEntry).await()

        return document.id
    }

    suspend fun getCheckIns(
        uid: String,
        startDate: String,
        endDate: String
    ): List<CheckInEntry> {
        return userCollection(uid, FirestorePaths.CHECK_INS)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date")
            .get(Source.SERVER)
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(CheckInEntry::class.java)
                    ?.copy(id = document.id)
            }
    }

    suspend fun updateCheckIn(
        uid: String,
        entry: CheckInEntry
    ) {
        require(entry.id.isNotBlank()) {
            "Check-in ID is required."
        }

        userCollection(uid, FirestorePaths.CHECK_INS)
            .document(entry.id)
            .set(entry)
            .await()
    }

    suspend fun deleteCheckIn(
        uid: String,
        id: String
    ) {
        userCollection(uid, FirestorePaths.CHECK_INS)
            .document(id)
            .delete()
            .await()
    }


    // All wellness entries are stored underneath the user's document.
    private fun userCollection(
        uid: String,
        collectionName: String
    ) = db.collection(FirestorePaths.USERS)
        .document(uid)
        .collection(collectionName)
}