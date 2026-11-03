package io.github.philips148.wellnesscoach.data.wellness

import io.github.philips148.wellnesscoach.data.model.WorkoutEntry
import java.time.LocalDate
import java.time.format.DateTimeParseException

object WorkoutValidation {
    fun validDate(date: String): Boolean = try {
        LocalDate.parse(date).toString() == date
    } catch (_: DateTimeParseException) { false }

    fun rangeError(start: String, end: String): String? = when {
        !validDate(start) || !validDate(end) -> "Enter real dates using yyyy-MM-dd."
        start > end -> "History start date must not be after the end date."
        else -> null
    }

    fun error(entry: WorkoutEntry): String? {
        if (!validDate(entry.date)) return "Enter a real workout date using yyyy-MM-dd."
        when (entry.workoutType) {
            "strength" -> {
                if (entry.exerciseName.isBlank()) return "Exercise name is required."
                if (entry.sets.isEmpty()) return "Add at least one strength set."
                entry.sets.forEachIndexed { index, set ->
                    if (set.repetitions <= 0) return "Set ${index + 1}: reps must be a positive whole number."
                    if (set.weightLbs != null && (!set.weightLbs.isFinite() || set.weightLbs < 0)) {
                        return "Set ${index + 1}: weight must be zero or a positive number in pounds."
                    }
                }
                if (entry.distanceMiles != null || entry.durationMinutes != null) {
                    return "Strength entries must not include walk/run distance or duration."
                }
            }
            "walk", "run" -> {
                val distance = entry.distanceMiles
                if (distance == null || !distance.isFinite() || distance <= 0) {
                    return "Distance must be a positive number in miles."
                }
                if (entry.durationMinutes == null || entry.durationMinutes <= 0) {
                    return "Duration must be a positive whole number of minutes."
                }
                if (entry.sets.isNotEmpty()) return "Walk/run entries must not contain strength sets."
            }
            else -> return "Choose Strength, Walk, or Run."
        }
        return null
    }
}
