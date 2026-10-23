package io.github.philips148.wellnesscoach.data.user

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import io.github.philips148.wellnesscoach.data.model.UserProfile

class FirestoreUserDataSource(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    // Read the profile from Firestore.
    // Null means the user does not have a profile document yet.
    suspend fun getProfile(uid: String): UserProfile? {
        val snapshot = db.collection(FirestorePaths.USERS)
            .document(uid)
            .get(Source.SERVER)
            .await()

        if (!snapshot.exists()) {
            return null
        }

        val profile = snapshot.toObject(UserProfile::class.java)
            ?: throw IllegalStateException(
                "Could not read the saved profile."
            )

        return profile.copy(uid = uid)
    }

    // Save the supplied profile information.
    suspend fun saveProfile(profile: UserProfile) {
        db.collection(FirestorePaths.USERS)
            .document(profile.uid)
            .set(profile, SetOptions.merge())
            .await()
    }

    // Create the initial profile while preserving an existing one.
    suspend fun createProfileIfMissing(profile: UserProfile) {
        val profileDocument = db.collection(FirestorePaths.USERS)
            .document(profile.uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(profileDocument)

            if (!snapshot.exists()) {
                transaction.set(profileDocument, profile)
            }

            null
        }.await()
    }
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
