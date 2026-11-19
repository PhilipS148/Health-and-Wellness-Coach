package io.github.philips148.wellnesscoach.ui.progress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.model.SleepEntry
import io.github.philips148.wellnesscoach.data.summary.DailySummary
import io.github.philips148.wellnesscoach.data.summary.SummaryRepository
import io.github.philips148.wellnesscoach.data.summary.WeeklySummary
import io.github.philips148.wellnesscoach.data.wellness.WellnessRepository
import io.github.philips148.wellnesscoach.util.ProgressPeriods
import io.github.philips148.wellnesscoach.util.ProgressPeriods.weekStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

data class ProgressUiState(
    val isWeekly: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailySummary: DailySummary? = null,
    val weeklySummary: WeeklySummary? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val canGoNext: Boolean = false,
    val isEmpty: Boolean = false
)
class ProgressViewModel (
    private val summaryRepository: SummaryRepository
) : ViewModel() {

    var uiState by mutableStateOf(ProgressUiState())
    private set

            private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()

        val weekly = uiState.isWeekly
        val date = uiState.selectedDate

        uiState = uiState.copy(
            isLoading = true,
            errorMessage = null,
            canGoNext = ProgressPeriods.canGoNext(date, weekly, LocalDate.now())
        )

        refreshJob = viewModelScope.launch {
            try {
                if (weekly) {
                    val info = summaryRepository.getWeeklySummary(
                        ProgressPeriods.weekStart(date).toString()).getOrThrow()
                    uiState = uiState.copy(
                        weeklySummary = info,
                        dailySummary = null,
                        isEmpty = info.days.none { ProgressPeriods.hasData(it)},
                        isLoading = false
                    )
                } else {
                    val info = summaryRepository.getDailySummary(date.toString()).getOrThrow()
                    uiState = uiState.copy(
                        dailySummary = info,
                        weeklySummary = null,
                        isEmpty = !ProgressPeriods.hasData(info),
                        isLoading = false

                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "Couldn't load your progress. Check your connection and try again."
                )
            }
        }
    }
    fun onModeChange(weekly: Boolean) {
        if (weekly == uiState.isWeekly) {
            return
        } else {
            uiState = uiState.copy(
                isWeekly = weekly,
                selectedDate = LocalDate.now()
            )
        }
        refresh()
    }

    fun previous() {
        uiState = uiState.copy(
            selectedDate = ProgressPeriods.shift(uiState.selectedDate, uiState.isWeekly, -1)
        )

        refresh()
    }

    fun next() {
        if (!uiState.canGoNext) {
            return
        } else {
            uiState = uiState.copy(
                selectedDate = ProgressPeriods.shift(uiState.selectedDate, uiState.isWeekly, +1)
            )
        }
        refresh()
    }
}