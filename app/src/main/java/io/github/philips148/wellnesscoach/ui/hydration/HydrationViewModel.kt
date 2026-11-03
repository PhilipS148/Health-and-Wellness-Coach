package io.github.philips148.wellnesscoach.ui.hydration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.hydration.HydrationValues
import io.github.philips148.wellnesscoach.data.hydration.WaterRepository
import io.github.philips148.wellnesscoach.data.model.WaterEntry
import io.github.philips148.wellnesscoach.data.user.UserRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

// Use the device's local calendar day; records are never reassigned by time zone.
data class HydrationUiState(
    val selectedDate: String = LocalDate.now().toString(),
    val dateInput: String = selectedDate,
    val entryDate: String = selectedDate,
    val amount: String = "",
    val entries: List<WaterEntry> = emptyList(),
    val goalOz: Int? = null,
    val editing: WaterEntry? = null,
    val pendingDelete: WaterEntry? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val dataAvailable: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val busy: Boolean get() = isLoading || isSaving
    val totalOz: Long get() = HydrationValues.totalForDate(entries, selectedDate)
    val progress: Float get() = HydrationValues.progress(totalOz, goalOz ?: 0)
}

class HydrationViewModel(
    private val waterRepository: WaterRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    var uiState by mutableStateOf(HydrationUiState())
        private set

    init { refresh() }

    fun onDateInputChange(value: String) {
        if (!uiState.busy) uiState = uiState.copy(dateInput = value)
    }

    fun onEntryDateChange(value: String) {
        if (!uiState.busy) uiState = uiState.copy(
            entryDate = value, errorMessage = null, successMessage = null
        )
    }

    fun onAmountChange(value: String) {
        if (!uiState.busy) uiState = uiState.copy(
            amount = value, errorMessage = null, successMessage = null
        )
    }

    fun refresh() {
        if (uiState.busy) return
        val date = uiState.dateInput.trim()
        if (!HydrationValues.validDate(date)) {
            uiState = uiState.copy(errorMessage = "Enter a real date using yyyy-MM-dd.")
            return
        }
        uiState = uiState.copy(
            selectedDate = date, dateInput = date, entryDate = date,
            amount = "", editing = null, pendingDelete = null,
            isLoading = true, dataAvailable = false,
            errorMessage = null, successMessage = null
        )
        viewModelScope.launch { load(date) }
    }

    private suspend fun load(date: String) {
        val profileResult = userRepository.getProfile()
        if (profileResult.isFailure) {
            loadFailed("Couldn't load your saved hydration goal. Check your connection and retry.")
            return
        }
        val goal = profileResult.getOrNull()?.dailyWaterGoalOz
        if (goal == null || goal <= 0) {
            loadFailed("Complete your profile and save a positive hydration goal before logging water.")
            return
        }
        val entriesResult = waterRepository.getWaterEntries(date)
        if (entriesResult.isFailure) {
            loadFailed("Couldn't load water records. Check your connection and tap Refresh / Retry.")
            return
        }
        uiState = uiState.copy(
            entries = entriesResult.getOrThrow(), goalOz = goal,
            isLoading = false, isSaving = false, dataAvailable = true,
            errorMessage = null
        )
    }

    private fun loadFailed(message: String) {
        uiState = uiState.copy(
            isLoading = false, isSaving = false,
            dataAvailable = false, errorMessage = message
        )
    }

    fun edit(entry: WaterEntry) {
        if (uiState.busy || !uiState.dataAvailable) return
        uiState = uiState.copy(
            editing = entry, entryDate = entry.date, amount = entry.amountOz.toString(),
            errorMessage = null, successMessage = null
        )
    }

    fun cancelEdit() {
        if (!uiState.busy) uiState = uiState.copy(
            editing = null, entryDate = uiState.selectedDate, amount = "",
            errorMessage = null, successMessage = null
        )
    }

    fun save() {
        if (uiState.busy || !uiState.dataAvailable) return
        val date = uiState.entryDate.trim()
        val error = HydrationValues.inputError(date, uiState.amount)
        if (error != null) {
            uiState = uiState.copy(errorMessage = error, successMessage = null)
            return
        }
        val editing = uiState.editing
        val entry = (editing ?: WaterEntry()).copy(
            date = date, amountOz = uiState.amount.trim().toInt()
        )
        uiState = uiState.copy(isSaving = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val result = if (editing == null) {
                waterRepository.createWaterEntry(entry).map { Unit }
            } else {
                waterRepository.updateWaterEntry(entry)
            }
            if (result.isFailure) {
                uiState = uiState.copy(
                    isSaving = false,
                    errorMessage = "Couldn't save water. Your input is kept; please try again."
                )
                return@launch
            }
            // Show the saved entry's day, including when an edit moves its date.
            uiState = uiState.copy(
                selectedDate = date, dateInput = date, entryDate = date,
                amount = "", editing = null, isSaving = false,
                isLoading = true, dataAvailable = false,
                successMessage = "Water entry saved."
            )
            load(date)
        }
    }

    fun requestDelete(entry: WaterEntry) {
        if (!uiState.busy && uiState.dataAvailable) {
            uiState = uiState.copy(pendingDelete = entry)
        }
    }

    fun dismissDelete() {
        if (!uiState.busy) uiState = uiState.copy(pendingDelete = null)
    }

    fun confirmDelete() {
        if (uiState.busy || !uiState.dataAvailable) return
        val entry = uiState.pendingDelete ?: return
        val date = uiState.selectedDate
        uiState = uiState.copy(
            pendingDelete = null, isSaving = true,
            errorMessage = null, successMessage = null
        )
        viewModelScope.launch {
            val result = waterRepository.deleteWaterEntry(entry.id)
            if (result.isFailure) {
                uiState = uiState.copy(
                    isSaving = false,
                    errorMessage = "Couldn't delete this entry. Please refresh and try again."
                )
                return@launch
            }
            uiState = uiState.copy(
                editing = null, amount = "", entryDate = date,
                isSaving = false, isLoading = true, dataAvailable = false,
                successMessage = "Water entry deleted."
            )
            load(date)
        }
    }
}
