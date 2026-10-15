package io.github.philips148.wellnesscoach.data.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun observeUserId(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }

        auth.addAuthStateListener(listener)

        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    suspend fun signIn(
        email: String,
        password: String
    ): String {
        val result = auth.signInWithEmailAndPassword(
            email.trim(),
            password
        ).await()

        return result.user?.uid
            ?: throw IllegalStateException(
                "Sign-in completed without a user."
            )
    }

    fun signOut() {
        auth.signOut()
    }
}