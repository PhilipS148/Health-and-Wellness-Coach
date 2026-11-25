package io.github.philips148.wellnesscoach.ui.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Displays the AI Coach conversation and lets the user ask questions.
// The ViewModel handles the actual requests and stores the chat state.
@Composable
fun CoachScreen(
    uiState: CoachUiState,
    onQuestionChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Remember where the user is scrolling in the conversation.
    val listState = rememberLazyListState()

    // Automatically show the newest message when the conversation changes.
    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        val lastItem = if (uiState.messages.isEmpty()) {
            0
        } else {
            uiState.messages.lastIndex + if (uiState.isLoading) 1 else 0
        }

        listState.animateScrollToItem(lastItem)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Simple heading for the AI Coach screen.
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "AI Coach",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Ask questions about your wellness and goals.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        // Display the conversation in a scrollable list.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Show instructions before the first message is sent.
            if (uiState.messages.isEmpty()) {
                item {
                    Text(
                        text = "Ask me about your hydration, sleep, " +
                                "workouts, nutrition, or other wellness habits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Show user messages on the right and AI responses on the left.
            items(uiState.messages) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isFromUser) {
                        Arrangement.End
                    } else {
                        Arrangement.Start
                    }
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 320.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (message.isFromUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (message.isFromUser) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Show a loading indicator while waiting for Cloudflare.
            if (uiState.isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )

                        Text("AI Coach is thinking...")
                    }
                }
            }
        }

        // Show errors and allow retrying a failed question.
        if (uiState.errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.errorMessage,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                if (uiState.failedQuestion != null) {
                    TextButton(
                        onClick = onRetry,
                        enabled = !uiState.isLoading
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

        // The input area stays at the bottom of the screen.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.question,
                onValueChange = onQuestionChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Ask a wellness question...")
                },
                maxLines = 4,
                enabled = !uiState.isLoading
            )

            // Disable Send for empty questions or while loading.
            Button(
                onClick = onSend,
                enabled = !uiState.isLoading &&
                        uiState.question.isNotBlank() &&
                        uiState.question.length <= 500
            ) {
                Text("Send")
            }
        }
    }
}
