package app.forge.gym

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
import app.forge.gym.data.ExerciseRepo
import app.forge.gym.data.PlanBuilder
import app.forge.gym.data.PlanDay
import app.forge.gym.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class AppState(val context: Context) {
    val store = Store(context)

    var exercises by mutableStateOf<List<Exercise>>(emptyList())
        private set

    private var byId: Map<String, Exercise> = emptyMap()
    private var cachedKey: String? = null
    private var cachedPlan: List<PlanDay> = emptyList()

    suspend fun loadExercises() {
        if (exercises.isNotEmpty()) return
        val list = withContext(Dispatchers.Default) { ExerciseRepo.load(context) }
        byId = list.associateBy { it.id }
        exercises = list
    }

    fun exercise(id: String): Exercise? = byId[id]

    /** The weekly program for the current profile. Cached until the profile or the library changes. */
    fun plan(): List<PlanDay> {
        val list = exercises
        val p = store.profile
        val key = p.planKey + "|" + list.size
        if (key != cachedKey) {
            cachedPlan = PlanBuilder.build(p, list)
            cachedKey = key
        }
        return cachedPlan
    }

    /** Which day of the plan is up next. */
    fun nextDayIndex(): Int {
        val n = plan().size
        if (n == 0) return 0
        return store.workouts.size % n
    }
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

@Suppress("DEPRECATION")
fun buzz(context: Context, long: Boolean = true) {
    try {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!v.hasVibrator()) return
        val pattern = if (long) longArrayOf(0, 180, 90, 180, 90, 320) else longArrayOf(0, 60)
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            v.vibrate(pattern, -1)
        }
    } catch (e: Exception) {
        // vibration is a nicety, never a reason to crash
    }
}

fun greeting(hour: Int): String = when {
    hour < 5 -> "Late night"
    hour < 12 -> "Morning"
    hour < 17 -> "Afternoon"
    else -> "Evening"
}

private val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
fun formatDay(d: LocalDate): String = d.format(dateFmt)
