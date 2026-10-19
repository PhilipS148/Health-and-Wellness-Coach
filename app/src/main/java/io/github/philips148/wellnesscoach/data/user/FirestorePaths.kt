package io.github.philips148.wellnesscoach.data.user

object FirestorePaths {
    const val USERS = "users"
    const val MEALS = "meals"
    const val WATER = "water"
    const val WORKOUTS = "workouts"
    const val SLEEP = "sleep"
    const val CHECK_INS = "checkIns"
    const val STEPS = "steps"

    val entry_collections = listOf(MEALS, WATER, WORKOUTS, SLEEP, CHECK_INS, STEPS)
}
