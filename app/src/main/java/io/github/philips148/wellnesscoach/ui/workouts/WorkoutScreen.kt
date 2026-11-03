package io.github.philips148.wellnesscoach.ui.workouts

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
import io.github.philips148.wellnesscoach.data.model.WorkoutEntry
import io.github.philips148.wellnesscoach.data.model.WorkoutSet
import io.github.philips148.wellnesscoach.ui.theme.HealthAndWellnessCoachTheme

@Composable
fun WorkoutScreen(
    uiState: WorkoutUiState,
    onDateChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onDistanceChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRepsChange: (Int, String) -> Unit,
    onWeightChange: (Int, String) -> Unit,
    onHistoryStartChange: (String) -> Unit,
    onHistoryEndChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSave: () -> Unit,
    onEdit: (WorkoutEntry) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: (WorkoutEntry) -> Unit,
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
    val form = uiState.form

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding().verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Health and Wellness Coach",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary)
            Text("Workouts", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            Text("Record your movement and review your progress.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBack, enabled = !uiState.isSaving) {
                Text("Back to Dashboard")
            }
            Text(if (uiState.editing == null) "Record a workout" else "Edit workout",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("strength" to "Strength", "walk" to "Walk", "run" to "Run").forEach { (type, label) ->
                    if (form.type == type) {
                        Button(onClick = { onTypeChange(type) }, enabled = enabled,
                            modifier = Modifier.weight(1f)) { Text(label) }
                    } else {
                        OutlinedButton(onClick = { onTypeChange(type) }, enabled = enabled,
                            modifier = Modifier.weight(1f)) { Text(label) }
                    }
                }
            }
            WorkoutField("Workout date (yyyy-MM-dd)", form.date, onDateChange, enabled)
            WorkoutField(if (form.type == "strength") "Exercise name" else "Activity name (optional)",
                form.name, onNameChange, enabled)

            if (form.type == "strength") {
                Text("Sets · ${form.sets.size}", fontWeight = FontWeight.Bold)
                Text("Enter reps for each set. Weight is optional and stored in pounds.",
                    style = MaterialTheme.typography.bodySmall)
                form.sets.forEachIndexed { index, set ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer,
                                RoundedCornerShape(16.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Set ${index + 1}", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WorkoutField("Reps", set.reps, { onRepsChange(index, it) },
                                enabled, KeyboardType.Number, Modifier.weight(1f))
                            WorkoutField("Weight (lb)", set.weightLbs, { onWeightChange(index, it) },
                                enabled, KeyboardType.Decimal, Modifier.weight(1f))
                        }
                        TextButton(onClick = { onRemoveSet(index) }, enabled = enabled) {
                            Text("Remove set ${index + 1}")
                        }
                    }
                }
                OutlinedButton(onClick = onAddSet, enabled = enabled,
                    modifier = Modifier.fillMaxWidth()) { Text("Add Set") }
            } else {
                WorkoutField("Distance (miles)", form.distanceMiles, onDistanceChange,
                    enabled, KeyboardType.Decimal)
                WorkoutField("Duration (whole minutes)", form.durationMinutes, onDurationChange,
                    enabled, KeyboardType.Number)
                Text("Enter distance and duration manually.", style = MaterialTheme.typography.bodySmall)
            }

            uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            uiState.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.onBackground) }
            if (uiState.isSaving) {
                CircularProgressIndicator()
                Text("Saving changes. If offline, reconnect to finish.")
            }
            Button(onClick = onSave, enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)) {
                Text(when {
                    uiState.isSaving -> "Saving..."
                    uiState.editing != null -> "Save Changes"
                    else -> "Save Workout"
                })
            }
            if (uiState.editing != null) {
                TextButton(onClick = onCancelEdit, enabled = enabled) { Text("Cancel editing") }
            }

            Text("Workout history", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            WorkoutField("From (yyyy-MM-dd)", uiState.historyStart, onHistoryStartChange, enabled)
            WorkoutField("Through (yyyy-MM-dd)", uiState.historyEnd, onHistoryEndChange, enabled)
            OutlinedButton(onClick = onRefresh, enabled = enabled,
                modifier = Modifier.fillMaxWidth()) { Text("Refresh / Retry") }
            if (uiState.isLoading) {
                CircularProgressIndicator()
                Text("Loading workout history...")
            }
            if (uiState.historyAvailable) {
                Text("${uiState.loadedStart} to ${uiState.loadedEnd} · ${uiState.entries.size} workouts",
                    style = MaterialTheme.typography.bodySmall)
                if (uiState.entries.isEmpty()) Text("No workouts recorded in this date range.")
                uiState.entries.forEach { entry ->
                    WorkoutHistoryCard(entry, enabled, { onEdit(entry) }, { onDelete(entry) })
                }
            }
        }
    }
    uiState.pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete workout?") },
            text = { Text("Delete ${workoutTitle(entry)} from ${entry.date}? This can't be undone.") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete, enabled = enabled) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = onDismissDelete) { Text("Cancel") } }
        )
    }
}

private fun workoutTitle(entry: WorkoutEntry): String = entry.exerciseName.ifBlank {
    if (entry.workoutType == "run") "Run" else if (entry.workoutType == "walk") "Walk" else "Strength"
}

@Composable
private fun WorkoutHistoryCard(
    entry: WorkoutEntry,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(workoutTitle(entry), fontWeight = FontWeight.Bold)
        Text("${entry.date} · ${entry.workoutType}", style = MaterialTheme.typography.bodySmall)
        if (entry.workoutType == "strength") {
            entry.sets.forEachIndexed { index, set ->
                Text("Set ${index + 1}: ${set.repetitions} reps · " +
                    (set.weightLbs?.let { "$it lb" } ?: "weight not recorded"))
            }
        } else {
            Text("${entry.distanceMiles} miles · ${entry.durationMinutes} minutes")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onEdit, enabled = enabled) { Text("Edit") }
            TextButton(onClick = onDelete, enabled = enabled) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun WorkoutField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    TextField(value = value, onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(), enabled = enabled,
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
        ))
}

@Preview(showBackground = true, widthDp = 400, heightDp = 1200)
@Composable
private fun WorkoutScreenPreview() {
    HealthAndWellnessCoachTheme {
        WorkoutScreen(
            uiState = WorkoutUiState(historyAvailable = true,
                form = WorkoutForm(name = "Bench press", sets = listOf(
                    WorkoutSetInput("12", "95"), WorkoutSetInput("10", "115"))),
                entries = listOf(WorkoutEntry(exerciseName = "Bench press", workoutType = "strength",
                    date = "2026-11-02", sets = listOf(WorkoutSet(12, 95.0), WorkoutSet(10, 115.0))))),
            onDateChange = {}, onTypeChange = {}, onNameChange = {}, onDistanceChange = {},
            onDurationChange = {}, onAddSet = {}, onRemoveSet = {}, onRepsChange = { _, _ -> },
            onWeightChange = { _, _ -> }, onHistoryStartChange = {}, onHistoryEndChange = {},
            onRefresh = {}, onSave = {}, onEdit = {}, onCancelEdit = {}, onDelete = {},
            onDismissDelete = {}, onConfirmDelete = {}, onBack = {}
        )
    }
}
