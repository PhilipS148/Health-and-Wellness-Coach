package io.github.philips148.wellnesscoach.data.user

import io.github.philips148.wellnesscoach.data.auth.FirebaseAuthDataSource
import io.github.philips148.wellnesscoach.data.model.TrackingCategories
import io.github.philips148.wellnesscoach.data.model.UserProfile
import kotlinx.coroutines.CancellationException

class DefaultUserRepository(
    private val authDataSource: FirebaseAuthDataSource,
    private val userDataSource: FirestoreUserDataSource
) : UserRepository {

    override suspend fun getProfile(): Result<UserProfile?> {
        return try {
            val uid = requireUserId()
            val profile = userDataSource.getProfile(uid)

            Result.success(profile)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun saveProfile(
        profile: UserProfile
    ): Result<Unit> {
        return try {
            val uid = requireUserId()

            require(profile.uid == uid) {
                "This profile does not belong to the signed-in user."
            }

            validateProfile(profile)

            userDataSource.saveProfile(
                profile.copy(
                    displayName = profile.displayName.trim()
                )
            )

            Result.success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    // Get the UID from authentication.
    private fun requireUserId(): String {
        return authDataSource.getCurrentUserId()
            ?: throw IllegalStateException(
                "You must be signed in to access your profile."
            )
    }

    private fun validateProfile(profile: UserProfile) {
        require(profile.displayName.isNotBlank()) {
            "Username is required."
        }

        profile.heightCm?.let { height ->
            require(height.isFinite() && height > 0) {
                "Height must be a positive number."
            }
        }

        profile.weightKg?.let { weight ->
            require(weight.isFinite() && weight > 0) {
                "Weight must be a positive number."
            }
        }

        require(profile.dailyCalorieGoal > 0) {
            "Daily calorie goal must be greater than zero."
        }

        require(profile.dailyWaterGoalOz > 0) {
            "Daily water goal must be greater than zero."
        }

        require(profile.dailyStepGoal > 0) {
            "Daily step goal must be greater than zero."
        }

        require(
            profile.sleepGoalHours > 0 &&
                    profile.sleepGoalHours <= 24
        ) {
            "Sleep goal must be greater than 0 and no more than 24 hours."
        }

        require(
            profile.selectedCategories.all {
                it in TrackingCategories.ALL
            }
        ) {
            "An unknown tracking category was selected."
        }

        require(
            profile.selectedCategories.distinct().size ==
                    profile.selectedCategories.size
        ) {
            "Tracking categories cannot contain duplicates."
        }
    }
}
