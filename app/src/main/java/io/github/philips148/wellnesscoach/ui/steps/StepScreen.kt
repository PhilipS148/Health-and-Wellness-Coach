package io.github.philips148.wellnesscoach.ui.steps

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.common.GoogleApiAvailability
import io.github.philips148.wellnesscoach.data.model.StepTotal
import io.github.philips148.wellnesscoach.ui.theme.HealthAndWellnessCoachTheme
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import kotlin.math.roundToLong

@Composable
fun StepScreen(
    uiState: StepUiState,
    onRefreshClick: () -> Unit,
    onRetrySaveClick: () -> Unit,
    onEnableStepsClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onFixPlayServicesClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val busy = uiState.isLoading || uiState.isRefreshing

    // Offer Google's repair action only for problems it can help resolve.
    val canFixServices = uiState.playServicesErrorCode?.let { code ->
        GoogleApiAvailability.getInstance().isUserResolvableError(code)
    } == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Shared navy header, matching the other feature screens.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Health and Wellness Coach",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Text(
                text = "Phone-based steps",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Text(
                text = "Your daily activity, in one place.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBackClick) {
                Text("Back to Dashboard")
            }

            // Steps are associated with the phone's local calendar date.
            StepCard {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(formatStepDate(uiState.date))
            }

            // Show the appropriate setup, permission, or count view.
            when (uiState.access) {
                StepAccess.CHECKING -> {
                    StepCard {
                        Text("Checking step tracking…")
                    }
                }

                StepAccess.PERMISSION_REQUIRED -> {
                    StepCard {
                        Text(
                            text = "Steps are off",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "— steps",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Allow physical activity access to start " +
                                    "recording steps on this phone."
                        )

                        Button(
                            onClick = onEnableStepsClick,
                            enabled = !uiState.isRefreshing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Enable Steps")
                        }
                    }
                }

                StepAccess.PERMISSION_DENIED -> {
                    StepCard {
                        Text(
                            text = "Activity access denied",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "— steps",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Step tracking is off. You can allow physical " +
                                    "activity access in this app's settings."
                        )

                        OutlinedButton(
                            onClick = onOpenSettingsClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Open Settings")
                        }
                    }
                }

                StepAccess.UNAVAILABLE -> {
                    StepCard {
                        Text(
                            text = "Steps unavailable",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "— steps",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            uiState.errorMessage
                                ?: "Phone-based step tracking is unavailable."
                        )
                        Text("Other wellness logs are still available.")

                        if (canFixServices) {
                            Button(
                                onClick = onFixPlayServicesClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Fix Google Play services")
                            }
                        }

                        OutlinedButton(
                            onClick = onBackClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Back to Dashboard")
                        }
                    }
                }

                StepAccess.READY -> {
                    val count = uiState.steps
                    val goal = uiState.dailyStepGoal

                    // Calculate progress only when both values are available.
                    val ratio = if (
                        count != null && goal != null && goal > 0
                    ) {
                        count.toDouble() / goal.toDouble()
                    } else {
                        null
                    }

                    StepCard(
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (goal != null) {
                                "Daily goal: ${formatStepNumber(goal)} steps"
                            } else {
                                "Daily goal unavailable"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = count?.let {
                                "${formatStepNumber(it)} steps"
                            } ?: "— steps",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // The bar fills at 100%; the actual count stays visible.
                        if (ratio != null) {
                            val progress = ratio.coerceIn(0.0, 1.0).toFloat()

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = Color(0xFF59AF51),
                                trackColor = MaterialTheme.colorScheme.onPrimary,
                                gapSize = 0.dp,
                                drawStopIndicator = {}
                            )
                        }
                    }

                    if (ratio != null) {
                        val percent = (ratio * 100).roundToLong()

                        Text(
                            text = "${formatStepNumber(percent)}% of your daily goal",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Format the refresh time using the phone's time settings.
                    uiState.lastUpdatedAt?.let { timestamp ->
                        val time = DateFormat.getTimeFormat(context)
                            .format(Date(timestamp))

                        Text(
                            text = "Last refreshed at $time",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = onRefreshClick,
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            if (busy) "Refreshing…" else "Refresh Steps"
                        )
                    }

                    StepCard(
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Collected from this phone",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your phone records steps while activity " +
                                    "access is enabled. Your first day's count " +
                                    "may be partial.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Availability explanations are already shown in their own card.
            if (uiState.access != StepAccess.UNAVAILABLE) {
                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Keep a real phone reading visible while its save is unconfirmed.
            if (uiState.hasPendingSave) {
                StepCard {
                    Text(
                        text = uiState.saveErrorMessage
                            ?: "Waiting for the step total to be saved.",
                        color = if (uiState.saveErrorMessage != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    OutlinedButton(
                        onClick = onRetrySaveClick,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            if (busy) "Saving…" else "Retry Save"
                        )
                    }
                }
            }

            // Allow saved information and availability to be checked again
            // even when the phone reader is currently unavailable.
            if (uiState.access != StepAccess.READY) {
                OutlinedButton(
                    onClick = onRefreshClick,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (busy) "Loading…" else "Refresh / Retry"
                    )
                }
            }

            Text(
                text = "Recent step history",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Today's count is shown above; list earlier saved dates here.
            val earlierTotals = uiState.recentTotals.filterNot {
                it.date == uiState.date
            }

            if (earlierTotals.isEmpty()) {
                Text(
                    text = if (uiState.isLoading) {
                        "Loading saved history…"
                    } else {
                        "No earlier step totals saved yet."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                earlierTotals.forEach { total ->
                    StepCard {
                        Text(
                            text = if (
                                total.date == LocalDate.now()
                                    .minusDays(1).toString()
                            ) {
                                "Yesterday"
                            } else {
                                formatStepDate(total.date)
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Text("${formatStepNumber(total.steps)} steps")
                    }
                }
            }
        }
    }
}

// Reuse the same rounded card layout throughout this screen.
@Composable
private fun StepCard(
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

// Display dates and numbers using the device's language conventions.
private fun formatStepDate(value: String): String {
    return LocalDate.parse(value).format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    )
}

private fun formatStepNumber(value: Number): String {
    return NumberFormat.getIntegerInstance().format(value)
}

// Sample values for Android Studio's preview; no Firebase or phone calls.
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun StepScreenPreview() {
    val today = LocalDate.now()

    HealthAndWellnessCoachTheme {
        StepScreen(
            uiState = StepUiState(
                date = today.toString(),
                steps = 6200,
                dailyStepGoal = 10000,
                access = StepAccess.READY,
                lastUpdatedAt = System.currentTimeMillis(),
                recentTotals = listOf(
                    StepTotal(
                        id = today.minusDays(1).toString(),
                        date = today.minusDays(1).toString(),
                        steps = 5980
                    )
                )
            ),
            onRefreshClick = {},
            onRetrySaveClick = {},
            onEnableStepsClick = {},
            onOpenSettingsClick = {},
            onFixPlayServicesClick = {},
            onBackClick = {}
        )
    }
}