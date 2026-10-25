package io.github.philips148.wellnesscoach.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.philips148.wellnesscoach.ui.theme.HealthAndWellnessCoachTheme

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    uiState: ProfileUiState = ProfileUiState(),
    onUsernameChange: (String) -> Unit = {},
    onHeightChange: (String) -> Unit = {},
    onWeightChange: (String) -> Unit = {},
    onSaveClick: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val showSetup = uiState.isSetupFlow || uiState.isNewProfile

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
                text = "Health and Wellness Coach",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Text(
                text = if (showSetup) {
                    "Complete your profile"
                } else {
                    "Your profile"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Text(
                text = if (showSetup) {
                    "Make this space your own."
                } else {
                    "Manage your details and preferences."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        if (!uiState.isSetupFlow) {
            TextButton(
                onClick = onBackClick,
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 8.dp
                ),
                enabled = !uiState.isSaving
            ) {
                Text("Back to Account Settings")
            }
        }

        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()

                    Text(
                        text = "Loading your profile...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            uiState.loadFailed -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = uiState.errorMessage
                            ?: "Couldn't load your profile. Please try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    Button(
                        onClick = onRetryClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextField(
                        value = uiState.username,
                        onValueChange = onUsernameChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving,
                        label = { Text("Username") },
                        placeholder = { Text("Enter a username") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor =
                                MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor =
                                MaterialTheme.colorScheme.surfaceContainer,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    TextField(
                        value = uiState.heightCm,
                        onValueChange = onHeightChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving,
                        label = { Text("Height (cm) — Optional") },
                        placeholder = { Text("Enter height") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor =
                                MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor =
                                MaterialTheme.colorScheme.surfaceContainer,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    TextField(
                        value = uiState.weightKg,
                        onValueChange = onWeightChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving,
                        label = { Text("Weight (kg) — Optional") },
                        placeholder = { Text("Enter weight") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor =
                                MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor =
                                MaterialTheme.colorScheme.surfaceContainer,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Your choice",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = "You can leave height and weight blank " +
                                    "and update your profile later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    uiState.errorMessage?.let { message ->
                        Text(
                            text = message,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    uiState.successMessage?.let { message ->
                        Text(
                            text = message,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Button(
                        onClick = onSaveClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !uiState.isSaving,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = when {
                                uiState.isSaving -> "Saving..."
                                showSetup -> "Continue"
                                else -> "Save Changes"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    name = "New profile",
    showBackground = true,
    widthDp = 360,
    heightDp = 720
)
@Composable
private fun NewProfileScreenPreview() {
    HealthAndWellnessCoachTheme {
        ProfileScreen()
    }
}

@Preview(
    name = "Setup existing registration profile",
    showBackground = true,
    widthDp = 360,
    heightDp = 720
)
@Composable
private fun SetupProfileScreenPreview() {
    HealthAndWellnessCoachTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                username = "Adam",
                isNewProfile = false,
                isSetupFlow = true
            )
        )
    }
}

@Preview(
    name = "Edit profile",
    showBackground = true,
    widthDp = 360,
    heightDp = 720
)
@Composable
private fun EditProfileScreenPreview() {
    HealthAndWellnessCoachTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                username = "Adam",
                heightCm = "175",
                weightKg = "75",
                isNewProfile = false
            )
        )
    }
}

@Preview(
    name = "Loading profile",
    showBackground = true,
    widthDp = 360,
    heightDp = 720
)
@Composable
private fun LoadingProfileScreenPreview() {
    HealthAndWellnessCoachTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = true
            )
        )
    }
}

@Preview(
    name = "Profile load failed",
    showBackground = true,
    widthDp = 360,
    heightDp = 720
)
@Composable
private fun ProfileLoadFailedScreenPreview() {
    HealthAndWellnessCoachTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                loadFailed = true,
                errorMessage =
                    "Couldn't load your profile. Please try again."
            )
        )
    }
}

@Preview(
    name = "Profile error",
    showBackground = true,
    widthDp = 360,
    heightDp = 720
)
@Composable
private fun ProfileErrorScreenPreview() {
    HealthAndWellnessCoachTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                username = "Adam",
                heightCm = "-5",
                errorMessage = "Height must be a positive number."
            )
        )
    }
}

@Preview(
    name = "Profile saved",
    showBackground = true,
    widthDp = 360,
    heightDp = 720
)
@Composable
private fun ProfileSavedScreenPreview() {
    HealthAndWellnessCoachTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                username = "Adam",
                heightCm = "175",
                weightKg = "75",
                isNewProfile = false,
                successMessage = "Your changes were saved."
            )
        )
    }
}