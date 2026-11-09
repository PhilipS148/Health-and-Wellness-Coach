package io.github.philips148.wellnesscoach.ui.sleep

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.philips148.wellnesscoach.data.model.SleepEntry
import io.github.philips148.wellnesscoach.ui.theme.HealthAndWellnessCoachTheme
import io.github.philips148.wellnesscoach.util.SleepValidator

@Composable
fun SleepScreen(
    uiState: SleepUiState = SleepUiState(isLoading = false),
    onBack: () -> Unit = {},
    onDateChange: (String) -> Unit = {},
    onHoursChange: (String) -> Unit = {},
    onMinutesChange: (String) -> Unit = {},
    onSave: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onEdit: (SleepEntry) -> Unit = {},
    onDelete: (SleepEntry) -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<SleepEntry?>(null) }

    BackHandler(enabled = uiState.isSaving) { }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Health and Wellness Coach",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "Sleep", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "Rest better.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            TextButton(onClick = onBack, enabled = !uiState.isSaving) {
                Text("Back to Dashboard")
            }

            Text(
                if (uiState.editingEntry == null) "Log Sleep" else "Edit sleep entry",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
            )

            SleepField(
                "Wake-up date (yyyy-MM-dd)", uiState.dateText,
                onDateChange, !uiState.busy
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SleepField(
                    "Hours", uiState.hoursText, onHoursChange,
                    !uiState.busy, KeyboardType.Number, Modifier.weight(1f)
                )
                SleepField(
                    "Minutes", uiState.minutesText, onMinutesChange,
                    !uiState.busy, KeyboardType.Number, Modifier.weight(1f)
                )
            }

            Text(
                "Use the date you woke up. Up to 24 hours per entry.",
                style = MaterialTheme.typography.bodySmall
            )

            uiState.formError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = onSave, enabled = !uiState.busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    when {
                        uiState.isSaving -> "Saving..."
                        uiState.editingEntry != null -> "Save Changes"
                        else -> "Save Sleep"
                    }
                )
            }

            if (uiState.editingEntry != null) {
                TextButton(onClick = onCancelEdit, enabled = !uiState.busy) {
                    Text("Cancel editing")
                }
            }

            Text("Last 30 days", style = MaterialTheme.typography.titleMedium)

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                    Text("Loading your sleep history...")
                }

                uiState.loadError != null -> {
                    Text(uiState.loadError, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                        Text("Retry")
                    }
                }

                uiState.entries.isEmpty() -> {
                    Text("No sleep logged in the last 30 days.")
                }

                else -> {
                    uiState.entries.forEach { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(formatDuration(entry.hoursSlept), fontWeight = FontWeight.Bold)
                            Text(entry.date, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                TextButton(onClick = { onEdit(entry) }, enabled = !uiState.busy) {
                                    Text("Edit")
                                }
                                TextButton(
                                    onClick = { pendingDelete = entry },
                                    enabled = !uiState.busy
                                ) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete sleep entry?") },
            text = {
                Text("Remove ${formatDuration(entry.hoursSlept)} recorded on ${entry.date}? This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(entry)
                    pendingDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SleepField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    )
}

private fun formatDuration(hoursSlept: Double): String {
    val (h, m) = SleepValidator.splitHours(hoursSlept)
    return if (m == 0) "$h h" else "$h h $m m"
}

private val previewEntries = listOf(
    SleepEntry(id = "1", date = "2026-11-07", hoursSlept = 7.5),
    SleepEntry(id = "2", date = "2026-11-06", hoursSlept = 8.0)
)

@Preview(showBackground = true)
@Composable
private fun SleepScreenEmptyPreview() {
    HealthAndWellnessCoachTheme {
        SleepScreen()
    }
}