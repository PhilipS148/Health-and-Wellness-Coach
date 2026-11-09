package io.github.philips148.wellnesscoach.data.steps

import io.github.philips148.wellnesscoach.data.auth.FirebaseAuthDataSource
import io.github.philips148.wellnesscoach.data.model.StepTotal
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.format.DateTimeParseException

// Checks step data and account access before asking the data source
// to communicate with Firestore.
class DefaultStepRepository(
    private val authDataSource: FirebaseAuthDataSource,
    private val stepDataSource: FirestoreStepDataSource
) : StepRepository {

    // Save one day's total under the currently signed-in account.
    override suspend fun saveDailyTotal(
        total: StepTotal
    ): Result<Unit> = result {
        val uid = requireUserId()

        // Check the date, record ID, and count before saving.
        validateTotal(total)

        // The data source replaces the document for this date.
        // Refreshing the same total does not add those steps again.
        stepDataSource.saveDailyTotal(uid, total)

        // Reject the result if the account changed during the request.
        checkSameUser(uid)
    }

    // Load saved daily totals, including both date-range endpoints.
    override suspend fun getStepTotals(
        startDate: String,
        endDate: String
    ): Result<List<StepTotal>> = result {
        val uid = requireUserId()

        // Convert the strings into real dates so we can compare them.
        val start = parseDate(startDate)
        val end = parseDate(endDate)

        require(!start.isAfter(end)) {
            "Start date must be on or before end date."
        }

        val totals = stepDataSource.getStepTotals(
            uid = uid,
            startDate = startDate,
            endDate = endDate
        )

        // Validate saved records too, before returning them to the screen.
        totals.forEach { total ->
            validateTotal(total)
        }

        checkSameUser(uid)
        totals
    }

    // Get the account ID from Firebase Authentication.
    // If nobody is signed in, stop the request with an error.
    private fun requireUserId(): String {
        return authDataSource.getCurrentUserId()
            ?: throw IllegalStateException(
                "Please sign in to access step totals."
            )
    }

    // Firebase requests take time. The user could sign out or switch
    // accounts while a request is running.
    private fun checkSameUser(uid: String) {
        check(authDataSource.getCurrentUserId() == uid) {
            "Your account changed. Please refresh and try again."
        }
    }

    // Check the information inside a daily step record.
    private fun validateTotal(total: StepTotal) {
        parseDate(total.date)

        // A new record can leave its ID empty.
        // If an ID is supplied, it must match the date.
        require(total.id.isEmpty() || total.id == total.date) {
            "The step record ID must match its date."
        }

        // Zero is a valid recorded count.
        require(total.steps >= 0) {
            "Steps cannot be negative."
        }
    }

    // Reject impossible dates, such as February 30.
    // Also require the exact yyyy-MM-dd format used by our database.
    private fun parseDate(value: String): LocalDate {
        val date = try {
            LocalDate.parse(value)
        } catch (_: DateTimeParseException) {
            throw IllegalArgumentException(
                "Enter a valid date in yyyy-MM-dd format."
            )
        }

        require(value.length == 10 && date.toString() == value) {
            "Enter a valid date in yyyy-MM-dd format."
        }

        return date
    }

    // Run an operation and return either its successful value or an error.
    // <T> lets this helper work with different results, such as
    // a list of step totals or a save operation that returns Unit.
    private suspend fun <T> result(
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (exception: CancellationException) {
            // Let Android cancel the work instead of treating
            // cancellation as an ordinary request failure.
            throw exception
        } catch (exception: Exception) {
            // Return the error so the ViewModel can handle it.
            Result.failure(exception)
        }
    }
}