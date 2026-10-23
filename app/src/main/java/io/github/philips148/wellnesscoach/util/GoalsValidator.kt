package io.github.philips148.wellnesscoach.util

object GoalsValidator {
    fun wholeNumberGoalError(text: String, label: String): String? {
        if (text.isBlank()) {
            return "$label is required."
        }
        val num = text.trim().toIntOrNull()
        if (num == null) {
            return "$label must be a whole number."
        }
        if (num <= 0) {
            return "$label must be greater than zero."
        }
        return null
    }

    fun sleepGoalError(text: String): String? {
        if (text.isBlank()) {
            return "Sleep goal is required."
        }
        val num = text.trim().toDoubleOrNull()
        if (num == null) {
            return "Sleep goal must be a number."
        }
        if (!(num > 0 && num <= 24)) {
            return "Sleep goal must be greater than 0 and no more than 24 hours."
        }

        return null
    }

}