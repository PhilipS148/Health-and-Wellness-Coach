package io.github.philips148.wellnesscoach.data.auth

import com.google.firebase.auth.EmailAuthProvider
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

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
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

    // Create the authentication account and return its user ID.
    suspend fun createAccount(
        email: String,
        password: String
    ): String {
        val result = auth.createUserWithEmailAndPassword(
            email.trim(),
            password
        ).await()

        return result.user?.uid
            ?: throw IllegalStateException(
                "Account creation completed without a user."
            )
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

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun reauthenticate(password: String) {
        val user = auth.currentUser
        if (user == null) {
            throw IllegalStateException("User is not signed in")
        }

        val email = user.email
        if (email == null) {
            throw IllegalStateException("No email address")
        }

        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential).await()
    }

    suspend fun deleteCurrentUser() {
        val user = auth.currentUser
        if (user == null) {
            throw IllegalStateException("User is not signed in")
        }

        user.delete().await()
    }
}