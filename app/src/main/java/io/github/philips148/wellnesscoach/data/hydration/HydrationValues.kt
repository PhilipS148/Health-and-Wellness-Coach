package io.github.philips148.wellnesscoach.data.hydration

import io.github.philips148.wellnesscoach.data.model.WaterEntry
import java.time.LocalDate
import java.time.format.DateTimeParseException

object HydrationValues {
    fun validDate(value: String): Boolean = try {
        LocalDate.parse(value).toString() == value
    } catch (_: DateTimeParseException) {
        false
    }

    fun inputError(date: String, amount: String): String? {
        if (!validDate(date)) return "Enter a real date using yyyy-MM-dd."
        val ounces = amount.trim().toIntOrNull()
        if (ounces == null || ounces <= 0) {
            return "Enter a positive whole-number amount in US fl oz."
        }
        return null
    }

    // Calculate from records each time, rather than keeping a separate total.
    fun totalForDate(entries: List<WaterEntry>, date: String): Long =
        entries.filter { it.date == date }.sumOf { it.amountOz.toLong() }

    fun progress(total: Long, goal: Int): Float =
        if (goal > 0) (total.toDouble() / goal).coerceIn(0.0, 1.0).toFloat()
        else 0f
}
