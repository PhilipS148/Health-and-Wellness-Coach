package io.github.philips148.wellnesscoach.ui.hydration

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.philips148.wellnesscoach.data.model.WaterEntry
import io.github.philips148.wellnesscoach.ui.theme.HealthAndWellnessCoachTheme

@Composable
fun HydrationScreen(
    uiState: HydrationUiState,
    onDateInputChange: (String) -> Unit,
    onEntryDateChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSave: () -> Unit,
    onEdit: (WaterEntry) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: (WaterEntry) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onBack: () -> Unit
) {
    // Keep an in-flight save visible until Firebase confirms its result.
    BackHandler(enabled = uiState.isSaving) { }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding().verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Health and Wellness Coach",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary)
            Text("Hydration", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            Text("Keep your day flowing.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBack, enabled = !uiState.isSaving) {
                Text("Back to Dashboard")
            }
            WaterField("View date (yyyy-MM-dd)", uiState.dateInput,
                onDateInputChange, !uiState.busy)
            OutlinedButton(onClick = onRefresh, enabled = !uiState.busy,
                modifier = Modifier.fillMaxWidth()) {
                Text("Refresh / Retry")
            }

            uiState.successMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.onBackground)
            }
            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            if (uiState.isLoading) {
                CircularProgressIndicator()
                Text("Loading your water records and saved goal...")
            }
            if (uiState.isSaving) {
                CircularProgressIndicator()
                Text("Saving changes. If offline, reconnect to finish.")
            }

            if (uiState.dataAvailable) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Daily water total · ${uiState.selectedDate}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("${uiState.totalOz} / ${uiState.goalOz} US fl oz",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    LinearProgressIndicator(
                        progress = { uiState.progress }, modifier = Modifier.fillMaxWidth()
                    )
                    Text(if (uiState.totalOz >= (uiState.goalOz ?: 0)) "Daily goal reached"
                        else "${(uiState.goalOz ?: 0) - uiState.totalOz} US fl oz to your goal",
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(if (uiState.editing == null) "Add water" else "Edit water entry",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                WaterField("Entry date (yyyy-MM-dd)", uiState.entryDate,
                    onEntryDateChange, !uiState.busy)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(8, 16, 24).forEach { amount ->
                        OutlinedButton(onClick = { onAmountChange(amount.toString()) },
                            enabled = !uiState.busy, modifier = Modifier.weight(1f)) {
                            Text("$amount fl oz")
                        }
                    }
                }
                WaterField("Amount (US fl oz)", uiState.amount, onAmountChange,
                    !uiState.busy, KeyboardType.Number)
                Text("Enter a positive whole number. Each save records one water entry.",
                    style = MaterialTheme.typography.bodySmall)
                Button(onClick = onSave, enabled = !uiState.busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)) {
                    Text(when {
                        uiState.isSaving -> "Saving..."
                        uiState.editing != null -> "Save Changes"
                        else -> "Save Water"
                    })
                }
                if (uiState.editing != null) {
                    TextButton(onClick = onCancelEdit, enabled = !uiState.busy) {
                        Text("Cancel editing")
                    }
                }
                Text("Entries for ${uiState.selectedDate}",
                    style = MaterialTheme.typography.titleMedium)
                if (uiState.entries.isEmpty()) Text("No water recorded for this date.")
                uiState.entries.forEach { entry ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer,
                                RoundedCornerShape(16.dp)).padding(16.dp)
                    ) {
                        Text("${entry.amountOz} US fl oz", fontWeight = FontWeight.Bold)
                        Text(entry.date, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            TextButton(onClick = { onEdit(entry) }, enabled = !uiState.busy) {
                                Text("Edit")
                            }
                            TextButton(onClick = { onDelete(entry) }, enabled = !uiState.busy) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete water entry?") },
            text = { Text("Remove ${entry.amountOz} US fl oz recorded on ${entry.date}? This can't be undone.") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete, enabled = !uiState.busy) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = onDismissDelete) { Text("Cancel") } }
        )
    }
}

@Composable
private fun WaterField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value, onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(), enabled = enabled,
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1000)
@Composable
private fun HydrationScreenPreview() {
    HealthAndWellnessCoachTheme {
        HydrationScreen(
            uiState = HydrationUiState(
                selectedDate = "2026-10-27", dataAvailable = true, goalOz = 64,
                entries = listOf(WaterEntry(id = "sample", date = "2026-10-27", amountOz = 16))
            ),
            onDateInputChange = {}, onEntryDateChange = {}, onAmountChange = {},
            onRefresh = {}, onSave = {}, onEdit = {}, onCancelEdit = {},
            onDelete = {}, onDismissDelete = {}, onConfirmDelete = {}, onBack = {}
        )
    }
}
