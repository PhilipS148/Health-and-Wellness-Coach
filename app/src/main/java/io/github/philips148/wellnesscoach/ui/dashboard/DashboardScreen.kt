package io.github.philips148.wellnesscoach.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.background
import androidx.compose.material3.ButtonDefaults
import io.github.philips148.wellnesscoach.data.summary.DailySummary
import io.github.philips148.wellnesscoach.data.summary.WeeklySummary

import io.github.philips148.wellnesscoach.data.model.TrackingCategories
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onMealsClick: () -> Unit,
    onHydrationClick: () -> Unit,
    onWorkoutsClick: () -> Unit,
    onSleepClick: () -> Unit,
    onCheckInClick: () -> Unit,
    onStepsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh when returning from a tracker.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefresh()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            DashboardHeader(
                displayName = uiState.displayName,
                onProfileClick = onProfileClick
            )
        }

        if (uiState.isLoading) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    CircularProgressIndicator()

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text("Loading dashboard...")
                }
            }
        }

        uiState.errorMessage?.let { message ->
            item {
                ErrorCard(
                    message = message,
                    onRetry = onRefresh
                )
            }
        }

        uiState.dailySummary?.let { summary ->
            item {
                GoalSection(
                    summary = summary,
                    onMealsClick = onMealsClick,
                    onHydrationClick = onHydrationClick,
                    onStepsClick = onStepsClick
                )
            }

            item {
                DailyProgressSection(
                    summary = summary
                )
            }

            item {
                ProgressMessage(
                    message = uiState.progressMessage
                )
            }

            item {
                EmptyStateIfNeeded(
                    summary = summary
                )
            }
        }

        uiState.weeklySummary?.let { summary ->
            item {
                WeeklyProgressSection(
                    summary = summary
                )
            }
        }

        item {
            ShortcutSection(
                onMealsClick = onMealsClick,
                onHydrationClick = onHydrationClick,
                onWorkoutsClick = onWorkoutsClick,
                onSleepClick = onSleepClick,
                onCheckInClick = onCheckInClick,
                onStepsClick = onStepsClick
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    displayName: String,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(
                horizontal = 24.dp,
                vertical = 28.dp
            )
    ) {
        Text(
            text = "Health and Wellness Coach",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = if (displayName.isBlank()) {
                "Welcome back"
            } else {
                "Good morning,\n$displayName"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = "Keep up the great work!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedButton(
            onClick = onProfileClick,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Profile")
        }
    }
}


@Composable
private fun GoalSection(
    summary: DailySummary,
    onMealsClick: () -> Unit,
    onHydrationClick: () -> Unit,
    onStepsClick: () -> Unit
) {
    val showSteps =
        TrackingCategories.STEPS in summary.selectedCategories

    val showHydration =
        TrackingCategories.HYDRATION in summary.selectedCategories

    val showCalories =
        TrackingCategories.MEALS in summary.selectedCategories

    if (!showSteps && !showHydration && !showCalories) {
        return
    }

    Column(
        modifier = Modifier.padding(
            horizontal = 16.dp,
            vertical = 8.dp
        )
    ) {
        Text(
            text = "Goals",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            // Steps card
            if (showSteps) {
                GoalCard(
                    title = "Steps",
                    value = summary.steps?.toString()
                        ?: "Unavailable",
                    goal = "${summary.stepGoal}",
                    modifier = Modifier.weight(1f),
                    onClick = onStepsClick
                )
            }

            // Hydration card
            if (showHydration) {
                GoalCard(
                    title = "Hydration",
                    value = "${summary.waterOz} oz",
                    goal = "${summary.waterGoalOz} oz",
                    modifier = Modifier.weight(1f),
                    onClick = onHydrationClick
                )
            }

            // Calories card
            if (showCalories) {
                GoalCard(
                    title = "Calories",
                    value = "${summary.calories}",
                    goal = "${summary.calorieGoal}",
                    modifier = Modifier.weight(1f),
                    onClick = onMealsClick
                )
            }
        }
    }
}


@Composable
private fun GoalCard(
    title: String,
    value: String,
    goal: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "/ $goal",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


@Composable
private fun DailyProgressSection(
    summary: DailySummary
) {
    val showHydration =
        TrackingCategories.HYDRATION in summary.selectedCategories

    val showSteps =
        TrackingCategories.STEPS in summary.selectedCategories

    val showSleep =
        TrackingCategories.SLEEP in summary.selectedCategories

    if (!showHydration && !showSteps && !showSleep) {
        return
    }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Daily goals",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        if (showHydration) {
            ProgressRow(
                label = "Hydration",
                value = "${summary.waterOz} / ${summary.waterGoalOz} oz",
                progress = summary.waterProgress.toFloat()
            )
        }

        if (showSteps) {
            if (summary.steps == null) {
                Text(
                    text = "Steps",
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = "No step total recorded today.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                ProgressRow(
                    label = "Steps",
                    value = "${summary.steps} / ${summary.stepGoal}",
                    progress = summary.stepProgress
                        ?.toFloat() ?: 0f
                )
            }
        }

        if (showSleep) {
            if (summary.sleepHours == null) {
                Text(
                    text = "Sleep",
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = "No sleep entry recorded today.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                ProgressRow(
                    label = "Sleep",
                    value = "${summary.sleepHours} / ${summary.sleepGoalHours} hrs",
                    progress = summary.sleepProgress
                        ?.toFloat() ?: 0f
                )
            }
        }
    }
}


@Composable
private fun ProgressRow(
    label: String,
    value: String,
    progress: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(label)

            Text(value)
        }

        LinearProgressIndicator(
            progress = {
                progress.coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ProgressMessage(
    message: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Your Progress",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(message)

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Based on your recorded information.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


@Composable
private fun WeeklyProgressSection(
    summary: WeeklySummary
) {
    val goalSource = summary.days.firstOrNull()
    val categories = goalSource?.selectedCategories.orEmpty()

    val showCalories =
        TrackingCategories.MEALS in categories

    val showHydration =
        TrackingCategories.HYDRATION in categories

    val showSteps =
        TrackingCategories.STEPS in categories

    val showSleep =
        TrackingCategories.SLEEP in categories

    if (!showCalories && !showHydration && !showSteps && !showSleep) {
        return
    }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Weekly progress",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {
                if (showCalories) {
                    WeeklyProgressRow(
                        label = "Calories",
                        value = "%.0f avg".format(
                            summary.averageDailyCalories
                        ),
                        progress = calculateWeeklyProgress(
                            summary.averageDailyCalories,
                            goalSource?.calorieGoal?.toDouble()
                        )
                    )
                }

                if (showHydration) {
                    WeeklyProgressRow(
                        label = "Hydration",
                        value = "%.1f oz avg".format(
                            summary.averageDailyWaterOz
                        ),
                        progress = calculateWeeklyProgress(
                            summary.averageDailyWaterOz,
                            goalSource?.waterGoalOz?.toDouble()
                        )
                    )
                }

                if (showSteps) {
                    WeeklyProgressRow(
                        label = "Steps",
                        value = summary.averageDailySteps?.let {
                            "%.0f avg".format(it)
                        } ?: "No recorded data",
                        progress = calculateWeeklyProgress(
                            summary.averageDailySteps,
                            goalSource?.stepGoal?.toDouble()
                        )
                    )
                }

                if (showSleep) {
                    WeeklyProgressRow(
                        label = "Sleep",
                        value = summary.averageSleepHours?.let {
                            "%.1f hrs avg".format(it)
                        } ?: "No recorded data",
                        progress = calculateWeeklyProgress(
                            summary.averageSleepHours,
                            goalSource?.sleepGoalHours
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyProgressRow(
    label: String,
    value: String,
    progress: Float?
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        if (progress != null) {
            LinearProgressIndicator(
                progress = {
                    progress.coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = "Not enough data",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun calculateWeeklyProgress(
    value: Double?,
    goal: Double?
): Float? {
    if (
        value == null ||
        goal == null ||
        goal <= 0.0
    ) {
        return null
    }

    return (value / goal)
        .toFloat()
        .coerceIn(0f, 1f)
}


@Composable
private fun EmptyStateIfNeeded(
    summary: DailySummary
) {
    val categories = summary.selectedCategories

    val hasData =
        (TrackingCategories.MEALS in categories &&
                summary.calories > 0) ||
                (TrackingCategories.HYDRATION in categories &&
                        summary.waterOz > 0) ||
                (TrackingCategories.STEPS in categories &&
                        summary.steps != null) ||
                (TrackingCategories.SLEEP in categories &&
                        summary.sleepHours != null) ||
                (TrackingCategories.WORKOUTS in categories &&
                        summary.workoutCount > 0) ||
                (TrackingCategories.STRESS in categories &&
                        summary.stressLevel != null) ||
                (TrackingCategories.MOOD in categories &&
                        summary.mood != null) ||
                (TrackingCategories.ENERGY in categories &&
                        summary.energyLevel != null)

    if (!hasData) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (categories.isEmpty()) {
                    "No tracking categories selected. Choose categories in your goal settings to see your progress."
                } else {
                    "No wellness records have been saved for your selected categories today yet."
                },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}


@Composable
private fun ShortcutSection(
    onMealsClick: () -> Unit,
    onHydrationClick: () -> Unit,
    onWorkoutsClick: () -> Unit,
    onSleepClick: () -> Unit,
    onCheckInClick: () -> Unit,
    onStepsClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onMealsClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Meals")
            }

            Button(
                onClick = onHydrationClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Hydration")
            }
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onWorkoutsClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Workouts")
            }

            Button(
                onClick = onSleepClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Sleep")
            }
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCheckInClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Check-in")
            }

            Button(
                onClick = onStepsClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Steps")
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Couldn't load your dashboard.",
                style = MaterialTheme.typography.titleMedium
            )

            Text(message)

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = onRetry
            ) {
                Text("Retry")
            }
        }
    }
}