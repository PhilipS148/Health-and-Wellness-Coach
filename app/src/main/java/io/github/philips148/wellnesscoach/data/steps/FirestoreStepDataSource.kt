package io.github.philips148.wellnesscoach.data.steps

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import io.github.philips148.wellnesscoach.data.model.StepTotal
import io.github.philips148.wellnesscoach.data.user.FirestorePaths
import kotlinx.coroutines.tasks.await

class FirestoreStepDataSource(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun saveDailyTotal(uid: String, total: StepTotal) {
        // Reuse the date ID so refreshing replaces that day's total.
        stepsCollection(uid)
            .document(total.date)
            .set(total.copy(id = total.date))
            .await()
    }

    suspend fun getStepTotals(
        uid: String,
        startDate: String,
        endDate: String
    ): List<StepTotal> {
        val snapshot = stepsCollection(uid)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date")
            .get(Source.SERVER)
            .await()

        return snapshot.documents.map { document ->
            val total = document.toObject(StepTotal::class.java)
                ?: throw IllegalStateException(
                    "Could not read a saved step total."
                )

            total.copy(id = document.id)
        }
    }

    private fun stepsCollection(uid: String): CollectionReference {
        return db.collection(FirestorePaths.USERS)
            .document(uid)
            .collection(FirestorePaths.STEPS)
    }
}