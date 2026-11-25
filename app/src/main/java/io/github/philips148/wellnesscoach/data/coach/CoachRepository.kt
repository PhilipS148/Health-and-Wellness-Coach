package io.github.philips148.wellnesscoach.data.coach

import io.github.philips148.wellnesscoach.data.auth.FirebaseAuthDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.time.LocalDate

// Defines how the app sends questions to the AI Coach.
interface CoachRepository {
    suspend fun askQuestion(message: String): Result<String>
}

class DefaultCoachRepository(
    private val authDataSource: FirebaseAuthDataSource
) : CoachRepository {

    override suspend fun askQuestion(message: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val question = message.trim()

                // Reject empty or overly long questions.
                require(question.isNotEmpty()) {
                    "Please enter a question."
                }
                require(question.length <= 500) {
                    "Questions must be 500 characters or fewer."
                }

                // Identify the signed-in account and get its Firebase token.
                val userId = authDataSource.getCurrentUserId()
                    ?: throw IllegalStateException("Please sign in again.")

                val token = authDataSource.getCurrentUserIdToken()

                // Connect to our deployed Cloudflare backend.
                val connection = URL(
                    "https://ai-coach.steep-mountain-2405.workers.dev/coach/chat"
                ).openConnection() as HttpURLConnection

                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 75_000
                    connection.doOutput = true

                    connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                    )
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer $token"
                    )

                    // Send the question and the phone's current date.
                    val requestBody = JSONObject()
                        .put("message", question)
                        .put("date", LocalDate.now().toString())
                        .toString()

                    connection.outputStream.use { output ->
                        output.write(requestBody.toByteArray(Charsets.UTF_8))
                    }

                    val status = connection.responseCode

                    // Read either the answer or the backend's error message.
                    val responseText = (
                            if (status in 200..299) {
                                connection.inputStream
                            } else {
                                connection.errorStream
                            }
                            )?.bufferedReader()?.use { it.readText() }.orEmpty()

                    // Do not return a previous account's response.
                    if (authDataSource.getCurrentUserId() != userId) {
                        throw IllegalStateException(
                            "Your account changed. Please try again."
                        )
                    }

                    val response = runCatching {
                        JSONObject(responseText)
                    }.getOrNull()

                    if (status !in 200..299) {
                        val error = response?.optString("error")
                            ?.takeIf { it.isNotBlank() }
                            ?: "The AI Coach is unavailable. Please try again."

                        throw IllegalStateException(error)
                    }

                    val reply = response?.optString("reply")
                        ?.trim()
                        .orEmpty()

                    if (reply.isEmpty()) {
                        throw IllegalStateException(
                            "The AI Coach returned an invalid response. Please try again."
                        )
                    }

                    Result.success(reply)

                } finally {
                    connection.disconnect()
                }

            } catch (error: CancellationException) {
                // Allow the ViewModel to cancel requests normally.
                throw error

            } catch (error: SocketTimeoutException) {
                Result.failure(
                    IllegalStateException(
                        "The AI Coach took too long to respond. Please try again.",
                        error
                    )
                )

            } catch (error: IOException) {
                Result.failure(
                    IllegalStateException(
                        "Could not reach the AI Coach. Check your connection and try again.",
                        error
                    )
                )

            } catch (error: Exception) {
                Result.failure(error)
            }
        }
}
