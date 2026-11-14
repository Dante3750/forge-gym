package app.forge.gym.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** A saved list of exercises you can start a workout from. */
data class RoutineItem(val exerciseId: String, val name: String, val sets: Int)

data class Routine(val id: Long, val title: String, val items: List<RoutineItem>)

/** One row of the workout you are doing right now. Weight and reps are what is typed (in the unit on screen). */
class LiveSet(weight: String = "", reps: String = "", done: Boolean = false, warm: Boolean = false) {
    var weight by mutableStateOf(weight)
    var reps by mutableStateOf(reps)
    var done by mutableStateOf(done)
    var warm by mutableStateOf(warm)
}

class LiveExercise(val exerciseId: String, val name: String, sets: List<LiveSet>) {
    val sets = mutableStateListOf<LiveSet>().apply { addAll(sets) }
}

/** The workout in progress. Kept on disk as it changes, so closing the app does not lose it. */
class LiveWorkout(title: String, val startedAt: Long, exercises: List<LiveExercise> = emptyList()) {
    var title by mutableStateOf(title)
    val exercises = mutableStateListOf<LiveExercise>().apply { addAll(exercises) }
    /** Rest countdown: when it ends (clock time, 0 = not resting) and how long it was set for. */
    var restEndsAt by mutableStateOf(0L)
    var restTotalSec by mutableStateOf(0)
    /** Rest started automatically after each ticked set. 0 = off. */
    var restSec by mutableStateOf(90)

    val doneSets: Int get() = exercises.sumOf { e -> e.sets.count { it.done } }

    fun toJson(): JSONObject {
        val ex = JSONArray()
        exercises.forEach { e ->
            val sets = JSONArray()
            e.sets.forEach { s -> sets.put(JSONObject().put("w", s.weight).put("r", s.reps).put("d", s.done).put("wu", s.warm)) }
            ex.put(JSONObject().put("id", e.exerciseId).put("name", e.name).put("sets", sets))
        }
        return JSONObject().put("title", title).put("at", startedAt).put("rest", restSec).put("ex", ex)
    }

    companion object {
        fun fromJson(s: String?): LiveWorkout? {
            if (s == null) return null
            return try {
                val o = JSONObject(s)
                val exArr = o.getJSONArray("ex")
                val ex = List(exArr.length()) { i ->
                    val e = exArr.getJSONObject(i)
                    val sa = e.getJSONArray("sets")
                    LiveExercise(
                        e.getString("id"), e.getString("name"),
                        List(sa.length()) { k ->
                            val so = sa.getJSONObject(k)
                            LiveSet(so.optString("w", ""), so.optString("r", ""), so.optBoolean("d", false), so.optBoolean("wu", false))
                        }
                    )
                }
                LiveWorkout(o.optString("title", "Workout"), o.getLong("at"), ex).also { it.restSec = o.optInt("rest", 90) }
            } catch (e: Exception) {
                null
            }
        }
    }
}

/** Ready-made routines offered the first time, so the Workout tab is never empty. */
val STARTER_ROUTINES: List<Pair<String, List<Pair<String, Int>>>> = listOf(
    "Push · chest, shoulders, triceps" to listOf(
        "Barbell_Bench_Press_-_Medium_Grip" to 4, "Incline_Dumbbell_Press" to 3, "Standing_Military_Press" to 3,
        "Side_Lateral_Raise" to 3, "Triceps_Pushdown" to 3
    ),
    "Pull · back, biceps" to listOf(
        "Pullups" to 4, "Bent_Over_Barbell_Row" to 3, "Wide-Grip_Lat_Pulldown" to 3, "Face_Pull" to 3, "Barbell_Curl" to 3
    ),
    "Legs" to listOf(
        "Barbell_Full_Squat" to 4, "Romanian_Deadlift" to 3, "Leg_Press" to 3, "Lying_Leg_Curls" to 3, "Standing_Calf_Raises" to 3
    ),
    "Home · no equipment" to listOf(
        "Pushups" to 3, "Bodyweight_Squat" to 3, "Bench_Dips" to 3, "Crunches" to 3, "Plank" to 3
    )
)

fun parseNumber(s: String): Float = s.trim().replace(',', '.').toFloatOrNull() ?: 0f
