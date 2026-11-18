package io.github.philips148.wellnesscoach.ui.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LogsScreen(
    onMealsClick: () -> Unit,
    onHydrationClick: () -> Unit,
    onWorkoutsClick: () -> Unit,
    onSleepClick: () -> Unit,
    onCheckInClick: () -> Unit,
    onStepsClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Logs",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            Text(
                text = "Track and review your wellness activity.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        item {
            LogButton(
                text = "Meals",
                onClick = onMealsClick
            )
        }

        item {
            LogButton(
                text = "Hydration",
                onClick = onHydrationClick
            )
        }

        item {
            LogButton(
                text = "Workouts",
                onClick = onWorkoutsClick
            )
        }

        item {
            LogButton(
                text = "Sleep",
                onClick = onSleepClick
            )
        }

        item {
            LogButton(
                text = "Daily Check-in",
                onClick = onCheckInClick
            )
        }

        item {
            LogButton(
                text = "Phone-based Steps",
                onClick = onStepsClick
            )
        }
    }
}

@Composable
private fun LogButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}