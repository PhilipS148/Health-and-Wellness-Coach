package io.github.philips148.wellnesscoach.data.hydration

import io.github.philips148.wellnesscoach.data.model.WaterEntry

// Hydration-only contract while W1's shared WellnessRepository is implemented.
// Uses the same model, operation names, and Firestore path as the shared contract.
interface WaterRepository {
    suspend fun createWaterEntry(entry: WaterEntry): Result<String>
    suspend fun getWaterEntries(date: String): Result<List<WaterEntry>>
    suspend fun updateWaterEntry(entry: WaterEntry): Result<Unit>
    suspend fun deleteWaterEntry(id: String): Result<Unit>
}
