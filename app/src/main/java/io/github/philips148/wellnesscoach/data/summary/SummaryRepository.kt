package io.github.philips148.wellnesscoach.data.summary

interface SummaryRepository {

    suspend fun getDailySummary(
        date: String
    ): Result<DailySummary>

    suspend fun getWeeklySummary(
        startDate: String
    ): Result<WeeklySummary>
}