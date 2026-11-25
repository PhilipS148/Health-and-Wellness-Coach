package io.github.philips148.wellnesscoach.ui.coach

import androidx.lifecycle.viewModelScope
import io.github.philips148.wellnesscoach.data.coach.CoachRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class CoachViewModelTest {

    private lateinit var repository: FakeCoachRepository
    private lateinit var viewModel: CoachViewModel

    // Set up a fresh repository and ViewModel before each test.
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        repository = FakeCoachRepository()
        viewModel = CoachViewModel(repository)
    }

    // Clean up after each test so it does not affect other tests.
    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    // Make sure an empty question is rejected.
    @Test
    fun emptyQuestionIsRejected() {
        viewModel.sendQuestion()

        assertEquals("Please enter a question.", viewModel.uiState.errorMessage)
        assertEquals(0, repository.requestCount)
    }

    // Make sure spaces alone are not accepted as a question.
    @Test
    fun whitespaceQuestionIsRejected() {
        viewModel.onQuestionChange("   ")
        viewModel.sendQuestion()

        assertEquals("Please enter a question.", viewModel.uiState.errorMessage)
        assertEquals(0, repository.requestCount)
    }

    // Make sure questions cannot exceed the backend's character limit.
    @Test
    fun longQuestionIsRejected() {
        viewModel.onQuestionChange("a".repeat(501))
        viewModel.sendQuestion()

        assertEquals(
            "Questions must be 500 characters or fewer.",
            viewModel.uiState.errorMessage
        )
        assertEquals(0, repository.requestCount)
    }

    // Make sure both the question and AI response appear in the chat.
    @Test
    fun successfulResponseAddsMessages() = runTest {
        viewModel.onQuestionChange("How can I improve my hydration?")
        viewModel.sendQuestion()

        advanceUntilIdle()

        val messages = viewModel.uiState.messages

        assertEquals(2, messages.size)
        assertTrue(messages[0].isFromUser)
        assertFalse(messages[1].isFromUser)
        assertEquals("Drink more water throughout the day.", messages[1].text)
        assertFalse(viewModel.uiState.isLoading)
        assertNull(viewModel.uiState.errorMessage)
    }

    // Make sure the loading state stays active while waiting for a response.
    @Test
    fun loadingStateIsUpdated() = runTest {
        viewModel.onQuestionChange("How am I doing?")
        viewModel.sendQuestion()

        assertTrue(viewModel.uiState.isLoading)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.isLoading)
    }

    // Make sure pressing Send multiple times does not create extra requests.
    @Test
    fun duplicateSubmissionsArePrevented() = runTest {
        viewModel.onQuestionChange("How can I sleep better?")

        viewModel.sendQuestion()
        viewModel.sendQuestion()
        viewModel.sendQuestion()

        advanceUntilIdle()

        assertEquals(1, repository.requestCount)
        assertEquals(2, viewModel.uiState.messages.size)
    }

    // Simulate a failed request and make sure the error is displayed.
    @Test
    fun failedRequestShowsError() = runTest {
        repository.response = Result.failure(
            IllegalStateException("Connection failed")
        )

        viewModel.onQuestionChange("How am I doing?")
        viewModel.sendQuestion()

        advanceUntilIdle()

        assertEquals("Connection failed", viewModel.uiState.errorMessage)
        assertEquals("How am I doing?", viewModel.uiState.failedQuestion)
        assertFalse(viewModel.uiState.isLoading)
    }

    // Make sure Retry works without duplicating the original question.
    @Test
    fun retryDoesNotDuplicateQuestion() = runTest {
        repository.response = Result.failure(
            IllegalStateException("Connection failed")
        )

        viewModel.onQuestionChange("How am I doing?")
        viewModel.sendQuestion()

        advanceUntilIdle()

        // Simulate the connection becoming available again.
        repository.response = Result.success("Keep working toward your goals.")

        viewModel.retry()

        advanceUntilIdle()

        assertEquals(2, repository.requestCount)
        assertEquals(2, viewModel.uiState.messages.size)
        assertEquals(
            "Keep working toward your goals.",
            viewModel.uiState.messages.last().text
        )
        assertNull(viewModel.uiState.errorMessage)
        assertNull(viewModel.uiState.failedQuestion)
    }

    // A fake repository that simulates the AI backend without using the internet.
    private class FakeCoachRepository : CoachRepository {

        // Counts how many requests were made.
        var requestCount = 0

        // Lets each test control whether the AI request succeeds or fails.
        var response: Result<String> = Result.success(
            "Drink more water throughout the day."
        )

        override suspend fun askQuestion(message: String): Result<String> {
            requestCount++

            // Simulate waiting for the AI backend to respond.
            delay(1000)

            return response
        }
    }
}
