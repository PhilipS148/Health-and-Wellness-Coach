package io.github.philips148.wellnesscoach

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import io.github.philips148.wellnesscoach.util.AuthErrorMessages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthErrorMessagesTest {

    @Test
    fun disabledAccount_showsDisabledMessageForEveryFlow() {
        val error = FirebaseAuthInvalidUserException(
            "ERROR_USER_DISABLED",
            "Technical disabled-account details."
        )

        val expected = "This account has been disabled."

        assertEquals(
            listOf(expected, expected, expected),
            messagesFor(error)
        )
    }

    @Test
    fun tooManyRequests_showsRetryMessageForEveryFlow() {
        val error = FirebaseTooManyRequestsException(
            "Technical request-limit details."
        )

        val expected = "Too many attempts. Please wait and try again."

        assertEquals(
            listOf(expected, expected, expected),
            messagesFor(error)
        )
    }

    @Test
    fun unexpectedErrors_showGeneralMessagesWithoutTechnicalDetails() {
        val detail = "INTERNAL_DIAGNOSTIC_FOR_TEST_ONLY"

        val errors = listOf(
            FirebaseAuthException("ERROR_INTERNAL_ERROR", detail),
            IllegalStateException(detail)
        )

        val expected = listOf(
            "Couldn't create your account. Please try again.",
            "Couldn't sign in. Please try again.",
            "Couldn't request a reset link. Please try again."
        )

        for (error in errors) {
            val messages = messagesFor(error)

            assertEquals(expected, messages)

            for (message in messages) {
                assertFalse(message.contains(detail))
            }
        }
    }

    // Check registration, login, and password reset for the same error.
    private fun messagesFor(error: Throwable): List<String> {
        return listOf(
            AuthErrorMessages.forRegistration(error),
            AuthErrorMessages.forSignIn(error),
            AuthErrorMessages.forPasswordReset(error)
        )
    }
}