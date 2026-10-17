package io.github.philips148.wellnesscoach.ui.passwordreset

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import kotlinx.coroutines.launch

data class PasswordResetUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val requestSuccessful: Boolean = false
)

class PasswordResetViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(PasswordResetUiState())
        private set

    fun onEmailChange(value: String) {
        uiState = uiState.copy(
            email = value,
            errorMessage = null,
            requestSuccessful = false
        )
    }

    fun sendPasswordReset() {
        if (uiState.isLoading) {
            return
        }

        val email = uiState.email.trim()

        val validationError = when {
            email.isBlank() -> "Email is required."

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "Please enter a valid email address."

            else -> null
        }

        if (validationError != null) {
            uiState = uiState.copy(
                errorMessage = validationError
            )
            return
        }

        uiState = uiState.copy(
            email = email,
            isLoading = true,
            errorMessage = null,
            requestSuccessful = false
        )

        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)

            uiState = if (result.isSuccess) {
                uiState.copy(
                    isLoading = false,
                    errorMessage = null,
                    requestSuccessful = true
                )
            } else {
                uiState.copy(
                    isLoading = false,
                    errorMessage =
                        "Couldn't request a reset link. " +
                                "Please check your connection and try again.",
                    requestSuccessful = false
                )
            }
        }
    }

    fun onRequestSuccessHandled() {
        uiState = uiState.copy(
            requestSuccessful = false
        )
    }
}