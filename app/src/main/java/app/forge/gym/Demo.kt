package app.forge.gym

import android.content.Intent
import android.content.pm.ApplicationInfo
import app.forge.gym.data.ExerciseLog
import app.forge.gym.data.Gender
import app.forge.gym.data.SetLog
import app.forge.gym.data.WorkoutLog

/**
 * Debug-build hooks used by CI to take screenshots:
 *   adb shell am start -n app.forge.gym/.MainActivity --ez forge_demo true --es forge_route tab:0
 * Routes: tab:0..2, home:<muscle>, body, live, routine, exercise:<id>, viewer:<id>, welcome.
 */
object Demo {
    fun isDebuggable(app: AppState) = (app.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun route(intent: Intent?, app: AppState): String? {
        if (intent == null || !isDebuggable(app)) return null
        if (intent.getBooleanExtra("forge_demo", false)) seed(app)
        return intent.getStringExtra("forge_route")
    }

    private const val DAY = 86_400_000L

    private fun seed(app: AppState) {
        val store = app.store
        if (store.workouts.isNotEmpty()) return
        store.updateProfile(store.profile.copy(name = "Aarav", gender = Gender.MALE, onboarded = true))
        val now = System.currentTimeMillis()
        val sessions = listOf(
            "Push · chest, shoulders, triceps" to listOf("Barbell_Bench_Press_-_Medium_Grip" to "Barbell Bench Press - Medium Grip", "Incline_Dumbbell_Press" to "Incline Dumbbell Press", "Standing_Military_Press" to "Standing Military Press"),
            "Legs" to listOf("Barbell_Full_Squat" to "Barbell Full Squat", "Romanian_Deadlift" to "Romanian Deadlift", "Leg_Press" to "Leg Press"),
            "Pull · back, biceps" to listOf("Pullups" to "Pullups", "Bent_Over_Barbell_Row" to "Bent Over Barbell Row", "Barbell_Curl" to "Barbell Curl")
        )
        listOf(1, 3, 5, 8, 10, 12, 16, 19, 23, 30).reversed().forEachIndexed { k, d ->
            val s = sessions[k % sessions.size]
            val logs = s.second.map { (id, name) ->
                val base = when {
                    id.contains("Squat") -> 80f
                    id.contains("Deadlift") -> 90f
                    id.contains("Bench") -> 60f
                    id.contains("Leg_Press") -> 140f
                    id == "Pullups" -> 0f
                    else -> 30f
                }
                ExerciseLog(id, name, List(3) { SetLog(8 + (it % 2), if (base > 0f) base + k * 2.5f else 0f) })
            }
            val at = now - d * DAY - 3_600_000L
            store.addWorkout(WorkoutLog(at, s.first, at, 48 * 60 + k * 41, logs))
        }
    }
}
