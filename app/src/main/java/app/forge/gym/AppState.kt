package app.forge.gym

import android.content.Context
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import app.forge.gym.data.Exercise
import app.forge.gym.data.ExerciseLog
import app.forge.gym.data.LiveExercise
import app.forge.gym.data.LiveSet
import app.forge.gym.data.LiveWorkout
import app.forge.gym.data.Routine
import app.forge.gym.data.RoutineItem
import app.forge.gym.data.STARTER_ROUTINES
import app.forge.gym.data.SetLog
import app.forge.gym.data.WorkoutLog
import app.forge.gym.data.displayToKg
import app.forge.gym.data.parseNumber
import app.forge.gym.data.ExerciseRepo
import app.forge.gym.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppState(val context: Context) {
    val store = Store(context)

    var exercises by mutableStateOf<List<Exercise>>(emptyList())
        private set

    private var byId: Map<String, Exercise> = emptyMap()

    private var motionMap: Map<String, String> = emptyMap()

    /** Name of the rendered 3D movement loop for an exercise, if one exists. */
    fun motionFor(id: String): String? = motionMap[id]

    /** HD 3D thumbnail for an exercise (falls back to the stock photo). */
    fun thumbFor(e: Exercise?): String? {
        if (e == null) return null
        val mv = motionMap[e.id] ?: return e.imageUrl(0)
        return app.forge.gym.ui.Motion.thumb(mv, store.profile.gender == app.forge.gym.data.Gender.FEMALE)
    }

    /** A library exercise that uses [movement], preferring one that trains [muscle]. */
    fun exampleFor(movement: String, muscle: String?, exclude: String? = null): Exercise? {
        val cands = exercises.filter { motionMap[it.id] == movement && it.id != exclude }
        return cands.firstOrNull { muscle != null && muscle in it.primary && it.level == "beginner" }
            ?: cands.firstOrNull { muscle != null && muscle in it.primary }
            ?: cands.minByOrNull { it.name.length }
    }

    suspend fun loadExercises() {
        if (exercises.isNotEmpty()) return
        val prefs = context.getSharedPreferences("forge_motion", Context.MODE_PRIVATE)
        app.forge.gym.ui.Motion.rev = prefs.getString("rev", "") ?: ""
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val r = app.forge.gym.ui.Motion.refreshRev()
            if (r.isNotEmpty()) {
                prefs.edit().putString("rev", r).apply()
                withContext(Dispatchers.Main) { app.forge.gym.ui.Motion.rev = r }
            }
        }
        motionMap = withContext(Dispatchers.IO) {
            runCatching {
                val o = org.json.JSONObject(context.assets.open("motion_map.json").bufferedReader().use { it.readText() })
                o.keys().asSequence().associateWith { o.getString(it) }
            }.getOrDefault(emptyMap())
        }
        val list = withContext(Dispatchers.Default) { ExerciseRepo.load(context) }
        byId = list.associateBy { it.id }
        exercises = list
        seedRoutines()
    }

    // ---- the workout in progress -------------------------------------------------

    var live by mutableStateOf(LiveWorkout.fromJson(store.readLive()))
        private set

    fun saveLive() { store.saveLive(live?.toJson()?.toString()) }

    /** Starts an empty workout, or one filled from [routine]. */
    fun startWorkout(routine: Routine? = null) {
        val w = LiveWorkout(routine?.title ?: defaultTitle(), System.currentTimeMillis())
        routine?.items?.forEach { item -> w.exercises.add(LiveExercise(item.exerciseId, item.name, List(item.sets.coerceAtLeast(1)) { LiveSet() })) }
        live = w
        saveLive()
    }

    /** Adds an exercise to the workout in progress, with as many sets as last time (at least one). */
    fun addToLive(e: Exercise) {
        val w = live ?: return
        val last = store.lastSetsFor(e.id).size
        val n = if (last == 0) 3 else last.coerceAtMost(8)
        w.exercises.add(LiveExercise(e.id, e.name, List(n) { LiveSet() }))
        saveLive()
    }

    fun discardLive() {
        live = null
        store.saveLive(null)
    }

    /** Saves the ticked sets as a finished workout. Returns its id, or null if nothing was ticked. */
    fun finishLive(): Long? {
        val w = live ?: return null
        val useKg = store.profile.useKg
        val logs = w.exercises.mapNotNull { e ->
            val sets = e.sets.filter { it.done }.map { SetLog(parseNumber(it.reps).toInt(), parseNumber(it.weight).displayToKg(useKg), it.warm) }
            if (sets.isEmpty()) null else ExerciseLog(e.exerciseId, e.name, sets)
        }
        if (logs.isEmpty()) return null
        val now = System.currentTimeMillis()
        val log = WorkoutLog(now, w.title.trim().ifEmpty { defaultTitle() }, w.startedAt, ((now - w.startedAt) / 1000L).toInt(), logs)
        store.addWorkout(log)
        discardLive()
        return log.id
    }

    private fun defaultTitle(): String {
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when { h < 12 -> "Morning workout"; h < 17 -> "Afternoon workout"; else -> "Evening workout" }
    }

    private fun seedRoutines() {
        if (store.routinesSeeded) return
        store.routinesSeeded = true
        if (store.routines.isNotEmpty()) return
        STARTER_ROUTINES.forEachIndexed { i, (title, items) ->
            val its = items.mapNotNull { (id, sets) -> byId[id]?.let { RoutineItem(id, it.name, sets) } }
            if (its.isNotEmpty()) store.saveRoutine(Routine(i + 1L, title, its))
        }
    }

    /** Reads [exercises] first so screens recompose once the library has loaded. */
    fun exercise(id: String): Exercise? = if (exercises.isEmpty()) null else byId[id]
}

val LocalApp = staticCompositionLocalOf<AppState> { error("AppState not provided") }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Marks an element as shared between two screens (list card to detail header). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedKey(key: String): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val anim = LocalAnimScope.current ?: return this
    return with(shared) {
        this@sharedKey.sharedElement(
            state = rememberSharedContentState(key = key),
            animatedVisibilityScope = anim
        )
    }
}
