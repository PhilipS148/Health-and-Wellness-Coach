package io.github.philips148.wellnesscoach.util

import java.time.LocalDate
import java.time.format.DateTimeParseException

object SleepValidator {

    fun durationError(hoursText: String, minutesText: String): String? {
        if (hoursText.isBlank() && minutesText.isBlank()) {
            return "Enter how long you slept."
        }

        val numHours = if (hoursText.isBlank()) 0 else hoursText.trim().toIntOrNull()
        val numMinutes = if (minutesText.isBlank()) 0 else minutesText.trim().toIntOrNull()

        if (numHours == null) {
            return "Hours must be a whole number."
        }
        if (numMinutes == null) {
            return "Minutes must be a whole number."
        }
        if (numMinutes !in 0..59) {
            return "Minutes must be between 0 and 59."
        }
        if (numHours < 0) {
            return "Hours can't be negative."
        }

        val totalMinutes = numHours * 60 + numMinutes
        if (totalMinutes <= 0 || totalMinutes > 24 * 60) {
            return "Sleep must be more than 0 and no more than 24 hours."
        }

        return null
    }

    fun toHours(hoursText: String, minutesText: String): Double {
        val numHours = if (hoursText.isBlank()) 0 else hoursText.trim().toInt()
        val numMinutes = if (minutesText.isBlank()) 0 else minutesText.trim().toInt()

        return numHours + numMinutes / 60.0
    }

    fun splitHours(hoursSlept: Double): Pair<Int, Int> {
        val totalMinutes = Math.round(hoursSlept * 60).toInt()
        return Pair(totalMinutes / 60, totalMinutes % 60)
    }

    fun dateError(dateText: String, today: LocalDate): String? {
        val date = try {
            LocalDate.parse(dateText)
        } catch (e: DateTimeParseException) {
            return "Enter a valid date"
        }

        if (date.isAfter(today)) {
            return "The wake up date can't be in the future."
        }

        return null
    }
}




