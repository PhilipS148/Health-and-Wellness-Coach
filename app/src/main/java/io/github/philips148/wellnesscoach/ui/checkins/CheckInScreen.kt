package io.github.philips148.wellnesscoach.ui.checkins

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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.philips148.wellnesscoach.data.model.CheckInEntry
import io.github.philips148.wellnesscoach.data.model.TrackingCategories
import io.github.philips148.wellnesscoach.ui.theme.HealthAndWellnessCoachTheme

@Composable
fun CheckInScreen(
    uiState: CheckInUiState,
    onDateChange: (String) -> Unit,
    onStressChange: (String) -> Unit,
    onMoodChange: (String) -> Unit,
    onEnergyChange: (String) -> Unit,
    onHistoryStartChange: (String) -> Unit,
    onHistoryEndChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSave: () -> Unit,
    onEdit: (CheckInEntry) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: (CheckInEntry) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(uiState.editing?.id) {
        if (uiState.editing != null) scrollState.animateScrollTo(0)
    }
    BackHandler(enabled = uiState.isSaving) { }
    val enabled = !uiState.busy
    val canEdit = enabled && uiState.preferencesLoaded

    Column(modifier = Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .imePadding().verticalScroll(scrollState)) {
        Column(modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Health and Wellness Coach", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary)
            Text("Daily check-in", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            Text("A quick moment to reflect on your day.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary)
        }
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onBack, enabled = !uiState.isSaving) { Text("Back to Dashboard") }
            if (uiState.isLoading) {
                CircularProgressIndicator()
                Text("Loading check-ins and tracking preferences...")
            }
            uiState.preferenceError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onRefresh, enabled = enabled) { Text("Retry loading preferences") }
            }
            if (uiState.preferencesLoaded) {
                Text(if (uiState.editing == null) "How are you feeling?" else "Edit check-in",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                CheckInField("Check-in date (yyyy-MM-dd)", uiState.form.date, onDateChange, canEdit)
                Text("Use whole numbers from 1 to 10. Leave a rating blank if you don't want to record it.",
                    style = MaterialTheme.typography.bodySmall)
                if (uiState.selectedCategories.isEmpty()) {
                    Text("No check-in categories are selected. Enable Stress, Mood, or Energy in your tracking preferences to add a new check-in.")
                }
                RatingField("Stress", "1 = very low stress · 10 = very high stress",
                    uiState.form.stress, onStressChange, canEdit,
                    TrackingCategories.STRESS in uiState.selectedCategories,
                    uiState.editing?.stressLevel)
                RatingField("Mood", "1 = very low mood · 10 = very good mood",
                    uiState.form.mood, onMoodChange, canEdit,
                    TrackingCategories.MOOD in uiState.selectedCategories,
                    uiState.editing?.mood)
                RatingField("Energy", "1 = very low energy · 10 = very high energy",
                    uiState.form.energy, onEnergyChange, canEdit,
                    TrackingCategories.ENERGY in uiState.selectedCategories,
                    uiState.editing?.energyLevel)
                uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                uiState.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.onBackground) }
                if (uiState.isSaving) {
                    CircularProgressIndicator()
                    Text("Saving changes. If offline, reconnect to finish.")
                }
                Button(onClick = onSave,
                    enabled = canEdit && (uiState.selectedCategories.isNotEmpty() || uiState.editing != null),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)) {
                    Text(when {
                        uiState.isSaving -> "Saving..."
                        uiState.editing != null -> "Save Changes"
                        else -> "Save Check-in"
                    })
                }
                if (uiState.editing != null) {
                    TextButton(onClick = onCancelEdit, enabled = enabled) { Text("Cancel editing") }
                }
            }

            Text("Previous check-ins", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            CheckInField("From (yyyy-MM-dd)", uiState.historyStart, onHistoryStartChange, enabled)
            CheckInField("Through (yyyy-MM-dd)", uiState.historyEnd, onHistoryEndChange, enabled)
            OutlinedButton(onClick = onRefresh, enabled = enabled,
                modifier = Modifier.fillMaxWidth()) { Text("Refresh / Retry") }
            uiState.historyError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!uiState.preferencesLoaded) {
                // Deleting history remains available even when profile loading failed.
                uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                uiState.successMessage?.let { Text(it) }
                if (uiState.isSaving) { CircularProgressIndicator(); Text("Saving changes...") }
            }
            if (uiState.historyAvailable) {
                Text("${uiState.loadedStart} to ${uiState.loadedEnd}",
                    style = MaterialTheme.typography.bodySmall)
                if (uiState.entries.isEmpty()) Text("No check-ins recorded in this date range.")
                uiState.entries.forEach { entry ->
                    Column(modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(entry.date, fontWeight = FontWeight.Bold)
                        Text("Stress: " + (entry.stressLevel?.let { "$it / 10" } ?: "Not recorded"))
                        Text("Mood: " + (entry.mood?.let { "$it / 10" } ?: "Not recorded"))
                        Text("Energy: " + (entry.energyLevel?.let { "$it / 10" } ?: "Not recorded"))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            TextButton(onClick = { onEdit(entry) }, enabled = canEdit) { Text("Edit") }
                            TextButton(onClick = { onDelete(entry) }, enabled = enabled) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
    uiState.pendingDelete?.let { entry ->
        AlertDialog(onDismissRequest = onDismissDelete,
            title = { Text("Delete check-in?") },
            text = { Text("Delete the check-in for ${entry.date}? This can't be undone.") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete, enabled = enabled) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = onDismissDelete) { Text("Cancel") } })
    }
}

@Composable
private fun RatingField(
    title: String,
    scale: String,
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    selected: Boolean,
    previous: Int?
) {
    if (selected) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(scale, style = MaterialTheme.typography.bodySmall)
            CheckInField("$title (1–10)", value, onChange, enabled, KeyboardType.Number)
            TextButton(onClick = { onChange("") }, enabled = enabled && value.isNotEmpty()) {
                Text("Clear $title rating")
            }
        }
    } else if (previous != null) {
        Text("$title: $previous / 10 — category turned off; existing rating is kept.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CheckInField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
        enabled = enabled, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp), colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
}

@Preview(showBackground = true, widthDp = 400, heightDp = 1200)
@Composable
private fun CheckInScreenPreview() {
    HealthAndWellnessCoachTheme {
        CheckInScreen(uiState = CheckInUiState(preferencesLoaded = true,
            selectedCategories = setOf(TrackingCategories.STRESS, TrackingCategories.MOOD, TrackingCategories.ENERGY),
            historyAvailable = true, entries = listOf(CheckInEntry(date = "2026-11-04", stressLevel = 3, mood = 8))),
            onDateChange = {}, onStressChange = {}, onMoodChange = {}, onEnergyChange = {},
            onHistoryStartChange = {}, onHistoryEndChange = {}, onRefresh = {}, onSave = {},
            onEdit = {}, onCancelEdit = {}, onDelete = {}, onDismissDelete = {},
            onConfirmDelete = {}, onBack = {})
    }
}
