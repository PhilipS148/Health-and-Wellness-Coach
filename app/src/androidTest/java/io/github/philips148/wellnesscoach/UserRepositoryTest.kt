package io.github.philips148.wellnesscoach

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.github.philips148.wellnesscoach.data.auth.FirebaseAuthDataSource
import io.github.philips148.wellnesscoach.data.model.TrackingCategories
import io.github.philips148.wellnesscoach.data.model.UserProfile
import io.github.philips148.wellnesscoach.data.user.DefaultUserRepository
import io.github.philips148.wellnesscoach.data.user.FirestorePaths
import io.github.philips148.wellnesscoach.data.user.FirestoreUserDataSource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class UserRepositoryTest {

    @Test
    fun profileStorageWorkflow() = runBlocking {
        val context =
            InstrumentationRegistry.getInstrumentation().targetContext

        val testId = UUID.randomUUID().toString()

        // Separate authentication session for this test.
        val testApp = FirebaseApp.initializeApp(
            context,
            FirebaseApp.getInstance().options,
            "profile-test-$testId"
        )

        val auth = FirebaseAuth.getInstance(testApp)
        val database = FirebaseFirestore.getInstance(testApp)

        val authDataSource = FirebaseAuthDataSource(auth)
        val userDataSource = FirestoreUserDataSource(database)

        val repository = DefaultUserRepository(
            authDataSource = authDataSource,
            userDataSource = userDataSource
        )

        val email = "profile-test-$testId@example.com"
        val password = "ProfileTest123!"
        var createdUserId: String? = null

        try {
            val uid = authDataSource.createAccount(email, password)
            createdUserId = uid

            // An account without a profile should return successful null.
            assertNull(repository.getProfile().getOrThrow())

            val originalProfile = UserProfile(
                uid = uid,
                email = email,
                displayName = "ProfileTest",
                heightCm = 175.0,
                weightKg = 72.5,
                wellnessGoals = listOf("Build consistent habits"),
                dailyCalorieGoal = 2300,
                dailyWaterGoalOz = 80,
                dailyStepGoal = 7000,
                sleepGoalHours = 7.5,
                selectedCategories = listOf(
                    TrackingCategories.HYDRATION,
                    TrackingCategories.WORKOUTS,
                    TrackingCategories.SLEEP
                )
            )

            repository.saveProfile(originalProfile).getOrThrow()

            // Read through a new repository instance.
            val freshRepository = DefaultUserRepository(
                authDataSource = FirebaseAuthDataSource(auth),
                userDataSource = FirestoreUserDataSource(database)
            )

            val loadedProfile = checkNotNull(
                freshRepository.getProfile().getOrThrow()
            )

            assertEquals(originalProfile, loadedProfile)

            // Change the username and clear optional measurements.
            // Previously saved goals and categories should remain intact.
            val updatedProfile = loadedProfile.copy(
                displayName = "UpdatedProfileTest",
                heightCm = null,
                weightKg = null
            )

            repository.saveProfile(updatedProfile).getOrThrow()

            assertEquals(
                updatedProfile,
                freshRepository.getProfile().getOrThrow()
            )

            // Invalid values must be rejected before they change saved data.
            val invalidProfiles = listOf(
                updatedProfile.copy(displayName = " "),
                updatedProfile.copy(heightCm = -1.0),
                updatedProfile.copy(weightKg = Double.NaN),
                updatedProfile.copy(dailyCalorieGoal = 0),
                updatedProfile.copy(dailyWaterGoalOz = 0),
                updatedProfile.copy(dailyStepGoal = -1),
                updatedProfile.copy(sleepGoalHours = 25.0),
                updatedProfile.copy(
                    selectedCategories = listOf("unknown")
                ),
                updatedProfile.copy(
                    selectedCategories = listOf(
                        TrackingCategories.SLEEP,
                        TrackingCategories.SLEEP
                    )
                )
            )

            for (invalidProfile in invalidProfiles) {
                val result = repository.saveProfile(invalidProfile)

                assertTrue(
                    "Invalid profile should be rejected.",
                    result.exceptionOrNull() is IllegalArgumentException
                )
            }

            // A profile belonging to a different UID must be rejected.
            val wrongOwnerResult = repository.saveProfile(
                updatedProfile.copy(uid = "other-$testId")
            )

            assertTrue(
                "A different user's profile should be rejected.",
                wrongOwnerResult.exceptionOrNull() is IllegalArgumentException
            )

            assertEquals(
                updatedProfile,
                repository.getProfile().getOrThrow()
            )

            // An unavailable server must produce failure, not missing data.
            database.disableNetwork().await()

            assertTrue(
                "An offline server read should fail.",
                repository.getProfile().isFailure
            )

            database.enableNetwork().await()

            // Signed-out users cannot read or save profiles.
            authDataSource.signOut()

            assertTrue(
                repository.getProfile().exceptionOrNull()
                        is IllegalStateException
            )

            assertTrue(
                repository.saveProfile(updatedProfile).exceptionOrNull()
                        is IllegalStateException
            )
        } finally {
            // Cleanup runs even when a test assertion fails.
            try {
                database.enableNetwork().await()

                val cleanupUid = createdUserId ?: auth.currentUser?.uid

                if (cleanupUid != null) {
                    if (auth.currentUser?.uid != cleanupUid) {
                        authDataSource.signIn(email, password)
                    }

                    check(auth.currentUser?.uid == cleanupUid)

                    database.collection(FirestorePaths.USERS)
                        .document(cleanupUid)
                        .delete()
                        .await()

                    authDataSource.deleteCurrentUser()
                }
            } finally {
                auth.signOut()
                database.terminate().await()
                database.clearPersistence().await()
            }
        }
    }
}
