package io.github.philips148.wellnesscoach.data.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class DefaultAuthRepository(
    private val authDataSource: FirebaseAuthDataSource
) : AuthRepository {

    override fun getCurrentUserId(): String? {
        return authDataSource.getCurrentUserId()
    }

    override fun observeUserId(): Flow<String?> {
        return authDataSource.observeUserId()
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
        return try {
            val userId = authDataSource.signIn(
                email = email,
                password = password
            )

            Result.success(userId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override fun signOut() {
        authDataSource.signOut()
    }

    override suspend fun sendPasswordReset(
        email: String
    ): Result<Unit> {
        return try {
            authDataSource.sendPasswordReset(email)

            Result.success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
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