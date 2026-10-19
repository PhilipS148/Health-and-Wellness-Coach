package io.github.philips148.wellnesscoach.data.user

import io.github.philips148.wellnesscoach.data.model.UserProfile

// These operations apply to the currently signed-in user.
interface UserRepository {

    // Return the saved profile.
    // Null means the signed-in user has no profile document yet.
    suspend fun getProfile(): Result<UserProfile?>

    // Create or update the signed-in user's profile.
    // The profile's UID must match the authenticated user's UID.
    suspend fun saveProfile(
        profile: UserProfile
    ): Result<Unit>
}