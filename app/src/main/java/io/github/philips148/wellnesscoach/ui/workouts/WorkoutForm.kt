package io.github.philips148.wellnesscoach.ui.workouts

import io.github.philips148.wellnesscoach.data.model.WorkoutEntry
import io.github.philips148.wellnesscoach.data.model.WorkoutSet
import io.github.philips148.wellnesscoach.data.wellness.WorkoutValidation
import java.time.LocalDate

data class WorkoutSetInput(val reps: String = "", val weightLbs: String = "")

data class WorkoutForm(
    val date: String = LocalDate.now().toString(),
    val type: String = "strength",
    val name: String = "",
    val sets: List<WorkoutSetInput> = listOf(WorkoutSetInput()),
    val distanceMiles: String = "",
    val durationMinutes: String = ""
) {
    fun toEntry(existing: WorkoutEntry? = null): Result<WorkoutEntry> = runCatching {
        val parsedSets = if (type == "strength") sets.mapIndexed { index, set ->
            val reps = set.reps.trim().toIntOrNull()
                ?: throw IllegalArgumentException("Set ${index + 1}: enter positive whole-number reps.")
            val weightText = set.weightLbs.trim()
            val weight = if (weightText.isEmpty()) null else weightText.toDoubleOrNull()
                ?: throw IllegalArgumentException("Set ${index + 1}: enter a valid weight in pounds.")
            WorkoutSet(repetitions = reps, weightLbs = weight)
        } else emptyList()
        val distance = if (type == "strength") null else distanceMiles.trim().toDoubleOrNull()
        val duration = if (type == "strength") null else durationMinutes.trim().toIntOrNull()
        val entry = (existing ?: WorkoutEntry()).copy(
            date = date.trim(), workoutType = type, exerciseName = name.trim(),
            sets = parsedSets, distanceMiles = distance, durationMinutes = duration
        )
        WorkoutValidation.error(entry)?.let { throw IllegalArgumentException(it) }
        entry
    }

    companion object {
        fun from(entry: WorkoutEntry) = WorkoutForm(
            date = entry.date, type = entry.workoutType, name = entry.exerciseName,
            sets = if (entry.sets.isEmpty()) listOf(WorkoutSetInput()) else entry.sets.map {
                WorkoutSetInput(it.repetitions.toString(), it.weightLbs?.toString().orEmpty())
            },
            distanceMiles = entry.distanceMiles?.toString().orEmpty(),
            durationMinutes = entry.durationMinutes?.toString().orEmpty()
        )
    }
}
