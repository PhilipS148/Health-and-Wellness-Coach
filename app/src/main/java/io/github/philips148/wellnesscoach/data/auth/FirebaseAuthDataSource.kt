package io.github.philips148.wellnesscoach.data.auth

import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun signOut() {
        auth.signOut()
    }
}