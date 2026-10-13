package io.github.philips148.wellnesscoach.data.auth

class DefaultAuthRepository(
    private val authDataSource: FirebaseAuthDataSource
) : AuthRepository {

    override fun getCurrentUserId(): String? {
        return authDataSource.getCurrentUserId()
    }

    override suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<String> {
        return Result.failure(
            UnsupportedOperationException(
                "Registration is not implemented yet."
            )
        )
    }

    override suspend fun signIn(
        email: String,
        password: String
    ): Result<String> {
        return Result.failure(
            UnsupportedOperationException(
                "Sign-in is not implemented yet."
            )
        )
    }

    override fun signOut() {
        authDataSource.signOut()
    }

    override suspend fun sendPasswordReset(
        email: String
    ): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException(
                "Password reset is not implemented yet."
            )
        )
    }

    override suspend fun deleteAccount(
        currentPassword: String
    ): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException(
                "Account deletion is not implemented yet."
            )
        )
    }
}