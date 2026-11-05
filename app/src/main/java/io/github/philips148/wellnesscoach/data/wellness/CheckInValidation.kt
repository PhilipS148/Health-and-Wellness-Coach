package io.github.philips148.wellnesscoach.data.wellness

import io.github.philips148.wellnesscoach.data.model.CheckInEntry
import java.time.LocalDate
import java.time.format.DateTimeParseException

object CheckInValidation {
    fun validDate(date: String): Boolean = try {
        LocalDate.parse(date).toString() == date
    } catch (_: DateTimeParseException) { false }

    fun rangeError(start: String, end: String): String? = when {
        !validDate(start) || !validDate(end) -> "Enter real dates using yyyy-MM-dd."
        start > end -> "History start date must not be after the end date."
        else -> null
    }

    fun error(entry: CheckInEntry): String? {
        if (!validDate(entry.date)) return "Enter a real check-in date using yyyy-MM-dd."
        val ratings = listOf("Stress" to entry.stressLevel, "Mood" to entry.mood, "Energy" to entry.energyLevel)
        for ((name, rating) in ratings) {
            if (rating != null && rating !in 1..10) return "$name must be a whole number from 1 to 10."
        }
        if (ratings.all { it.second == null }) return "Record at least one rating before saving."
        return null
    }
}
