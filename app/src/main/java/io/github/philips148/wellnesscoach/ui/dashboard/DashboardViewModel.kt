package io.github.philips148.wellnesscoach.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.summary.DailySummary
import io.github.philips148.wellnesscoach.data.summary.SummaryRepository
import io.github.philips148.wellnesscoach.data.summary.WeeklySummary
import io.github.philips148.wellnesscoach.data.user.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val displayName: String = "",
    val dailySummary: DailySummary? = null,
    val weeklySummary: WeeklySummary? = null,
    val progressMessage: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class DashboardViewModel(
    private val summaryRepository: SummaryRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    var uiState by mutableStateOf(DashboardUiState())
        private set

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()

        refreshJob = viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val today = LocalDate.now()

                val weekStart = today.minusDays(
                    (today.dayOfWeek.value - 1).toLong()
                )

                val profile = userRepository
                    .getProfile()
                    .getOrThrow()

                val dailySummary = summaryRepository
                    .getDailySummary(today.toString())
                    .getOrThrow()

                val weeklySummary = summaryRepository
                    .getWeeklySummary(weekStart.toString())
                    .getOrThrow()

                uiState = DashboardUiState(
                    displayName = profile?.displayName.orEmpty(),
                    dailySummary = dailySummary,
                    weeklySummary = weeklySummary,
                    progressMessage = createProgressMessage(
                        dailySummary
                    ),
                    isLoading = false
                )

            } catch (exception: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage =
                        exception.message ?: "Could not load dashboard."
                )
            }
        }
    }

    private fun createProgressMessage(
        summary: DailySummary
    ): String {
        if (
            summary.waterGoalOz > 0 &&
            summary.waterOz >= summary.waterGoalOz
        ) {
            return "You reached your hydration goal today."
        }

        if (
            summary.waterGoalOz > 0 &&
            summary.waterOz > 0
        ) {
            val remaining =
                (summary.waterGoalOz - summary.waterOz)
                    .coerceAtLeast(0)

            return "You have $remaining fl oz left to reach your hydration goal."
        }

        if (
            summary.steps != null &&
            summary.stepGoal > 0
        ) {
            val remaining =
                (summary.stepGoal - summary.steps)
                    .coerceAtLeast(0)

            return if (remaining == 0) {
                "You reached your step goal today."
            } else {
                "You have $remaining steps left to reach your step goal."
            }
        }

        return "Log your wellness activity to see your progress."
    }
}