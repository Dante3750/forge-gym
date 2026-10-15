package app.forge.gym

import android.content.Intent
import android.content.pm.ApplicationInfo
import app.forge.gym.data.ExerciseLog
import app.forge.gym.data.Gender
import app.forge.gym.data.Goal
import app.forge.gym.data.Level
import app.forge.gym.data.Place
import app.forge.gym.data.SetLog
import app.forge.gym.data.Store
import app.forge.gym.data.WalkLog
import app.forge.gym.data.WorkoutLog

/**
 * Debug-build hooks used by CI to take README screenshots:
 *   adb shell am start -n app.forge.gym/.MainActivity --ez forge_demo true --es forge_route tab:0
 * Routes: tab:0..3, tab:1:body, exercise:<id>, plan, session:<day>, settings, onboarding.
 */
object Demo {
    fun isDebuggable(app: AppState) = (app.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun route(intent: Intent?, app: AppState): String? {
        if (intent == null || !isDebuggable(app)) return null
        if (intent.getBooleanExtra("forge_demo", false)) seed(app.store)
        return intent.getStringExtra("forge_route")
    }

    private const val DAY = 86_400_000L

    fun seed(store: Store) {
        if (store.workouts.size >= 5) return
        store.resetAll()
        store.updateProfile(
            store.profile.copy(
                name = "Aarav", gender = Gender.MALE, level = Level.INTERMEDIATE, goal = Goal.BUILD, place = Place.GYM,
                daysPerWeek = 4, heightCm = 176, goalWeightKg = 74f, onboarded = true
            )
        )
        val now = System.currentTimeMillis()
        val kg = listOf(79.4f, 79.1f, 78.9f, 78.6f, 78.7f, 78.2f, 77.9f, 77.8f, 77.4f, 77.1f, 76.9f, 76.6f)
        kg.forEachIndexed { i, w -> store.addWeight(w, "manual", now - (kg.size - 1 - i) * 3 * DAY) }
        val sessions = listOf(
            Triple("Upper A", listOf("Barbell_Bench_Press_-_Medium_Grip" to "Barbell Bench Press", "Pullups" to "Pull-ups", "Standing_Dumbbell_Press" to "Dumbbell Press"), 0),
            Triple("Lower A", listOf("Barbell_Full_Squat" to "Barbell Squat", "Barbell_Deadlift" to "Deadlift", "Goblet_Squat" to "Goblet Squat"), 1),
            Triple("Upper B", listOf("Dumbbell_Bench_Press" to "Dumbbell Bench Press", "Pushups" to "Push-ups"), 2),
            Triple("Lower B", listOf("Barbell_Full_Squat" to "Barbell Squat", "Goblet_Squat" to "Goblet Squat"), 3)
        )
        val days = listOf(13, 11, 9, 8, 6, 4, 2, 1, 0)
        days.forEachIndexed { k, d ->
            val s = sessions[k % sessions.size]
            val bump = k * 1.5f
            val logs = s.second.map { (id, name) ->
                val base = when {
                    id.contains("Squat") && id.startsWith("Barbell") -> 80f
                    id.contains("Deadlift") -> 110f
                    id.contains("Bench") && id.startsWith("Barbell") -> 62.5f
                    id.contains("Dumbbell") -> 22f
                    else -> 0f
                }
                ExerciseLog(id, name, List(3) { SetLog(8 + (it % 2), if (base > 0f) base + bump else 0f) })
            }
            store.addWorkout(WorkoutLog(now - d * DAY - 3_600_000L, s.first, now - d * DAY - 3_600_000L, 52 * 60 + k * 37, logs))
        }
        listOf(0, 1, 2, 4, 5).forEach { d ->
            val steps = if (d == 0) 6420 else 5200 + d * 830
            store.addWalk(WalkLog(now - d * DAY - 7_200_000L, steps / 2, steps, steps * 0.76f, steps / 22))
        }
        store.addWater(1750)
    }
}
