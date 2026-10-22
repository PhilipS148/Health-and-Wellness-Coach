package io.github.philips148.wellnesscoach

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.github.philips148.wellnesscoach.data.auth.DefaultAuthRepository
import io.github.philips148.wellnesscoach.data.auth.FirebaseAuthDataSource
import io.github.philips148.wellnesscoach.data.user.FirestorePaths
import io.github.philips148.wellnesscoach.data.user.FirestoreUserDataSource
import io.github.philips148.wellnesscoach.ui.auth.RegistrationViewModel
import io.github.philips148.wellnesscoach.ui.navigation.AppNavHost
import io.github.philips148.wellnesscoach.ui.theme.HealthAndWellnessCoachTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RegistrationProfileTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun failedProfileSave_retryUsesSameAccount() {
        checkRecovery(retry = true)
    }

    @Test
    fun failedProfileSave_backSignsOut() {
        checkRecovery(retry = false)
    }

    private fun checkRecovery(retry: Boolean) = runBlocking {
        val context =
            InstrumentationRegistry.getInstrumentation().targetContext

        val testId = UUID.randomUUID().toString()

        val testApp = FirebaseApp.initializeApp(
            context,
            FirebaseApp.getInstance().options,
            "registration-test-$testId"
        )

        val auth = FirebaseAuth.getInstance(testApp)

        val dataApp = FirebaseApp.initializeApp(
            context,
            FirebaseApp.getInstance().options,
            "registration-data-$testId"
        )

        val dataAuth = FirebaseAuth.getInstance(dataApp)
        val database = FirebaseFirestore.getInstance(dataApp)

        val authDataSource = FirebaseAuthDataSource(auth)
        val userDataSource = FirestoreUserDataSource(database)

        val authRepository = DefaultAuthRepository(
            authDataSource = authDataSource,
            userDataSource = userDataSource
        )

        val email = "registration-test-$testId@example.com"
        val password = "RegistrationTest123!"
        var createdUserId: String? = null

        try {

            val userIdFlow = authRepository.observeUserId()

            composeRule.setContent {
                val userId by userIdFlow.collectAsState(initial = null)

                HealthAndWellnessCoachTheme {
                    AppNavHost(
                        userId = userId,
                        authRepository = authRepository,
                        onSignOutClick = authRepository::signOut
                    )
                }
            }

            // Open registration from the login screen.
            composeRule.onNode(
                hasText(
                    "Create",
                    substring = true,
                    ignoreCase = true
                ) and hasClickAction()
            ).performClick()

            // Retrieve the shared ViewModel created by AppNavHost.
            val viewModel = composeRule.runOnIdle {
                ViewModelProvider(composeRule.activity)[
                    RegistrationViewModel::class.java
                ]
            }

            composeRule.runOnIdle {
                viewModel.onEmailChange(email)
                viewModel.onUsernameChange("RegistrationTest")
                viewModel.onPasswordChange(password)
                viewModel.onConfirmPasswordChange(password)
                viewModel.register()
            }

            try {
                composeRule.waitUntil(timeoutMillis = 60_000) {
                    !viewModel.uiState.isLoading
                }
            } catch (exception: androidx.compose.ui.test.ComposeTimeoutException) {
                val state = composeRule.runOnUiThread {
                    viewModel.uiState
                }

                throw AssertionError(
                    "Registration stayed loading. " +
                            "Signed in: ${auth.currentUser != null}; " +
                            "Profile pending: ${state.isProfileSavePending}; " +
                            "Error: ${state.errorMessage}",
                    exception
                )
            }

            val registrationState = composeRule.runOnIdle {
                viewModel.uiState
            }

            org.junit.Assert.assertTrue(
                "Expected a profile-save failure. " +
                        "Signed in: ${auth.currentUser != null}; " +
                        "Successful: ${registrationState.registrationSuccessful}; " +
                        "Profile pending: ${registrationState.isProfileSavePending}; " +
                        "Error: ${registrationState.errorMessage}",
                registrationState.isProfileSavePending
            )

            val uid = checkNotNull(auth.currentUser?.uid)
            createdUserId = uid

            // Account exists, but registration must remain open.
            composeRule.onNodeWithText("Retry Profile Save")
                .assertIsDisplayed()

            composeRule.onNodeWithText("Dashboard")
                .assertDoesNotExist()

            // Original registration details must stay locked.
            for (label in listOf(
                "Email",
                "Username",
                "Password",
                "Confirm Password"
            )) {
                composeRule.onNodeWithText(label)
                    .assertIsNotEnabled()
            }

            if (retry) {
                dataAuth.signInWithEmailAndPassword(email, password).await()

                assertNull(userDataSource.getProfile(uid))

                composeRule.onNode(
                    hasText("Retry Profile Save") and hasClickAction()
                ).performClick()

                composeRule.waitUntil(timeoutMillis = 60_000) {
                    composeRule.onAllNodesWithText("Dashboard")
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }

                composeRule.onNodeWithText("Dashboard")
                    .assertIsDisplayed()

                // Retry must use the existing Authentication account.
                assertEquals(uid, auth.currentUser?.uid)

                val profile = checkNotNull(
                    userDataSource.getProfile(uid)
                )

                assertEquals(uid, profile.uid)
                assertEquals(email, profile.email)
                assertEquals("RegistrationTest", profile.displayName)
                assertEquals("", viewModel.uiState.password)
                assertEquals("", viewModel.uiState.confirmPassword)
            } else {
                composeRule.onNodeWithText("Back to Sign In")
                    .performClick()

                assertNull(auth.currentUser)

                composeRule.onNodeWithText("Username")
                    .assertDoesNotExist()

                composeRule.onNodeWithText("Dashboard")
                    .assertDoesNotExist()

                assertEquals("", viewModel.uiState.email)
                assertEquals("", viewModel.uiState.password)

                // Authenticate the database client to check for a saved profile.
                dataAuth.signInWithEmailAndPassword(email, password).await()

                assertNull(userDataSource.getProfile(uid))
            }
        } finally {
            try {
                // Stop any remaining ViewModel work before cleanup.
                composeRule.runOnUiThread {
                    composeRule.activity.viewModelStore.clear()
                }

                val cleanupUid = createdUserId ?: auth.currentUser?.uid

                if (cleanupUid != null) {
                    if (dataAuth.currentUser?.uid != cleanupUid) {
                        dataAuth.signInWithEmailAndPassword(
                            email,
                            password
                        ).await()
                    }

                    check(dataAuth.currentUser?.uid == cleanupUid)

                    database.collection(FirestorePaths.USERS)
                        .document(cleanupUid)
                        .delete()
                        .await()

                    // Delete only this test's temporary account.
                    checkNotNull(dataAuth.currentUser)
                        .delete()
                        .await()
                }
            } finally {
                auth.signOut()
                dataAuth.signOut()
                database.terminate().await()
                database.clearPersistence().await()
            }
        }
    }
}