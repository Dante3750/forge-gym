package app.forge.gym

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.forge.gym.data.RoutineItem
import app.forge.gym.data.formatClock
import app.forge.gym.screens.BodyPickerScreen
import app.forge.gym.screens.ExerciseDetailScreen
import app.forge.gym.screens.LibraryScreen
import app.forge.gym.screens.LibraryUi
import app.forge.gym.screens.LiveWorkoutScreen
import app.forge.gym.screens.ProfileScreen
import app.forge.gym.screens.RoutineDraft
import app.forge.gym.screens.RoutineEditorScreen
import app.forge.gym.screens.WelcomeScreen
import app.forge.gym.screens.WorkoutDetailScreen
import app.forge.gym.screens.WorkoutHomeScreen
import app.forge.gym.screens.rememberLibraryUi
import app.forge.gym.ui.C
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt
import app.forge.gym.ui.pressable
import app.forge.gym.viewer.Viewer3DScreen
import kotlinx.coroutines.delay

/**
 * Three tabs: Workout (start or continue a workout, routines), Exercises (the library) and Profile (stats, history).
 * Routes: welcome, main, live (the workout in progress), routine (editor), pick/{for} (add an exercise to the
 * workout or to the routine), workout/{id}, exercise/{id}, viewer/{id}, body, me.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ForgeApp(route: String? = null) {
    val app = LocalApp.current
    val nav = rememberNavController()
    val start = if (route == "welcome" || !app.store.profile.onboarded) "welcome" else "main"
    var tab by rememberSaveable { mutableIntStateOf(route?.takeIf { it.startsWith("tab:") }?.removePrefix("tab:")?.toIntOrNull() ?: 0) }
    val ui = rememberLibraryUi()
    val list = rememberLazyListState()
    val draft = remember { RoutineDraft() }
    LaunchedEffect(route) {
        if (route == null) return@LaunchedEffect
        when {
            route.startsWith("home:") -> { tab = 1; ui.muscle = route.removePrefix("home:") }
            route == "body" -> nav.navigate("body")
            route == "live" -> {
                while (app.exercises.isEmpty()) delay(100)
                if (app.live == null) app.startWorkout(app.store.routines.firstOrNull())
                nav.navigate("live")
            }
            route == "routine" -> {
                while (app.exercises.isEmpty()) delay(100)
                draft.load(app.store.routines.firstOrNull())
                nav.navigate("routine")
            }
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
                    WelcomeScreen(onBack = null) { nav.navigate("main") { popUpTo("welcome") { inclusive = true } } }
                }
                composable("me") {
                    WelcomeScreen(onBack = { nav.popBackStack() }) { nav.popBackStack() }
                }
                composable("main") {
                    CompositionLocalProvider(LocalAnimScope provides this) {
                        MainShell(
                            tab, { tab = it }, ui, list,
                            onOpen = { nav.navigate("exercise/$it") },
                            onBody = { nav.navigate("body") },
                            onLive = { nav.navigate("live") },
                            onEditRoutine = { id ->
                                draft.load(app.store.routines.firstOrNull { it.id == id })
                                nav.navigate("routine")
                            },
                            onOpenWorkout = { nav.navigate("workout/$it") },
                            onMe = { nav.navigate("me") }
                        )
                    }
                }
                composable("live") {
                    LiveWorkoutScreen(
                        onBack = { nav.popBackStack() },
                        onAdd = { nav.navigate("pick/live") },
                        onOpen = { nav.navigate("exercise/$it") },
                        onFinished = { id ->
                            tab = 2
                            nav.navigate("workout/$id") { popUpTo("main") }
                        }
                    )
                }
                composable("routine") {
                    RoutineEditorScreen(draft, onBack = { nav.popBackStack() }, onAdd = { nav.navigate("pick/routine") })
                }
                composable("pick/{for}", arguments = listOf(navArgument("for") { type = NavType.StringType })) { e ->
                    val target = e.arguments?.getString("for").orEmpty()
                    val pickUi = remember { LibraryUi() }
                    val pickList = rememberLazyListState()
                    LibraryScreen(
                        pickUi, pickList,
                        onOpen = { id ->
                            val ex = app.exercise(id)
                            if (ex != null) {
                                if (target == "live") app.addToLive(ex)
                                else draft.items.add(RoutineItem(ex.id, ex.name, 3))
                            }
                            nav.popBackStack()
                        },
                        onBody = {},
                        onBack = { nav.popBackStack() }
                    )
                }
                composable("workout/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    WorkoutDetailScreen(e.arguments?.getLong("id") ?: 0L, { nav.popBackStack() }, { nav.navigate("exercise/$it") })
                }
                composable("body") {
                    BodyPickerScreen(onBack = { nav.popBackStack() }) { muscle ->
                        ui.query = ""
                        ui.muscle = muscle
                        tab = 1
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

private data class TabDef(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabDef("Workout", Icons.Rounded.FitnessCenter),
    TabDef("Exercises", Icons.Rounded.Search),
    TabDef("Profile", Icons.Rounded.Person)
)

@Composable
private fun MainShell(
    tab: Int, onTab: (Int) -> Unit, ui: LibraryUi, list: LazyListState,
    onOpen: (String) -> Unit, onBody: () -> Unit, onLive: () -> Unit,
    onEditRoutine: (Long) -> Unit, onOpenWorkout: (Long) -> Unit, onMe: () -> Unit
) {
    val live = LocalApp.current.live
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                label = "tabs"
            ) { t ->
                when (t) {
                    0 -> WorkoutHomeScreen(onLive, onEditRoutine)
                    1 -> LibraryScreen(ui, list, onOpen, onBody)
                    else -> ProfileScreen(onOpenWorkout, onMe)
                }
            }
        }
        // a workout in progress stays one tap away on every tab
        if (live != null) {
            val now by produceState(System.currentTimeMillis()) {
                while (true) { value = System.currentTimeMillis(); delay(1000) }
            }
            Row(
                Modifier.fillMaxWidth().pressable(onClick = onLive).background(C.volt).padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Txt("WORKOUT IN PROGRESS", T.label, C.bg, Modifier.weight(1f), maxLines = 1)
                Txt(formatClock(((now - live.startedAt) / 1000L).toInt().coerceAtLeast(0)), T.h3, C.bg)
            }
        }
        Row(
            Modifier.fillMaxWidth().background(C.surface).navigationBarsPadding().height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { i, d ->
                val sel = i == tab
                val c by animateColorAsState(if (sel) C.volt else C.dim, tween(200), label = "tabc")
                Column(
                    Modifier.weight(1f).fillMaxSize().pressable { onTab(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(d.icon, null, tint = c, modifier = Modifier.size(24.dp))
                    Txt(d.label.uppercase(), T.label, c, Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}
