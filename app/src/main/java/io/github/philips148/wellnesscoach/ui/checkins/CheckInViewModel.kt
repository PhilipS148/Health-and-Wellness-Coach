package io.github.philips148.wellnesscoach.ui.checkins

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.model.CheckInEntry
import io.github.philips148.wellnesscoach.data.model.TrackingCategories
import io.github.philips148.wellnesscoach.data.user.UserRepository
import io.github.philips148.wellnesscoach.data.wellness.WellnessRepository
import io.github.philips148.wellnesscoach.data.wellness.CheckInValidation
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CheckInUiState(
    val form: CheckInForm = CheckInForm(),
    val historyStart: String = LocalDate.now().minusDays(6).toString(),
    val historyEnd: String = LocalDate.now().toString(),
    val loadedStart: String = historyStart,
    val loadedEnd: String = historyEnd,
    val selectedCategories: Set<String> = emptySet(),
    val preferencesLoaded: Boolean = false,
    val preferenceError: String? = null,
    val historyError: String? = null,
    val historyAvailable: Boolean = false,
    val entries: List<CheckInEntry> = emptyList(),
    val editing: CheckInEntry? = null,
    val pendingDelete: CheckInEntry? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val busy: Boolean get() = isLoading || isSaving
}

class CheckInViewModel(
    private val repository: WellnessRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    var uiState by mutableStateOf(CheckInUiState())
        private set

    init { refresh() }

    private fun changeForm(update: (CheckInForm) -> CheckInForm) {
        if (!uiState.busy) uiState = uiState.copy(
            form = update(uiState.form), errorMessage = null, successMessage = null
        )
    }
    fun onDateChange(value: String) = changeForm { it.copy(date = value) }
    fun onStressChange(value: String) = changeForm { it.copy(stress = value) }
    fun onMoodChange(value: String) = changeForm { it.copy(mood = value) }
    fun onEnergyChange(value: String) = changeForm { it.copy(energy = value) }
    fun onHistoryStartChange(value: String) {
        if (!uiState.busy) uiState = uiState.copy(historyStart = value)
    }
    fun onHistoryEndChange(value: String) {
        if (!uiState.busy) uiState = uiState.copy(historyEnd = value)
    }

    fun refresh() {
        if (uiState.busy) return
        val start = uiState.historyStart.trim()
        val end = uiState.historyEnd.trim()
        val error = CheckInValidation.rangeError(start, end)
        if (error != null) {
            uiState = uiState.copy(errorMessage = error, successMessage = null)
            return
        }
        uiState = uiState.copy(
            historyStart = start, historyEnd = end,
            isLoading = true, preferencesLoaded = false, historyAvailable = false,
            pendingDelete = null, preferenceError = null, historyError = null,
            errorMessage = null, successMessage = null
        )
        viewModelScope.launch {
            val result = userRepository.getProfile()
            val profile = result.getOrNull()
            uiState = if (result.isFailure) {
                uiState.copy(preferenceError = "Couldn't load tracking preferences. Check your connection and retry.")
            } else if (profile == null) {
                uiState.copy(preferenceError = "Complete your profile before recording check-ins.")
            } else {
                uiState.copy(preferencesLoaded = true,
                    selectedCategories = profile.selectedCategories.filter {
                        it in setOf(TrackingCategories.STRESS, TrackingCategories.MOOD, TrackingCategories.ENERGY)
                    }.toSet())
            }
            loadHistory(start, end)
        }
    }

    private suspend fun loadHistory(start: String, end: String) {
        val result = repository.getCheckIns(start, end)
        uiState = if (result.isSuccess) {
            uiState.copy(entries = result.getOrThrow(), loadedStart = start, loadedEnd = end,
                historyAvailable = true, historyError = null, isLoading = false)
        } else {
            uiState.copy(historyAvailable = false, isLoading = false,
                historyError = "Couldn't load check-in history. Check your connection and tap Refresh / Retry.")
        }
    }

    fun edit(entry: CheckInEntry) {
        if (!uiState.busy && uiState.preferencesLoaded) uiState = uiState.copy(
            editing = entry, form = CheckInForm.from(entry), errorMessage = null, successMessage = null
        )
    }
    fun cancelEdit() {
        if (!uiState.busy) uiState = uiState.copy(
            editing = null, form = CheckInForm(), errorMessage = null, successMessage = null
        )
    }

    fun save() {
        if (uiState.busy || !uiState.preferencesLoaded) return
        val parsed = uiState.form.toEntry(uiState.selectedCategories, uiState.editing)
        if (parsed.isFailure) {
            uiState = uiState.copy(errorMessage = parsed.exceptionOrNull()?.message, successMessage = null)
            return
        }
        val entry = parsed.getOrThrow()
        val editing = uiState.editing != null
        val start = minOf(uiState.loadedStart, entry.date)
        val end = maxOf(uiState.loadedEnd, entry.date)
        uiState = uiState.copy(isSaving = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val result = if (editing) repository.updateCheckIn(entry)
                else repository.createCheckIn(entry).map { Unit }
            if (result.isFailure) {
                uiState = uiState.copy(isSaving = false,
                    errorMessage = "Couldn't save this check-in. Your input is kept; please try again.")
                return@launch
            }
            uiState = uiState.copy(isSaving = false, isLoading = true, historyAvailable = false,
                form = CheckInForm(), editing = null, historyStart = start, historyEnd = end,
                successMessage = "Check-in saved.")
            loadHistory(start, end)
        }
    }

    fun requestDelete(entry: CheckInEntry) {
        if (!uiState.busy && uiState.historyAvailable) uiState = uiState.copy(pendingDelete = entry)
    }
    fun dismissDelete() {
        if (!uiState.busy) uiState = uiState.copy(pendingDelete = null)
    }
    fun confirmDelete() {
        if (uiState.busy) return
        val entry = uiState.pendingDelete ?: return
        val start = uiState.loadedStart
        val end = uiState.loadedEnd
        uiState = uiState.copy(pendingDelete = null, isSaving = true,
            errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val result = repository.deleteCheckIn(entry.id)
            if (result.isFailure) {
                uiState = uiState.copy(isSaving = false,
                    errorMessage = "Couldn't delete this check-in. Please refresh and try again.")
                return@launch
            }
            val deletingEdited = uiState.editing?.id == entry.id
            uiState = uiState.copy(isSaving = false, isLoading = true, historyAvailable = false,
                editing = if (deletingEdited) null else uiState.editing,
                form = if (deletingEdited) CheckInForm() else uiState.form,
                historyStart = start, historyEnd = end, successMessage = "Check-in deleted.")
            loadHistory(start, end)
        }
    }
}
