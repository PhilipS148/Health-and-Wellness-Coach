package io.github.philips148.wellnesscoach.data.auth

interface AuthRepository {

    // Return the signed-in user's ID, or null when signed out.
    fun getCurrentUserId(): String?

    // Create the account and save its initial username.
    // Return the new user's ID when successful.
    suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<String>

    // Sign in and return the user's ID when successful.
    suspend fun signIn(
        email: String,
        password: String
    ): Result<String>

    // End the current user's session.
    fun signOut()

    // Request a password-reset email.
    suspend fun sendPasswordReset(email: String): Result<Unit>

    // Verify the password, then delete the account
    // and all associated app data.
    suspend fun deleteAccount(
        currentPassword: String
    ): Result<Unit>
}