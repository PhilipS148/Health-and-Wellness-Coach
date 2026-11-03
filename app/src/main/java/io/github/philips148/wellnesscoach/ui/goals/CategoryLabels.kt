package io.github.philips148.wellnesscoach.ui.goals

import io.github.philips148.wellnesscoach.data.model.TrackingCategories

fun categoryLabel(id: String): String = when (id) {
    TrackingCategories.MEALS -> "Meals & Calories"
    TrackingCategories.HYDRATION -> "Hydration"
    TrackingCategories.WORKOUTS -> "Workouts"
    TrackingCategories.SLEEP -> "Sleep"
    TrackingCategories.STRESS -> "Stress"
    TrackingCategories.MOOD -> "Mood"
    TrackingCategories.ENERGY -> "Energy"
    TrackingCategories.STEPS -> "Steps"
    else -> id
}