package io.github.philips148.wellnesscoach.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import kotlinx.coroutines.launch

data class DeleteAccountUiState(
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isDeleted: Boolean = false
)

class DeleteAccountViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    var uiState by mutableStateOf(DeleteAccountUiState())
        private set

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(
            password = value,
            errorMessage = null
        )
    }

    fun deleteAccount() {
        if (uiState.isLoading) {
            return
        }
        val password = uiState.password
        if (password.isBlank()) {
            uiState = uiState.copy(errorMessage = "Password is required")
            return
        }
        uiState = uiState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = authRepository.deleteAccount(password)

            uiState = if (result.isSuccess) {
                uiState.copy(
                    isLoading = false,
                    isDeleted = true,
                    password = ""
                )
            } else {
                uiState.copy(
                    isLoading = false,
                    errorMessage = "Could not delete account please try again " +
                            "and make sure password is correct"
                )
            }
        }
    }
}
