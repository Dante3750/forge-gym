package app.forge.gym.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.forge.gym.LocalApp
import app.forge.gym.data.Gender
import app.forge.gym.data.Goal
import app.forge.gym.data.Level
import app.forge.gym.data.Place
import app.forge.gym.ui.C
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.Segmented
import app.forge.gym.ui.Stepper
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val store = LocalApp.current.store
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableIntStateOf(0) }
    var level by rememberSaveable { mutableIntStateOf(0) }
    var goal by rememberSaveable { mutableIntStateOf(0) }
    var place by rememberSaveable { mutableIntStateOf(0) }
    var days by rememberSaveable { mutableIntStateOf(3) }
    var height by rememberSaveable { mutableIntStateOf(172) }
    var weight by rememberSaveable { mutableFloatStateOf(70f) }
    var goalW by rememberSaveable { mutableFloatStateOf(0f) }
    var useKg by rememberSaveable { mutableStateOf(true) }
    val last = 3

    Column(Modifier.fillMaxSize().background(C.bg).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(statusTop() + 20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0..last) {
                Box(Modifier.weight(1f).height(4.dp).background(if (i <= step) C.volt else C.line))
            }
        }
        Spacer(Modifier.height(24.dp))
        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val fwd = targetState > initialState
                (slideInHorizontally(tween(320)) { if (fwd) it / 3 else -it / 3 } + fadeIn(tween(320))) togetherWith
                    (slideOutHorizontally(tween(260)) { if (fwd) -it / 3 else it / 3 } + fadeOut(tween(200)))
            },
            label = "onb"
        ) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (s) {
                    0 -> {
                        Txt("FORGE", T.hero, C.volt)
                        Txt("Let's build your plan.", T.h2)
                        Txt("A few quick questions. Everything stays on your phone.", T.body, C.dim)
                        Spacer(Modifier.height(8.dp))
                        NameField(name, { name = it })
                        Segmented(Gender.values().map { it.label }, gender, { gender = it })
                    }
                    1 -> {
                        Txt("YOUR LEVEL", T.h1)
                        Level.values().forEachIndexed { i, l -> OptionRow(l.label, l.blurb, level == i) { level = i } }
                    }
                    2 -> {
                        Txt("YOUR GOAL", T.h1)
                        Goal.values().forEachIndexed { i, g -> OptionRow(g.label, g.blurb, goal == i) { goal = i } }
                        Spacer(Modifier.height(4.dp))
                        Segmented(Place.values().map { it.label }, place, { place = it })
                        Stepper("$days", "days / week", { days = (days - 1).coerceAtLeast(2) }, { days = (days + 1).coerceAtMost(6) })
                    }
                    else -> {
                        Txt("YOUR BODY", T.h1)
                        Txt("Used for BMI and your weight chart. You can change it later.", T.body, C.dim)
                        Segmented(listOf("kg", "lb"), if (useKg) 0 else 1, { useKg = it == 0 })
                        Stepper("$height", "cm tall", { height = (height - 1).coerceAtLeast(120) }, { height = (height + 1).coerceAtMost(220) })
                        val f = if (useKg) 1f else 2.20462f
                        Stepper("%.1f".format(weight * f), if (useKg) "kg now" else "lb now",
                            { weight = (weight - 0.5f).coerceAtLeast(30f) }, { weight = (weight + 0.5f).coerceAtMost(250f) })
                        Stepper(if (goalW <= 0f) "skip" else "%.1f".format(goalW * f), if (useKg) "kg goal" else "lb goal",
                            { goalW = if (goalW <= 0f) weight else (goalW - 0.5f).coerceAtLeast(30f) },
                            { goalW = if (goalW <= 0f) weight else (goalW + 0.5f).coerceAtMost(250f) })
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) ForgeButton("Back", { step-- }, Modifier.weight(1f), filled = false)
            ForgeButton(if (step == last) "Build my plan" else "Next", {
                if (step < last) step++ else {
                    store.updateProfile(
                        store.profile.copy(
                            name = name.trim(), gender = Gender.values()[gender], level = Level.values()[level],
                            goal = Goal.values()[goal], place = Place.values()[place], daysPerWeek = days,
                            heightCm = height, goalWeightKg = goalW, useKg = useKg, onboarded = true
                        )
                    )
                    store.addWeight(weight)
                    onDone()
                }
            }, Modifier.weight(2f))
        }
    }
}
