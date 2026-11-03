package io.github.philips148.wellnesscoach.ui.meals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.philips148.wellnesscoach.data.model.MealEntry

@Composable
fun MealTrackingScreen(
    uiState: MealTrackingUiState,
    onDateChange: (String) -> Unit,
    onMealNameChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onLoadDateClick: () -> Unit,
    onSaveClick: () -> Unit,
    onEditClick: (MealEntry) -> Unit,
    onDeleteClick: (String) -> Unit,
    onCancelEditClick: () -> Unit,
    onBackClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Meal & Calorie Tracking",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            TextButton(onClick = onBackClick) {
                Text("Back")
            }
        }

        item {
            OutlinedTextField(
                value = uiState.selectedDate,
                onValueChange = onDateChange,
                label = {
                    Text("Date (yyyy-MM-dd)")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = onLoadDateClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Load Date")
            }
        }

        item {
            Text(
                text = "Daily Calories: ${uiState.dailyCalories}",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            OutlinedTextField(
                value = uiState.mealName,
                onValueChange = onMealNameChange,
                label = {
                    Text("Meal name or description")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = uiState.calories,
                onValueChange = onCaloriesChange,
                label = {
                    Text("Estimated calories")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        uiState.errorMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            Button(
                onClick = onSaveClick,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (uiState.editingMealId == null) {
                        "Add Meal"
                    } else {
                        "Save Changes"
                    }
                )
            }
        }

        if (uiState.editingMealId != null) {
            item {
                TextButton(
                    onClick = onCancelEditClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Edit")
                }
            }
        }

        if (uiState.isLoading) {
            item {
                CircularProgressIndicator()
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Meals",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(
            items = uiState.meals,
            key = { it.id }
        ) { meal ->

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = meal.mealName,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("${meal.calories} calories")

                    Text(meal.date)

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                onEditClick(meal)
                            }
                        ) {
                            Text("Edit")
                        }

                        TextButton(
                            onClick = {
                                onDeleteClick(meal.id)
                            }
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}