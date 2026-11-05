package io.github.philips148.wellnesscoach.ui.checkins

import io.github.philips148.wellnesscoach.data.model.CheckInEntry
import io.github.philips148.wellnesscoach.data.model.TrackingCategories
import io.github.philips148.wellnesscoach.data.wellness.CheckInValidation
import java.time.LocalDate

// Blank means unrecorded. Never substitute a neutral rating for missing input.
data class CheckInForm(
    val date: String = LocalDate.now().toString(),
    val stress: String = "",
    val mood: String = "",
    val energy: String = ""
) {
    fun toEntry(selectedCategories: Set<String>, existing: CheckInEntry? = null): Result<CheckInEntry> = runCatching {
        fun rating(category: String, name: String, text: String, previous: Int?): Int? {
            if (category !in selectedCategories) return previous
            if (text.isBlank()) return null
            return text.trim().toIntOrNull()
                ?: throw IllegalArgumentException("$name must be a whole number from 1 to 10.")
        }
        val entry = (existing ?: CheckInEntry()).copy(
            date = date.trim(),
            stressLevel = rating(TrackingCategories.STRESS, "Stress", stress, existing?.stressLevel),
            mood = rating(TrackingCategories.MOOD, "Mood", mood, existing?.mood),
            energyLevel = rating(TrackingCategories.ENERGY, "Energy", energy, existing?.energyLevel)
        )
        CheckInValidation.error(entry)?.let { throw IllegalArgumentException(it) }
        entry
    }

    companion object {
        fun from(entry: CheckInEntry) = CheckInForm(
            date = entry.date,
            stress = entry.stressLevel?.toString().orEmpty(),
            mood = entry.mood?.toString().orEmpty(),
            energy = entry.energyLevel?.toString().orEmpty()
        )
    }
}
