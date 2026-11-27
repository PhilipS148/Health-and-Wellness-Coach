package io.github.philips148.wellnesscoach.data.reminders

// Wall-clock times follow the phone's current time zone, not a fixed UTC offset.
data class ReminderPreference(val enabled: Boolean = false, val hour: Int = 10, val minute: Int = 0) {
    init { require(hour in 0..23 && minute in 0..59) { "Choose a valid reminder time." } }
}

data class ReminderPreferences(
    val hydration: ReminderPreference = ReminderPreference(hour = 10),
    val checkIn: ReminderPreference = ReminderPreference(hour = 20),
    val workout: ReminderPreference = ReminderPreference(hour = 17)
) {
    fun toFields(): Map<String, Any> = mapOf(
        "hydration" to hydration.fields(), "checkIn" to checkIn.fields(), "workout" to workout.fields()
    )

    companion object {
        fun fromFields(value: Any?): ReminderPreferences {
            if (value == null) return ReminderPreferences()
            val fields = value as? Map<*, *> ?: error("Couldn't read reminder preferences.")
            return ReminderPreferences(
                read(fields["hydration"], 10), read(fields["checkIn"], 20), read(fields["workout"], 17)
            )
        }

        private fun read(value: Any?, defaultHour: Int): ReminderPreference {
            if (value == null) return ReminderPreference(hour = defaultHour)
            val fields = value as? Map<*, *> ?: error("Couldn't read a reminder setting.")
            val enabled = fields["enabled"] as? Boolean ?: error("Couldn't read reminder status.")
            fun number(key: String): Int {
                val n = fields[key] as? Number ?: error("Couldn't read reminder time.")
                val x = n.toDouble()
                require(x.isFinite() && x == n.toInt().toDouble()) { "Choose a whole-number reminder time." }
                return n.toInt()
            }
            return ReminderPreference(enabled, number("hour"), number("minute"))
        }
    }
}

private fun ReminderPreference.fields(): Map<String, Any> = mapOf("enabled" to enabled, "hour" to hour, "minute" to minute)
