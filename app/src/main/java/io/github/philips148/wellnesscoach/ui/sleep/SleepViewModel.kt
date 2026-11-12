package io.github.philips148.wellnesscoach.ui.sleep

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.model.SleepEntry
import io.github.philips148.wellnesscoach.data.wellness.WellnessRepository
import io.github.philips148.wellnesscoach.util.SleepValidator
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SleepUiState(
    val entries: List<SleepEntry> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val dateText: String = LocalDate.now().toString(),
    val hoursText: String = "",
    val minutesText: String = "",
    val editingEntry: SleepEntry? = null,
    val formError: String? = null,
    val isSaving: Boolean = false
) {
    val busy: Boolean get() = isLoading || isSaving
}

class SleepViewModel(
    private val wellnessRepository: WellnessRepository
) : ViewModel() {

    var uiState by mutableStateOf(SleepUiState())
        private set

    init {
        loadEntries()
    }

    fun loadEntries() {
        uiState = uiState.copy(
            isLoading = true,
            loadError = null
        )

        viewModelScope.launch {
            val today = LocalDate.now()
            val result = wellnessRepository.getSleepEntries(
                today.minusDays(29).toString(),
                today.toString()
            )

            result.onSuccess { list ->
                uiState = uiState.copy(
                    entries = list.sortedByDescending { it.date },
                    isLoading = false
                )
            }

            result.onFailure {
                uiState = uiState.copy(
                    loadError = "Could no load sleep history.",
                    isLoading = false
                )
            }
        }
    }


        fun onDateChange(text: String) {
            uiState = uiState.copy(
                dateText = text,
                formError = null
            )
        }

        fun onHoursChange(text: String) {
            uiState = uiState.copy(
                hoursText = text,
                formError = null
            )
        }

        fun onMinutesChange(text: String) {
            uiState = uiState.copy(
                minutesText = text,
                formError = null
            )
        }

        fun startEditing(entry: SleepEntry) {
            val (hours, minutes) = SleepValidator.splitHours(entry.hoursSlept)

            uiState = uiState.copy(
                editingEntry = entry,
                dateText = entry.date,
                hoursText = hours.toString(),
                minutesText = minutes.toString(),
                formError = null
            )
        }

        fun cancelEditing() {
            uiState = SleepUiState(
                entries = uiState.entries,
                isLoading = false
            )
        }

        fun save() {
            if (uiState.isSaving) {
                return
            }

            val dateError = SleepValidator.dateError(uiState.dateText, LocalDate.now())
            if (dateError != null) {
                uiState = uiState.copy(formError = dateError)
                return
            }

            val durationError = SleepValidator.durationError(uiState.hoursText, uiState.minutesText)
            if (durationError != null) {
                uiState = uiState.copy(formError = durationError)
                return
            }

            val hours = SleepValidator.toHours(uiState.hoursText, uiState.minutesText)
            val date = uiState.dateText
            val editing = uiState.editingEntry

            uiState = uiState.copy(
                isSaving = true
            )
            viewModelScope.launch {
                val result: Result<Unit>
                if (editing == null) {
                    result = wellnessRepository.createSleepEntry(
                        SleepEntry(date = date, hoursSlept = hours)
                    ).map {}

                } else {
                    result = wellnessRepository.updateSleepEntry(
                        editing.copy(date = date, hoursSlept = hours)
                    )
                }

                result.onSuccess {
                    cancelEditing()
                    loadEntries()
                }

                result.onFailure {
                    uiState = uiState.copy(
                        formError = "Save failed",
                        isSaving = false
                    )
                }
            }
        }

        fun delete(entry: SleepEntry) {
            viewModelScope.launch {
                val result = wellnessRepository.deleteSleepEntry(entry.id)

                result.onSuccess {
                    if (uiState.editingEntry?.id == entry.id) {
                        cancelEditing()
                    }

                    loadEntries()
                }

                result.onFailure {
                    uiState = uiState.copy(formError = "Entry couldn't be deleted")
                }

            }
        }


    }
