package io.github.philips148.wellnesscoach.ui.coach

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.coach.CoachRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// One message displayed in the AI Coach conversation.
data class CoachMessage(
    val text: String,
    val isFromUser: Boolean
)

// Information the chat screen needs to display.
data class CoachUiState(
    val question: String = "",
    val messages: List<CoachMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val failedQuestion: String? = null
)

class CoachViewModel(
    private val coachRepository: CoachRepository
) : ViewModel() {

    var uiState by mutableStateOf(CoachUiState())
        private set

    // Update the question as the user types.
    fun onQuestionChange(question: String) {
        uiState = uiState.copy(
            question = question,
            errorMessage = null,
            failedQuestion = null
        )
    }

    // Validate the question before sending it.
    fun sendQuestion() {
        if (uiState.isLoading) return

        val question = uiState.question.trim()

        if (question.isEmpty()) {
            uiState = uiState.copy(
                errorMessage = "Please enter a question."
            )
            return
        }

        if (question.length > 500) {
            uiState = uiState.copy(
                errorMessage = "Questions must be 500 characters or fewer."
            )
            return
        }

        requestAnswer(question, addUserMessage = true)
    }

    // Retry the last failed request without duplicating its message.
    fun retry() {
        if (uiState.isLoading) return

        val question = uiState.failedQuestion ?: return
        requestAnswer(question, addUserMessage = false)
    }

    private fun requestAnswer(
        question: String,
        addUserMessage: Boolean
    ) {
        if (uiState.isLoading) return

        val updatedMessages = if (addUserMessage) {
            uiState.messages + CoachMessage(question, isFromUser = true)
        } else {
            uiState.messages
        }

        uiState = uiState.copy(
            question = if (addUserMessage) "" else uiState.question,
            messages = updatedMessages,
            isLoading = true,
            errorMessage = null,
            failedQuestion = null
        )

        viewModelScope.launch {
            try {
                val result = coachRepository.askQuestion(question)

                result.fold(
                    onSuccess = { answer ->
                        uiState = uiState.copy(
                            messages = uiState.messages + CoachMessage(
                                text = answer,
                                isFromUser = false
                            ),
                            isLoading = false
                        )
                    },
                    onFailure = { error ->
                        showError(question, error)
                    }
                )
            } catch (error: CancellationException) {
                // Stop normally when the ViewModel is destroyed.
                throw error
            } catch (error: Exception) {
                showError(question, error)
            }
        }
    }

    private fun showError(question: String, error: Throwable) {
        uiState = uiState.copy(
            isLoading = false,
            errorMessage = error.message
                ?: "The AI Coach is unavailable. Please try again.",
            failedQuestion = question
        )
    }
}
