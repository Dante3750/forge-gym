package app.forge.gym

import android.content.Intent
import android.content.pm.ApplicationInfo
import app.forge.gym.data.Gender

/**
 * Debug-build hooks used by CI to take screenshots:
 *   adb shell am start -n app.forge.gym/.MainActivity --ez forge_demo true --es forge_route home
 * Routes: home, home:<muscle>, body, exercise:<id>, viewer:<id>, welcome.
 */
object Demo {
    fun isDebuggable(app: AppState) = (app.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun route(intent: Intent?, app: AppState): String? {
        if (intent == null || !isDebuggable(app)) return null
        if (intent.getBooleanExtra("forge_demo", false) && !app.store.profile.onboarded) {
            app.store.updateProfile(app.store.profile.copy(name = "Aarav", gender = Gender.MALE, onboarded = true))
        }
        return intent.getStringExtra("forge_route")
    }
}
