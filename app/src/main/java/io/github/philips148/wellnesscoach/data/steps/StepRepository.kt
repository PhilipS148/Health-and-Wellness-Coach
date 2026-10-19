package io.github.philips148.wellnesscoach.data.steps

import io.github.philips148.wellnesscoach.data.model.StepTotal

// All operations apply to the currently signed-in user.
interface StepRepository {

    // Save under users/{uid}/steps/{date}.
    // The date is also the document ID.
    // Replace the previous total for that date on refresh.
    suspend fun saveDailyTotal(
        total: StepTotal
    ): Result<Unit>

    // Dates use yyyy-MM-dd, including both range endpoints.
    suspend fun getStepTotals(
        startDate: String,
        endDate: String
    ): Result<List<StepTotal>>
}