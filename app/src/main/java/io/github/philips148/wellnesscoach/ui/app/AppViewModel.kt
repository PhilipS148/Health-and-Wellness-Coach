package io.github.philips148.wellnesscoach.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Store the current user ID. Null means no user is currently signed in.
    private val _userId = MutableStateFlow(
        authRepository.getCurrentUserId()
    )

    // Allow the interface to observe the value without changing it directly.
    val userId: StateFlow<String?> = _userId.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeUserId().collect { currentUserId ->
                _userId.value = currentUserId
            }
        }
    }

    // Read the latest user ID from the repository when requested.
    fun refreshUserId() {
        _userId.value = authRepository.getCurrentUserId()
    }
}