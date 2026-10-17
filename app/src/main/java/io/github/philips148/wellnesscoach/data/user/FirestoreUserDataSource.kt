package io.github.philips148.wellnesscoach.data.user

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreUserDataSource(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun deleteAllUserData(uid: String) {
        val userDoc = db.collection(FirestorePaths.USERS).document(uid)
        for(name in FirestorePaths.entry_collections) {
            deleteCollection(userDoc.collection((name)))
        }
        userDoc.delete().await()
    }

    private suspend fun deleteCollection(collection: CollectionReference) {
        while (true) {
            val snapshot = collection.limit(BATCH_SIZE).get().await()
            if (snapshot.isEmpty) {
                return
            }
            val batch = db.batch()

            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
    }

    private companion object {
        const val BATCH_SIZE = 400L
    }

}
