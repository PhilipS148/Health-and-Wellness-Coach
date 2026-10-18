package io.github.philips148.wellnesscoach.util

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

object AuthErrorMessages {

    fun forRegistration(error: Throwable?): String {
        return commonMessage(error) ?: when (error) {
            is FirebaseAuthUserCollisionException ->
                "An account already uses this email. Sign in or reset your password."

            is FirebaseAuthWeakPasswordException ->
                "Please choose a password that meets all password requirements."

            is FirebaseAuthInvalidCredentialsException ->
                "Please enter a valid email address."

            else ->
                "Couldn't create your account. Please try again."
        }
    }

    fun forSignIn(error: Throwable?): String {
        return commonMessage(error) ?: when (error) {
            is FirebaseAuthInvalidCredentialsException,
            is FirebaseAuthInvalidUserException ->
                "Couldn't sign in. Check your email and password, then try again."

            else ->
                "Couldn't sign in. Please try again."
        }
    }

    fun forPasswordReset(error: Throwable?): String {
        return commonMessage(error) ?: when (error) {
            is FirebaseAuthInvalidCredentialsException ->
                "Please enter a valid email address."

            else ->
                "Couldn't request a reset link. Please try again."
        }
    }

    // Handle errors shared by all three authentication screens.
    // Null means the calling function should choose its own message.
    private fun commonMessage(error: Throwable?): String? {
        return when {
            error is FirebaseNetworkException ->
                "Couldn't connect. Check your internet connection and try again."

            error is FirebaseTooManyRequestsException ->
                "Too many attempts. Please wait and try again."

            error is FirebaseAuthInvalidUserException &&
                    error.errorCode == "ERROR_USER_DISABLED" ->
                "This account has been disabled."

            else -> null
        }
    }
}