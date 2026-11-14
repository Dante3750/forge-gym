package app.forge.gym

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.forge.gym.screens.BodyPickerScreen
import app.forge.gym.screens.ExerciseDetailScreen
import app.forge.gym.screens.LibraryScreen
import app.forge.gym.screens.WelcomeScreen
import app.forge.gym.screens.rememberLibraryUi
import app.forge.gym.ui.C
import app.forge.gym.viewer.Viewer3DScreen

/**
 * The whole app: your name once, then the exercise list. Tap an exercise to see it.
 * Routes: welcome -> home -> exercise/{id} -> viewer/{id}; home -> body (muscle picker); home -> profile.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ForgeApp(route: String? = null) {
    val app = LocalApp.current
    val nav = rememberNavController()
    val start = if (route == "welcome" || !app.store.profile.onboarded) "welcome" else "home"
    val ui = rememberLibraryUi()
    val list = rememberLazyListState()
    LaunchedEffect(route) {
        if (route == null) return@LaunchedEffect
        when {
            route.startsWith("home:") -> ui.muscle = route.removePrefix("home:")
            route == "body" -> nav.navigate("body")
            route.startsWith("exercise:") -> nav.navigate("exercise/" + route.removePrefix("exercise:"))
            route.startsWith("viewer:") -> nav.navigate("viewer/" + route.removePrefix("viewer:"))
        }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedScope provides this) {
            NavHost(
                nav, startDestination = start,
                modifier = Modifier.fillMaxSize().background(C.bg),
                enterTransition = { fadeIn(tween(260)) + slideInVertically(tween(300)) { it / 24 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(240)) },
                popExitTransition = { fadeOut(tween(200)) + slideOutVertically(tween(260)) { it / 24 } }
            ) {
                composable("welcome") {
                    WelcomeScreen(onBack = null) { nav.navigate("home") { popUpTo("welcome") { inclusive = true } } }
                }
                composable("profile") {
                    WelcomeScreen(onBack = { nav.popBackStack() }) { nav.popBackStack() }
                }
                composable("home") {
                    CompositionLocalProvider(LocalAnimScope provides this) {
                        LibraryScreen(
                            ui, list,
                            onOpen = { nav.navigate("exercise/$it") },
                            onBody = { nav.navigate("body") },
                            onProfile = { nav.navigate("profile") }
                        )
                    }
                }
                composable("body") {
                    BodyPickerScreen(onBack = { nav.popBackStack() }) { muscle ->
                        ui.query = ""
                        ui.muscle = muscle
                        nav.popBackStack()
                    }
                }
                composable("exercise/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                    CompositionLocalProvider(LocalAnimScope provides this) {
                        ExerciseDetailScreen(e.arguments?.getString("id").orEmpty(), { nav.popBackStack() }, { nav.navigate("viewer/$it") })
                    }
                }
                composable("viewer/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                    Viewer3DScreen(e.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
                }
            }
        }
    }
}
