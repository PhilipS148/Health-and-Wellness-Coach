package io.github.philips148.wellnesscoach.ui.reminders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.reminders.*
import kotlinx.coroutines.launch

data class ReminderPreferencesUiState(
    val preferences: ReminderPreferences = ReminderPreferences(),
    val loading: Boolean = true,
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

class ReminderPreferencesViewModel(
    private val userId: String,
    private val repository: ReminderPreferencesRepository
) : ViewModel() {
    var uiState by mutableStateOf(ReminderPreferencesUiState())
        private set
    init { load() }

    fun load() {
        if (uiState.saving) return
        viewModelScope.launch {
            uiState = uiState.copy(loading = true, loaded = false, error = null, saved = false)
            repository.load(userId).fold(
                onSuccess = { uiState = ReminderPreferencesUiState(preferences = it, loading = false, loaded = true) },
                onFailure = { uiState = uiState.copy(loading = false, error = "Couldn't load reminders. Check your connection and retry.") }
            )
        }
    }

    fun change(preferences: ReminderPreferences) {
        if (uiState.loaded && !uiState.saving) uiState = uiState.copy(preferences = preferences, error = null, saved = false)
    }

    fun save() {
        if (!uiState.loaded || uiState.saving) return
        val preferences = uiState.preferences
        uiState = uiState.copy(saving = true, error = null, saved = false)
        viewModelScope.launch {
            repository.save(userId, preferences).fold(
                onSuccess = { uiState = uiState.copy(saving = false, saved = true) },
                onFailure = { uiState = uiState.copy(saving = false, error = "Couldn't save reminders. Your choices are kept. Check your connection and try again.") }
            )
        }
    }
}

class ReminderPreferencesViewModelFactory(
    private val userId: String,
    private val repository: ReminderPreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ReminderPreferencesViewModel::class.java))
        return ReminderPreferencesViewModel(userId, repository) as T
    }
}
