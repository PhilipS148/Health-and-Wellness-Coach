package io.github.philips148.wellnesscoach.data.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import io.github.philips148.wellnesscoach.data.user.FirestoreUserDataSource
import io.github.philips148.wellnesscoach.data.model.UserProfile
import io.github.philips148.wellnesscoach.util.PasswordValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultAuthRepository(
    private val authDataSource: FirebaseAuthDataSource,
    private val userDataSource: FirestoreUserDataSource
) : AuthRepository {

    // Prevent simultaneous registration requests.
    private val registrationMutex = Mutex()

    // Keep the initial profile available if saving it needs another attempt.
    private var pendingRegistrationProfile: UserProfile? = null

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
        return registrationMutex.withLock {
            try {
                val cleanEmail = email.trim()
                val cleanUsername = username.trim()

                require(cleanUsername.isNotEmpty()) {
                    "Username is required."
                }

                val pendingProfile = pendingRegistrationProfile
                val profile: UserProfile

                if (pendingProfile == null) {
                    check(authDataSource.getCurrentUserId() == null) {
                        "Sign out before creating another account."
                    }

                    val passwordError = PasswordValidator.validate(password)

                    if (passwordError != null) {
                        throw IllegalArgumentException(passwordError)
                    }

                    val uid = authDataSource.createAccount(
                        email = cleanEmail,
                        password = password
                    )

                    profile = UserProfile(
                        uid = uid,
                        email = cleanEmail,
                        displayName = cleanUsername
                    )

                    pendingRegistrationProfile = profile
                } else {
                    // Retry the profile save for the account already created.
                    check(
                        authDataSource.getCurrentUserId() == pendingProfile.uid
                    ) {
                        "Your sign-in session changed. Please sign in again."
                    }

                    require(
                        cleanEmail.equals(
                            pendingProfile.email,
                            ignoreCase = true
                        ) && cleanUsername == pendingProfile.displayName
                    ) {
                        "Retry using the original registration details."
                    }

                    profile = pendingProfile
                }

                try {
                    userDataSource.createProfileIfMissing(profile)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    throw RegistrationProfileException(exception)
                }

                pendingRegistrationProfile = null
                Result.success(profile.uid)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }
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
        pendingRegistrationProfile = null
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
        val uid = authDataSource.getCurrentUserId()
        if (uid == null) {
            return Result.failure(IllegalStateException("User is not signed in"))
        }

        return try {
            authDataSource.reauthenticate(currentPassword)
            userDataSource.deleteAllUserData(uid)
            authDataSource.deleteCurrentUser()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    }
}