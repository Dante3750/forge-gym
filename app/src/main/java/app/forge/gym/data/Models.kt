package app.forge.gym.data

import kotlin.math.roundToInt

enum class Gender(val label: String) {
    MALE("Male"),
    FEMALE("Female")
}

enum class Level(val label: String, val blurb: String) {
    BEGINNER("Beginner", "New to lifting, or coming back after a long break."),
    INTERMEDIATE("Intermediate", "Training 6+ months. You know the basic lifts."),
    HARD("Hard", "Years under the bar. Bring volume and intensity.")
}

enum class Goal(val label: String, val blurb: String) {
    BUILD("Build muscle", "Size and shape. Moderate reps, steady progress."),
    STRENGTH("Get stronger", "Heavy sets, low reps, long rests."),
    LOSE("Lose fat", "Higher reps, shorter rests, a cardio finisher."),
    FIT("Stay fit", "Balanced training that fits around your life.")
}

enum class Place(val label: String, val blurb: String) {
    GYM("Gym", "Barbells, dumbbells, machines, cables."),
    HOME("Home", "Bodyweight and resistance bands only.")
}

data class Exercise(
    val id: String,
    val name: String,
    val level: String,
    val equipment: String,
    val category: String,
    val mechanic: String,
    val force: String,
    val primary: List<String>,
    val secondary: List<String>,
    val steps: List<String>,
    val images: List<String>
) {
    val isCompound: Boolean get() = mechanic == "compound"
    val isBodyweight: Boolean get() = equipment == "body only" || equipment == "none"
    val levelLabel: String
        get() = when (level) {
            "expert" -> "Hard"
            "intermediate" -> "Intermediate"
            else -> "Beginner"
        }

    fun imageUrl(i: Int = 0): String? =
        images.getOrNull(i)?.let { "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/$it" }
}

data class Profile(
    val name: String = "",
    val gender: Gender = Gender.MALE,
    val level: Level = Level.BEGINNER,
    val goal: Goal = Goal.BUILD,
    val place: Place = Place.GYM,
    val daysPerWeek: Int = 3,
    val heightCm: Int = 172,
    val goalWeightKg: Float = 0f,
    val useKg: Boolean = true,
    val stepGoal: Int = 8000,
    val waterGoalMl: Int = 2500,
    val planSeed: Int = 0,
    val onboarded: Boolean = false
) {
    val planKey: String get() = "$gender|$level|$goal|$place|$daysPerWeek|$planSeed"
}

data class SetLog(val reps: Int, val weightKg: Float)

data class ExerciseLog(val exerciseId: String, val name: String, val sets: List<SetLog>)

data class WorkoutLog(
    val id: Long,
    val title: String,
    val startedAt: Long,
    val durationSec: Int,
    val exercises: List<ExerciseLog>
) {
    val volumeKg: Float get() = exercises.sumOf { ex -> ex.sets.sumOf { (it.reps * it.weightKg).toDouble() } }.toFloat()
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
}

data class WeightEntry(val time: Long, val kg: Float, val source: String)

data class WalkLog(val start: Long, val durationSec: Int, val steps: Int, val distanceM: Float, val kcal: Int)

data class ActiveWalk(
    val startedAt: Long,
    val running: Boolean,
    val accumSteps: Int,
    val accumMillis: Long,
    val lastResumeAt: Long,
    val baseline: Float
)

data class PlanExercise(val exerciseId: String, val sets: Int, val reps: String, val restSec: Int)

data class PlanDay(val title: String, val focus: String, val items: List<PlanExercise>)

fun Float.kgToDisplay(useKg: Boolean): Float = if (useKg) this else this * 2.20462f

fun Float.displayToKg(useKg: Boolean): Float = if (useKg) this else this / 2.20462f

fun formatWeight(kg: Float, useKg: Boolean): String {
    val v = kg.kgToDisplay(useKg)
    val rounded = (v * 10f).roundToInt() / 10f
    return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
}

fun unitLabel(useKg: Boolean) = if (useKg) "kg" else "lb"

fun formatClock(totalSec: Int): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun prettyMuscle(m: String): String = when (m) {
    "abdominals" -> "Abs"
    "middle back" -> "Mid back"
    "lower back" -> "Lower back"
    "quadriceps" -> "Quads"
    else -> m.replaceFirstChar { it.uppercase() }
}

val ALL_MUSCLES = listOf(
    "chest", "shoulders", "biceps", "triceps", "forearms", "abdominals",
    "lats", "middle back", "lower back", "traps", "neck",
    "glutes", "quadriceps", "hamstrings", "calves", "adductors", "abductors"
)
