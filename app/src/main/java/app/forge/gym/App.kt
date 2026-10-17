package app.forge.gym

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import app.forge.gym.screens.ExerciseDetailScreen
import app.forge.gym.screens.ExploreScreen
import app.forge.gym.screens.HomeScreen
import app.forge.gym.screens.OnboardingScreen
import app.forge.gym.screens.PlanScreen
import app.forge.gym.screens.ProgressScreen
import app.forge.gym.screens.SessionScreen
import app.forge.gym.screens.SettingsScreen
import app.forge.gym.screens.WalkScreen
import app.forge.gym.screens.rememberExploreUi
import app.forge.gym.ui.C
import app.forge.gym.ui.Txt
import app.forge.gym.ui.T
import app.forge.gym.ui.pressable

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ForgeApp(route: String? = null) {
    val app = LocalApp.current
    val nav = rememberNavController()
    val start = if (route == "onboarding") "onboarding" else if (app.store.profile.onboarded) "main" else "onboarding"
    var tab by rememberSaveable { mutableIntStateOf(route?.takeIf { it.startsWith("tab:") }?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0) }
    val exploreUi = rememberExploreUi()
    androidx.compose.runtime.LaunchedEffect(route) {
        if (route == null) return@LaunchedEffect
        if (route.endsWith(":body")) exploreUi.seg = 1
        when {
            route.startsWith("exercise:") -> nav.navigate("exercise/" + route.removePrefix("exercise:"))
            route.startsWith("viewer:") -> nav.navigate("viewer/" + route.removePrefix("viewer:"))
            route == "plan" -> nav.navigate("plan")
            route.startsWith("session:") -> nav.navigate("session/" + route.removePrefix("session:"))
            route == "settings" -> nav.navigate("settings")
        }
    }
    val exploreList = rememberLazyListState()

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
                composable("onboarding") {
                    CompositionLocalProvider(LocalAnimScope provides this) {
                        OnboardingScreen {
                            tab = 0
                            nav.navigate("main") { popUpTo("onboarding") { inclusive = true } }
                        }
                    }
                }
                composable("main") {
                    CompositionLocalProvider(LocalAnimScope provides this) {
                        MainShell(
                            tab, { tab = it }, exploreUi, exploreList,
                            onOpen = { nav.navigate("exercise/$it") },
                            onStart = { nav.navigate("session/$it") },
                            onPlan = { nav.navigate("plan") },
                            onSettings = { nav.navigate("settings") }
                        )
                    }
                }
                composable("exercise/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                    CompositionLocalProvider(LocalAnimScope provides this) {
                        ExerciseDetailScreen(e.arguments?.getString("id").orEmpty(), { nav.popBackStack() }, { nav.navigate("exercise/$it") }, { nav.navigate("viewer/$it") })
                    }
                }
                composable("viewer/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                    app.forge.gym.viewer.Viewer3DScreen(e.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
                }
                composable("plan") {
                    CompositionLocalProvider(LocalAnimScope provides this) {
                        PlanScreen({ nav.popBackStack() }, { nav.navigate("session/$it") }, { nav.navigate("exercise/$it") })
                    }
                }
                composable("session/{day}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { e ->
                    CompositionLocalProvider(LocalAnimScope provides this) {
                        SessionScreen(e.arguments?.getInt("day") ?: 0) { nav.popBackStack() }
                    }
                }
                composable("settings") {
                    CompositionLocalProvider(LocalAnimScope provides this) {
                        SettingsScreen { nav.popBackStack() }
                    }
                }
            }
        }
    }
}

private data class TabDef(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabDef("Home", Icons.Rounded.Home),
    TabDef("Explore", Icons.Rounded.Search),
    TabDef("Walk", Icons.Rounded.DirectionsWalk),
    TabDef("Progress", Icons.Rounded.BarChart)
)

@Composable
private fun MainShell(
    tab: Int,
    onTab: (Int) -> Unit,
    exploreUi: app.forge.gym.screens.ExploreUi,
    exploreList: androidx.compose.foundation.lazy.LazyListState,
    onOpen: (String) -> Unit,
    onStart: (Int) -> Unit,
    onPlan: () -> Unit,
    onSettings: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                label = "tabs"
            ) { t ->
                when (t) {
                    0 -> HomeScreen(
                        onStart = onStart, onPlan = onPlan,
                        onBody = { exploreUi.seg = 1; onTab(1) },
                        onWalk = { onTab(2) }, onProgress = { onTab(3) }, onSettings = onSettings
                    )
                    1 -> ExploreScreen(exploreUi, exploreList, onOpen)
                    2 -> WalkScreen()
                    else -> ProgressScreen(onSettings)
                }
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
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Icon(d.icon, null, tint = c, modifier = Modifier.size(24.dp))
                    Txt(d.label.uppercase(), T.label, c, Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}
