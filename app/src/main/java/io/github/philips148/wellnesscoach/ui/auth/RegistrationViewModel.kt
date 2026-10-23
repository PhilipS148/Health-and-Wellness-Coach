package io.github.philips148.wellnesscoach.ui.auth
import io.github.philips148.wellnesscoach.util.AuthErrorMessages
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.github.philips148.wellnesscoach.util.PasswordValidator
import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import io.github.philips148.wellnesscoach.data.auth.RegistrationProfileException
import kotlinx.coroutines.launch

data class RegistrationUiState(
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isProfileSavePending: Boolean = false,
    val errorMessage: String? = null,
    val registrationSuccessful: Boolean = false
)

class RegistrationViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(RegistrationUiState())
        private set

    fun onEmailChange(value: String) {
        uiState = uiState.copy(
            email = value,
            errorMessage = null
        )
    }

    fun onUsernameChange(value: String) {
        uiState = uiState.copy(
            username = value,
            errorMessage = null
        )
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(
            password = value,
            errorMessage = null
        )
    }

    fun onConfirmPasswordChange(value: String) {
        uiState = uiState.copy(
            confirmPassword = value,
            errorMessage = null
        )
    }

    fun register() {
        if (uiState.isLoading) {
            return
        }

        val validationError = validateRegistration()

        if (validationError != null) {
            uiState = uiState.copy(errorMessage = validationError)
            return
        }

        // Capture the submitted values before starting the request.
        val email = uiState.email.trim()
        val password = uiState.password
        val username = uiState.username.trim()

        uiState = uiState.copy(
            isLoading = true,
            errorMessage = null,
            registrationSuccessful = false
        )

        viewModelScope.launch {
            val result = authRepository.register(
                email = email,
                password = password,
                username = username
            )

            uiState = if (result.isSuccess) {
                uiState.copy(
                    password = "",
                    confirmPassword = "",
                    isLoading = false,
                    isProfileSavePending = false,
                    errorMessage = null,
                    registrationSuccessful = true
                )
            } else {
                val error = result.exceptionOrNull()
                val profilePending = error is RegistrationProfileException

                uiState.copy(
                    isLoading = false,
                    isProfileSavePending = profilePending,
                    errorMessage = if (profilePending) {
                        error?.message
                    } else {
                        AuthErrorMessages.forRegistration(error)
                    }
                )
            }
        }
    }

    // Clear the form when starting again or after successful registration.
    fun resetForm() {
        if (uiState.isLoading || uiState.isProfileSavePending) {
            return
        }

        uiState = RegistrationUiState()
    }

    // Leave registration after a failed attempt.
    fun cancelRegistration() {
        if (uiState.isLoading) {
            return
        }

        if (uiState.isProfileSavePending) {
            authRepository.signOut()
        }

        uiState = RegistrationUiState()
    }

    private fun validateRegistration(): String? {

        if (uiState.email.isBlank()) {
            return "Email is required."
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(uiState.email.trim())
                .matches()
        ) {
            return "Please enter a valid email address."
        }

        if (uiState.username.isBlank()) {
            return "Username is required."
        }

        val passwordError =
            PasswordValidator.validate(uiState.password)

        if (passwordError != null) {
            return passwordError
        }

        if (uiState.password != uiState.confirmPassword) {
            return "Passwords do not match."
        }

        return null
    }
}