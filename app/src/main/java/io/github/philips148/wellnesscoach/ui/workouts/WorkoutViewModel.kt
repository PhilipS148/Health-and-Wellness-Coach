package io.github.philips148.wellnesscoach.ui.workouts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.model.WorkoutEntry
import io.github.philips148.wellnesscoach.data.wellness.WellnessRepository
import io.github.philips148.wellnesscoach.data.wellness.WorkoutValidation
import kotlinx.coroutines.launch
import java.time.LocalDate

data class WorkoutUiState(
    val historyStart: String = LocalDate.now().minusDays(6).toString(),
    val historyEnd: String = LocalDate.now().toString(),
    val loadedStart: String = historyStart,
    val loadedEnd: String = historyEnd,
    val form: WorkoutForm = WorkoutForm(),
    val entries: List<WorkoutEntry> = emptyList(),
    val editing: WorkoutEntry? = null,
    val pendingDelete: WorkoutEntry? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val historyAvailable: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val busy: Boolean get() = isLoading || isSaving
}

class WorkoutViewModel(private val repository: WellnessRepository) : ViewModel() {
    var uiState by mutableStateOf(WorkoutUiState())
        private set

    init { refresh() }

    private fun changeForm(update: (WorkoutForm) -> WorkoutForm) {
        if (!uiState.busy) uiState = uiState.copy(
            form = update(uiState.form), errorMessage = null, successMessage = null
        )
    }
    fun onDateChange(value: String) = changeForm { it.copy(date = value) }
    fun onTypeChange(value: String) = changeForm { it.copy(type = value) }
    fun onNameChange(value: String) = changeForm { it.copy(name = value) }
    fun onDistanceChange(value: String) = changeForm { it.copy(distanceMiles = value) }
    fun onDurationChange(value: String) = changeForm { it.copy(durationMinutes = value) }
    fun addSet() = changeForm { it.copy(sets = it.sets + WorkoutSetInput()) }
    fun removeSet(index: Int) = changeForm { form ->
        form.copy(sets = form.sets.filterIndexed { i, _ -> i != index })
    }
    fun onRepsChange(index: Int, value: String) = changeForm { form ->
        form.copy(sets = form.sets.mapIndexed { i, set ->
            if (i == index) set.copy(reps = value) else set
        })
    }
    fun onWeightChange(index: Int, value: String) = changeForm { form ->
        form.copy(sets = form.sets.mapIndexed { i, set ->
            if (i == index) set.copy(weightLbs = value) else set
        })
    }
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
        val error = WorkoutValidation.rangeError(start, end)
        if (error != null) {
            uiState = uiState.copy(errorMessage = error, successMessage = null)
            return
        }
        uiState = uiState.copy(
            historyStart = start, historyEnd = end,
            isLoading = true, historyAvailable = false,
            pendingDelete = null, errorMessage = null, successMessage = null
        )
        viewModelScope.launch { load(start, end) }
    }

    private suspend fun load(start: String, end: String) {
        val result = repository.getWorkouts(start, end)
        if (result.isFailure) {
            uiState = uiState.copy(
                isLoading = false, historyAvailable = false,
                errorMessage = "Couldn't load workout history. Check your connection and tap Refresh / Retry."
            )
            return
        }
        uiState = uiState.copy(
            entries = result.getOrThrow(), loadedStart = start, loadedEnd = end,
            historyAvailable = true, isLoading = false, errorMessage = null
        )
    }

    fun edit(entry: WorkoutEntry) {
        if (!uiState.busy) uiState = uiState.copy(
            editing = entry, form = WorkoutForm.from(entry),
            errorMessage = null, successMessage = null
        )
    }
    fun cancelEdit() {
        if (!uiState.busy) uiState = uiState.copy(
            editing = null, form = WorkoutForm(), errorMessage = null, successMessage = null
        )
    }

    fun save() {
        if (uiState.busy) return
        val parsed = uiState.form.toEntry(uiState.editing)
        if (parsed.isFailure) {
            uiState = uiState.copy(
                errorMessage = parsed.exceptionOrNull()?.message,
                successMessage = null
            )
            return
        }
        val entry = parsed.getOrThrow()
        val editing = uiState.editing != null
        // Expand the loaded range if the saved date lies outside it.
        val start = minOf(uiState.loadedStart, entry.date)
        val end = maxOf(uiState.loadedEnd, entry.date)
        uiState = uiState.copy(isSaving = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val result = if (editing) repository.updateWorkout(entry)
                else repository.createWorkout(entry).map { Unit }
            if (result.isFailure) {
                uiState = uiState.copy(
                    isSaving = false,
                    errorMessage = "Couldn't save the workout. Your input is kept; please try again."
                )
                return@launch
            }
            uiState = uiState.copy(
                isSaving = false, isLoading = true, historyAvailable = false,
                editing = null, form = WorkoutForm(),
                historyStart = start, historyEnd = end,
                successMessage = "Workout saved."
            )
            load(start, end)
        }
    }

    fun requestDelete(entry: WorkoutEntry) {
        if (!uiState.busy) uiState = uiState.copy(pendingDelete = entry)
    }
    fun dismissDelete() {
        if (!uiState.busy) uiState = uiState.copy(pendingDelete = null)
    }
    fun confirmDelete() {
        if (uiState.busy) return
        val entry = uiState.pendingDelete ?: return
        val start = uiState.loadedStart
        val end = uiState.loadedEnd
        uiState = uiState.copy(
            pendingDelete = null, isSaving = true, errorMessage = null, successMessage = null
        )
        viewModelScope.launch {
            val result = repository.deleteWorkout(entry.id)
            if (result.isFailure) {
                uiState = uiState.copy(
                    isSaving = false,
                    errorMessage = "Couldn't delete this workout. Please refresh and try again."
                )
                return@launch
            }
            val wasEditingDeletedEntry = uiState.editing?.id == entry.id
            uiState = uiState.copy(
                isSaving = false, isLoading = true, historyAvailable = false,
                editing = if (wasEditingDeletedEntry) null else uiState.editing,
                form = if (wasEditingDeletedEntry) WorkoutForm() else uiState.form,
                historyStart = start, historyEnd = end,
                successMessage = "Workout deleted."
            )
            load(start, end)
        }
    }
}
