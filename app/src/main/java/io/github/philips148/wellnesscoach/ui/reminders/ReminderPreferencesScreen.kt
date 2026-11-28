package io.github.philips148.wellnesscoach.ui.reminders

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.philips148.wellnesscoach.data.reminders.*
import io.github.philips148.wellnesscoach.ui.theme.HealthAndWellnessCoachTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ReminderPreferencesScreen(
    state: ReminderPreferencesUiState,
    onChange: (ReminderPreferences) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().background(Color(0xFF323E71)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Health and Wellness Coach", color = Color.White, style = MaterialTheme.typography.labelLarge)
            Text("Reminders", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text("Choose what works for your day.", color = Color.White)
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Allow the user to leave even if Firebase is waiting for a connection.
            TextButton(onClick = onBack) {
                Text("Back to Profile")
            }
            if (state.loading) {
                CircularProgressIndicator()
                Text("Loading reminders…")
            }
            if (state.loaded) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFCBD8FF))) {
                    Text("Reminders use this phone's local time, including after travel or daylight saving changes.", Modifier.padding(16.dp))
                }
                val preferences = state.preferences
                ReminderRow("Hydration", preferences.hydration, !state.saving) {
                    onChange(preferences.copy(hydration = it))
                }
                ReminderRow("Daily check-in", preferences.checkIn, !state.saving) {
                    onChange(preferences.copy(checkIn = it))
                }
                ReminderRow("Workout", preferences.workout, !state.saving) {
                    onChange(preferences.copy(workout = it))
                }
                Button(onClick = onSave, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.saving) "Saving…" else "Save Reminders")
                }
                Text("Your enabled reminders use the times you choose.")
                // Let the user know that their changes are still waiting to sync.
                if (state.saving) {
                    Text("Saving your choices. You can leave this screen, but changes may remain pending until you reconnect.")
                }
                if (state.saved) Text("Your reminders are saved.", color = MaterialTheme.colorScheme.primary)
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                if (!state.loaded) OutlinedButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun ReminderRow(label: String, value: ReminderPreference, editable: Boolean, onChange: (ReminderPreference) -> Unit) {
    val context = LocalContext.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label, Modifier.weight(1f))
            Checkbox(checked = value.enabled, enabled = editable, onCheckedChange = { onChange(value.copy(enabled = it)) })
        }
        OutlinedButton(enabled = editable && value.enabled, modifier = Modifier.fillMaxWidth(), onClick = {
            TimePickerDialog(context, { _, hour, minute -> onChange(value.copy(hour = hour, minute = minute)) },
                value.hour, value.minute, android.text.format.DateFormat.is24HourFormat(context)).show()
        }) {
            Text("Reminder time: " + LocalTime.of(value.hour, value.minute)
                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderPreferencesPreview() {
    HealthAndWellnessCoachTheme {
        ReminderPreferencesScreen(ReminderPreferencesUiState(loading = false, loaded = true), {}, {}, {}, {})
    }
}
