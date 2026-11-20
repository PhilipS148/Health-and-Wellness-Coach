package io.github.philips148.wellnesscoach.ui.progress

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.philips148.wellnesscoach.data.model.TrackingCategories
import io.github.philips148.wellnesscoach.data.summary.DailySummary
import io.github.philips148.wellnesscoach.data.summary.WeeklySummary
import io.github.philips148.wellnesscoach.ui.theme.HealthAndWellnessCoachTheme
import io.github.philips148.wellnesscoach.util.ProgressPeriods
import io.github.philips148.wellnesscoach.util.ProgressPeriods.ratio
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProgressScreen(
    uiState: ProgressUiState = ProgressUiState(isLoading = false),
    onModeChange: (Boolean) -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                "Progress", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "Review your personal data.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!uiState.isWeekly) {
                    Button(
                        onClick = { onModeChange(false) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Day") }

                } else {
                    OutlinedButton(
                        onClick = { onModeChange(false) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Day") }
                }
                if (uiState.isWeekly) {
                    Button(onClick = { onModeChange(true) }, modifier = Modifier.weight(1f)) {
                        Text(
                            "Week"
                        )
                    }

                } else {
                    OutlinedButton(
                        onClick = { onModeChange(true) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Week") }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous period"
                    )
                }

                Text(
                    ProgressPeriods.periodLabel(
                        uiState.selectedDate,
                        uiState.isWeekly,
                        LocalDate.now()
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                IconButton(onClick = onNext, enabled = uiState.canGoNext) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next period"
                    )
                }
            }

            OutlinedButton(
                onClick = onRefresh,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh")
            }

            var periodType = ""

            if (uiState.isWeekly) {
                periodType = "week"
            } else {
                periodType = "day"
            }

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                    Text("Loading your progress...")
                }
                uiState.errorMessage != null -> {
                    Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRefresh) { Text("Retry") }
                }

                uiState.isEmpty -> {
                        Text("No activity recorded for this $periodType.")
                        Text(
                            "Add entries from the Logs tab to see your progress here.",
                            style = MaterialTheme.typography.bodySmall
                        )
                }

                uiState.isWeekly -> {
                   uiState.weeklySummary?.let{WeekView(it)}
                }

                else -> {
                    uiState.dailySummary?.let {DayView(it)}

                }
            }
        }
    }
}

@Composable
private fun GoalCard(
    title: String,
    valueText: String,
    goalText: String,
    progress: Double?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("$valueText / $goalText")

        if (progress != null) {
            LinearProgressIndicator(
                progress = {progress.coerceIn(0.0, 1.0).toFloat()},
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                if (progress >= 1.0) "Goal reached · ${ProgressPeriods.percent(progress)}"
                else ProgressPeriods.percent(progress),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text("No data recorded", style = MaterialTheme.typography.bodySmall)
        }
    }

}

@Composable
private fun DayView(summary: DailySummary) {
    Column(
        verticalArrangement = Arrangement.spacedBy((12.dp))
    ) {
        val categories = summary.selectedCategories

        if (TrackingCategories.MEALS in categories) {
            GoalCard(
                title = "Calories",
                valueText = "${summary.calories}",
                goalText = "${summary.calorieGoal} kcal",
                progress = summary.calorieProgress
            )
        }

        if (TrackingCategories.HYDRATION in categories) {
            GoalCard(
                title = "Water",
                valueText = "${summary.waterOz}",
                goalText = "${summary.waterGoalOz} fl oz",
                progress = summary.waterProgress
            )
        }

        if (TrackingCategories.STEPS in categories) {
            GoalCard(
                title = "Steps",
                valueText = summary.steps?.toString() ?: "No data",
                goalText = "${summary.stepGoal} steps",
                progress = summary.stepProgress
            )
        }

        if (TrackingCategories.SLEEP in categories) {
            GoalCard(
                title = "Sleep",
                valueText = ProgressPeriods.average(summary.sleepHours),
                goalText = "${summary.sleepGoalHours} h",
                progress = summary.sleepProgress
            )
        }

        if (TrackingCategories.WORKOUTS in categories) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Workouts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${summary.workoutCount} workouts · ${summary.workoutMinutes} min")
            }

        }
        val showCheckIn = TrackingCategories.STRESS in categories ||
                TrackingCategories.MOOD in categories ||
                TrackingCategories.ENERGY in categories

        if(showCheckIn) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Check-in", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                if (TrackingCategories.STRESS in categories) {
                    Text("Stress: ${summary.stressLevel ?: "No rating"}")
                }

                if (TrackingCategories.MOOD in categories) {
                    Text("Mood: ${summary.mood ?: "No rating"}")
                }

                if (TrackingCategories.ENERGY in categories) {
                    Text("Energy: ${summary.energyLevel ?: "No rating"}")
                }
            }
        }
    }
}

@Composable
private fun WeeklyBarChart(
    title: String,
    progressByDay: List<Double?>
) {
    val barColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.error
    val maxValue = 1.5

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val slotWidth = size.width / progressByDay.size
            val barWidth = slotWidth * 0.6f

            progressByDay.forEachIndexed { index, progress ->
                if (progress != null) {
                    val fraction = (progress.coerceIn(0.0, maxValue) / maxValue).toFloat()
                    val barHeight = size.height * fraction
                    drawRect(
                        color = barColor,
                        topLeft = Offset(
                            x = index * slotWidth + (slotWidth - barWidth) / 2,
                            y = size.height - barHeight
                        ),
                        size = Size(barWidth, barHeight)
                    )
                }
            }

            val goalY = size.height * (1f - (1.0 / maxValue).toFloat())
            drawLine(
                color = goalColor,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 2.dp.toPx()
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun WeekView(summary: WeeklySummary) {
    Column(
        verticalArrangement = Arrangement.spacedBy((12.dp))
    ) {
        val firstDay = summary.days.firstOrNull() ?: return
        val categories = firstDay.selectedCategories

        Text(
            "Weekly Averages",
            style = MaterialTheme.typography.titleMedium
        )
        if (TrackingCategories.MEALS in categories) {
            GoalCard(
                title = "Average Calories Consumed",
                valueText = ProgressPeriods.average(summary.averageDailyCalories),
                goalText = "${firstDay.calorieGoal} kcal",
                progress = ProgressPeriods.ratio(summary.averageDailyCalories, firstDay.calorieGoal.toDouble())
            )
        }

        if (TrackingCategories.HYDRATION in categories) {
            GoalCard(
                title = "Average Water Intake",
                valueText = ProgressPeriods.average(summary.averageDailyWaterOz),
                goalText = "${firstDay.waterGoalOz} fl oz",
                progress = ProgressPeriods.ratio(summary.averageDailyWaterOz, firstDay.waterGoalOz.toDouble())
            )
        }

        if (TrackingCategories.STEPS in categories) {
            GoalCard(
                title = "Average Steps",
                valueText = ProgressPeriods.average(summary.averageDailySteps),
                goalText = "${firstDay.stepGoal} steps",
                progress = ProgressPeriods.ratio(summary.averageDailySteps, firstDay.stepGoal.toDouble())
            )
        }

        if (TrackingCategories.SLEEP in categories) {
            GoalCard(
                title = "Average Sleep",
                valueText = ProgressPeriods.average(summary.averageSleepHours),
                goalText = "${firstDay.sleepGoalHours} h",
                progress = ProgressPeriods.ratio(summary.averageSleepHours, firstDay.sleepGoalHours))
        }

        if (TrackingCategories.WORKOUTS in categories) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Workouts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${summary.totalWorkouts} workouts · ${summary.totalWorkoutMinutes} min")
            }

        }
        val showCheckIn = TrackingCategories.STRESS in categories ||
                TrackingCategories.MOOD in categories ||
                TrackingCategories.ENERGY in categories

        if(showCheckIn) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Check-in averages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (TrackingCategories.STRESS in categories) {
                    Text("Stress: ${ProgressPeriods.average(summary.averageStressLevel)}")
                }

                if (TrackingCategories.MOOD in categories) {
                    Text("Mood: ${ProgressPeriods.average(summary.averageMood)}")
                }
                if (TrackingCategories.ENERGY in categories) {
                    Text("Energy: ${ProgressPeriods.average(summary.averageEnergyLevel)}")
                }

            }
        }
        Text("Daily progress", style = MaterialTheme.typography.titleMedium)

        if (TrackingCategories.MEALS in categories) {
            WeeklyBarChart("Calories", summary.days.map { it.calorieProgress })
        }
        if (TrackingCategories.HYDRATION in categories) {
            WeeklyBarChart("Water", summary.days.map { it.waterProgress })
        }
        if (TrackingCategories.STEPS in categories) {
            WeeklyBarChart("Steps", summary.days.map { it.stepProgress })
        }
        if (TrackingCategories.SLEEP in categories) {
            WeeklyBarChart("Sleep", summary.days.map { it.sleepProgress })
        }
        Text("Day by day", style = MaterialTheme.typography.titleMedium)

        summary.days.forEach { day ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    LocalDate.parse(day.date)
                        .format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)),
                    fontWeight = FontWeight.Bold
                )

                if (!ProgressPeriods.hasData(day)) {
                    Text("Nothing recorded", style = MaterialTheme.typography.bodySmall)
                } else {
                    val parts = mutableListOf<String>()
                    if (TrackingCategories.MEALS in categories) {
                        parts.add("${day.calories} kcal")
                    }

                    if (TrackingCategories.HYDRATION in categories) {
                        parts.add("${day.waterOz} fl oz")
                    }

                    if (TrackingCategories.STEPS in categories) {
                        parts.add("${day.steps ?: "—"} steps")
                    }

                    if (TrackingCategories.SLEEP in categories) {
                        parts.add(day.sleepHours?.let { "${ProgressPeriods.average(it)} h sleep" } ?: "— sleep")
                    }

                    if (TrackingCategories.WORKOUTS in categories) {
                        parts.add("${day.workoutCount} workouts")
                    }

                    Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}


@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun ProgressWeekPreview() {
    val days = (0..6).map { i ->
        DailySummary(
            date = LocalDate.of(2026, 11, 16).plusDays(i.toLong()).toString(),
            selectedCategories = TrackingCategories.ALL,
            calories = 1800 + i * 50, calorieGoal = 2000, calorieProgress = (1800 + i * 50) / 2000.0,
            waterOz = 40 + i * 6, waterGoalOz = 64, waterProgress = (40 + i * 6) / 64.0,
            steps = if (i % 2 == 0) 4000 + i * 500 else null, stepGoal = 5000,
            stepProgress = if (i % 2 == 0) (4000 + i * 500) / 5000.0 else null,
            sleepHours = if (i == 3) null else 7.0, sleepGoalHours = 8.0,
            sleepProgress = if (i == 3) null else 0.875,
            mood = if (i < 4) 7 else null
        )
    }
    HealthAndWellnessCoachTheme {
        ProgressScreen(
            uiState = ProgressUiState(
                isLoading = false,
                isWeekly = true,
                selectedDate = LocalDate.of(2026, 11, 16),
                weeklySummary = WeeklySummary(
                    startDate = "2026-11-16", endDate = "2026-11-22", days = days,
                    averageDailyCalories = 1950.0, averageDailyWaterOz = 58.0,
                    averageDailySteps = 5500.0, averageSleepHours = 7.0,
                    totalWorkouts = 3, totalWorkoutMinutes = 90, averageMood = 7.0
                )
            )
        )
    }
}