package io.github.philips148.wellnesscoach.ui.steps

import io.github.philips148.wellnesscoach.data.model.StepTotal
import java.time.LocalDate

// Describes whether the phone is ready to provide steps.
enum class StepAccess {
    CHECKING,
    READY,
    PERMISSION_REQUIRED,
    PERMISSION_DENIED,
    UNAVAILABLE
}

// The ViewModel updates this information, and the screen displays it.
data class StepUiState(
    val date: String = LocalDate.now().toString(),

    // Null means no count is available yet.
    // Zero means a recorded count of zero steps.
    val steps: Int? = null,

    // Loaded from the signed-in user's profile.
    val dailyStepGoal: Int? = null,

    // Saved daily totals belonging to this account.
    val recentTotals: List<StepTotal> = emptyList(),

    val access: StepAccess = StepAccess.CHECKING,

    // Lets the screen offer a Google Play services fix when appropriate.
    val playServicesErrorCode: Int? = null,

    // Used to display loading indicators and prevent repeated requests.
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,

    // A phone count was read, but its save has not been confirmed.
    val hasPendingSave: Boolean = false,

    // Time of the latest successful phone reading, in epoch milliseconds.
    val lastUpdatedAt: Long? = null,

    val errorMessage: String? = null,
    val saveErrorMessage: String? = null
)