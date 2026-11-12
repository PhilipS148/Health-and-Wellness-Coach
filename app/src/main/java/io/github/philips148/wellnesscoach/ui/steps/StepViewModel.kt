package io.github.philips148.wellnesscoach.ui.steps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import io.github.philips148.wellnesscoach.data.auth.AuthRepository
import io.github.philips148.wellnesscoach.data.model.StepTotal
import io.github.philips148.wellnesscoach.data.model.UserProfile
import io.github.philips148.wellnesscoach.data.steps.PhoneStepDataSource
import io.github.philips148.wellnesscoach.data.steps.StepRepository
import io.github.philips148.wellnesscoach.data.user.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.util.concurrent.TimeoutException

class StepViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val stepRepository: StepRepository,
    private val phoneStepDataSource: PhoneStepDataSource
) : ViewModel() {

    var uiState by mutableStateOf(StepUiState())
        private set

    // This ViewModel belongs to the account that opened the screen.
    private val ownerUid = authRepository.getCurrentUserId()

    // Keep the latest reading available if its save needs another attempt.
    private var pendingTotal: StepTotal? = null

    init {
        refresh()
    }

    // Load saved information, then read and save today's phone count
    // when the device and permission are ready.
    fun refresh() {
        if (uiState.isLoading || uiState.isRefreshing || !checkAccount()) {
            return
        }

        val today = LocalDate.now()
        val date = today.toString()
        val dayChanged = uiState.date != date
        val accessMessage = updateAccess()

        // Clear yesterday's displayed count when the local date changes.
        uiState = uiState.copy(
            date = date,
            steps = if (dayChanged) null else uiState.steps,
            lastUpdatedAt = if (dayChanged) null else uiState.lastUpdatedAt,
            isLoading = true,
            errorMessage = accessMessage
        )

        viewModelScope.launch {
            try {
                val loadErrors = loadSavedData(today)

                if (!checkAccount()) return@launch

                // Permission or service availability may have changed
                // while the saved information was loading.
                val currentAccessMessage = updateAccess()

                uiState = uiState.copy(
                    errorMessage = (
                            listOfNotNull(currentAccessMessage) + loadErrors
                            ).joinToString("\n").ifBlank { null }
                )

                if (uiState.access != StepAccess.READY) return@launch

                uiState = uiState.copy(
                    isLoading = false,
                    isRefreshing = true
                )

                // Confirm an older day's pending save before replacing it
                // with a reading for a different day.
                val previous = pendingTotal

                if (
                    previous != null &&
                    previous.date != date &&
                    !saveTotal(previous)
                ) {
                    return@launch
                }

                val reading = request {
                    phoneStepDataSource.readTodayTotal()
                }

                if (!checkAccount()) return@launch

                val total = reading.getOrElse { error ->
                    showReadError(error)
                    return@launch
                }

                // Display the actual phone reading immediately.
                // Saving it to Firestore is a separate operation.
                pendingTotal = total

                uiState = uiState.copy(
                    date = total.date,
                    steps = total.steps,
                    lastUpdatedAt = System.currentTimeMillis(),
                    hasPendingSave = true,
                    saveErrorMessage = null
                )

                saveTotal(total)
            } finally {
                uiState = uiState.copy(
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
    }

    // Retry the existing reading instead of adding its steps again.
    fun retrySave() {
        val total = pendingTotal ?: return

        if (uiState.isLoading || uiState.isRefreshing || !checkAccount()) {
            return
        }

        uiState = uiState.copy(
            isRefreshing = true,
            saveErrorMessage = null
        )

        viewModelScope.launch {
            try {
                saveTotal(total)
            } finally {
                uiState = uiState.copy(isRefreshing = false)
            }
        }
    }

    // The screen calls this after Android's permission dialog finishes.
    fun onPermissionResult(granted: Boolean) {
        if (!checkAccount()) return

        if (granted) {
            refresh()
        } else {
            uiState = uiState.copy(
                access = StepAccess.PERMISSION_DENIED,
                steps = null,
                lastUpdatedAt = null,
                errorMessage = "Physical activity access is required to record steps."
            )
        }
    }

    // Load the profile goal and the last seven calendar days of saved totals.
    // A failure in one request still allows us to try the other.
    private suspend fun loadSavedData(today: LocalDate): List<String> {
        val errors = mutableListOf<String>()

        val profile = request {
            userRepository.getProfile().getOrThrow()
        }

        if (!checkAccount()) return errors

        if (profile.isSuccess) {
            val goal = profile.getOrNull()?.dailyStepGoal
                ?: UserProfile().dailyStepGoal

            if (goal > 0) {
                uiState = uiState.copy(dailyStepGoal = goal)
            } else {
                uiState = uiState.copy(dailyStepGoal = null)
                errors.add("Could not load a valid step goal.")
            }
        } else {
            errors.add("Could not load your step goal. Please refresh.")
        }

        val history = request {
            stepRepository.getStepTotals(
                startDate = today.minusDays(6).toString(),
                endDate = today.toString()
            ).getOrThrow()
        }

        if (!checkAccount()) return errors

        if (history.isSuccess) {
            val totals = history.getOrThrow()
                .sortedByDescending { it.date }

            val pendingCount = pendingTotal
                ?.takeIf { it.date == uiState.date }
                ?.steps

            val savedCount = totals
                .firstOrNull { it.date == uiState.date }
                ?.steps

            // Preserve a newer phone reading when loading saved history.
            uiState = uiState.copy(
                recentTotals = totals,
                steps = pendingCount ?: uiState.steps ?: savedCount
            )
        } else {
            errors.add("Could not load saved step history. Please refresh.")
        }

        return errors
    }

    // Determine which setup or availability state the screen should show.
    // Return an explanation when the device or service is unavailable.
    private fun updateAccess(): String? {
        if (!phoneStepDataSource.hasStepCounter()) {
            uiState = uiState.copy(
                access = StepAccess.UNAVAILABLE,
                playServicesErrorCode = null
            )
            return "This device does not support phone-based step tracking."
        }

        val servicesCode = phoneStepDataSource.getPlayServicesStatus()

        if (servicesCode != ConnectionResult.SUCCESS) {
            uiState = uiState.copy(
                access = StepAccess.UNAVAILABLE,
                playServicesErrorCode = servicesCode
            )
            return "Google Play services must be available and up to date."
        }

        val access = when {
            phoneStepDataSource.hasActivityPermission() ->
                StepAccess.READY

            uiState.access == StepAccess.PERMISSION_DENIED ->
                StepAccess.PERMISSION_DENIED

            else ->
                StepAccess.PERMISSION_REQUIRED
        }

        uiState = uiState.copy(
            access = access,
            playServicesErrorCode = null
        )

        return null
    }

    // Save under the existing date ID and update the displayed history
    // only after Firestore confirms write.
    private suspend fun saveTotal(total: StepTotal): Boolean {
        if (!checkAccount()) return false

        val saved = request {
            stepRepository.saveDailyTotal(total).getOrThrow()
        }

        if (!checkAccount()) return false

        if (saved.isFailure) {
            uiState = uiState.copy(
                hasPendingSave = true,
                saveErrorMessage =
                    "Could not confirm the total for ${total.date} was saved. " +
                            "Check your connection and retry."
            )
            return false
        }

        if (pendingTotal == total) {
            pendingTotal = null
        }

        // Replace this date in the list so refreshes cannot create duplicates.
        val totals = (
                uiState.recentTotals.filterNot { it.date == total.date } + total
                ).sortedByDescending { it.date }.take(7)

        uiState = uiState.copy(
            recentTotals = totals,
            hasPendingSave = pendingTotal != null,
            saveErrorMessage = null
        )

        return true
    }

    // Turn phone-reading failures into useful screen states.
    // A failed reading never becomes a recorded zero.
    private fun showReadError(error: Throwable) {
        if (error is SecurityException) {
            onPermissionResult(granted = false)
            return
        }

        val code = (error as? ApiException)?.statusCode

        if (
            code == ConnectionResult.API_UNAVAILABLE ||
            code == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ||
            code == ConnectionResult.SERVICE_MISSING ||
            code == ConnectionResult.SERVICE_DISABLED
        ) {
            uiState = uiState.copy(
                access = StepAccess.UNAVAILABLE,
                playServicesErrorCode = code,
                errorMessage =
                    "Phone step recording is unavailable. Check Google Play services."
            )
        } else {
            val accessMessage = updateAccess()

            uiState = uiState.copy(
                errorMessage = accessMessage ?: if (error is TimeoutException) {
                    "Reading steps took too long. Please refresh."
                } else {
                    "Could not read your phone's steps. Please refresh."
                }
            )
        }
    }

    // Clear this account's displayed information if the user signs out
    // or switches accounts while a request is running.
    private fun checkAccount(): Boolean {
        if (
            ownerUid != null &&
            authRepository.getCurrentUserId() == ownerUid
        ) {
            return true
        }

        pendingTotal = null
        uiState = StepUiState(
            access = StepAccess.UNAVAILABLE,
            errorMessage = "Please sign in again to view your steps."
        )

        return false
    }

    // Limit each request to 20 seconds and return ordinary errors as Result.
    // Cancellation from the ViewModel's lifetime still propagates normally.
    private suspend fun <T> request(
        block: suspend () -> T
    ): Result<T> {
        return withTimeoutOrNull(20_000L) {
            try {
                Result.success(block())
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        } ?: Result.failure(TimeoutException("Request timed out."))
    }
}