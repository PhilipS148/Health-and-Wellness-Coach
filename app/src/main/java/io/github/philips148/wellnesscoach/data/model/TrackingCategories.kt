package io.github.philips148.wellnesscoach.data.model

// Shared identifiers for the categories users can choose to track.
object TrackingCategories {
    const val MEALS = "meals"
    const val HYDRATION = "hydration"
    const val WORKOUTS = "workouts"
    const val SLEEP = "sleep"
    const val STRESS = "stress"
    const val MOOD = "mood"
    const val ENERGY = "energy"
    const val STEPS = "steps"

    val ALL: List<String> = listOf(
        MEALS,
        HYDRATION,
        WORKOUTS,
        SLEEP,
        STRESS,
        MOOD,
        ENERGY,
        STEPS
    )
}