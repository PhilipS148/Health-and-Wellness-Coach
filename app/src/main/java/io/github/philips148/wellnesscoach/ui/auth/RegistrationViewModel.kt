package io.github.philips148.wellnesscoach.ui.auth
import io.github.philips148.wellnesscoach.util.AuthErrorMessages
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import io.github.philips148.wellnesscoach.util.PasswordValidator

data class RegistrationUiState(
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrationSuccessful: Boolean = false
)

class RegistrationViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

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
        val validationError = validateRegistration()

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

        auth.createUserWithEmailAndPassword(
            uiState.email.trim(),
            uiState.password
        ).addOnSuccessListener {
            uiState = uiState.copy(
                isLoading = false,
                registrationSuccessful = true,
                errorMessage = null
            )
        }.addOnFailureListener { exception ->
            uiState = uiState.copy(
                isLoading = false,
                errorMessage = AuthErrorMessages.forRegistration(exception)
            )
        }
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