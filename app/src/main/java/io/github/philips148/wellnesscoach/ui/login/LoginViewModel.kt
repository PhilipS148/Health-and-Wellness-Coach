package io.github.philips148.wellnesscoach.ui.login
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import kotlinx.coroutines.launch
import io.github.philips148.wellnesscoach.util.AuthErrorMessages

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun onEmailChange(value: String) {
        uiState = uiState.copy(
            email = value,
            errorMessage = null
        )
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(
            password = value,
            errorMessage = null
        )
    }

    fun signIn() {
        if (uiState.isLoading) {
            return
        }

        val email = uiState.email.trim()
        val password = uiState.password

        val validationError = when {
            email.isBlank() -> "Email is required."

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "Please enter a valid email address."

            password.isEmpty() -> "Password is required."

            else -> null
        }

        if (validationError != null) {
            uiState = uiState.copy(
                errorMessage = validationError
            )
            return
        }

        uiState = uiState.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = authRepository.signIn(
                email = email,
                password = password
            )

            uiState = if (result.isSuccess) {
                uiState.copy(
                    password = "",
                    isLoading = false,
                    errorMessage = null
                )
            } else {
                uiState.copy(
                    isLoading = false,
                    errorMessage = AuthErrorMessages.forSignIn(
                        result.exceptionOrNull()
                    )
                )
            }
        }
    }
}