package app.forge.gym.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.forge.gym.LocalApp
import app.forge.gym.data.Gender
import app.forge.gym.data.formatWeight
import app.forge.gym.data.prettyMuscle
import app.forge.gym.data.unitLabel
import app.forge.gym.sharedKey
import app.forge.gym.ui.C
import app.forge.gym.ui.CutSm
import app.forge.gym.ui.CutXs
import app.forge.gym.ui.Empty
import app.forge.gym.ui.ExerciseImage
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.IconBox
import app.forge.gym.ui.SectionLabel
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt
import app.forge.gym.ui.pressable
import app.forge.gym.viewer.Trainer
import kotlin.math.max

@Composable
private fun MiniToggle(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .pressable(onClick = onClick)
            .clip(CutXs)
            .background(if (on) C.volt else C.bg.copy(alpha = 0.72f))
            .border(1.dp, if (on) C.volt else C.line, CutXs)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) { Txt(text.uppercase(), T.label, if (on) C.bg else C.text) }
}

/** One exercise: the moving demo, which muscles it works, and how to do it. */
@Composable
fun ExerciseDetailScreen(id: String, onBack: () -> Unit, onOpen3d: (String) -> Unit = {}) {
    val app = LocalApp.current
    val e = app.exercise(id)
    if (e == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (app.exercises.isEmpty()) Empty("Loading", "Getting the exercises ready.")
                else Empty("Exercise not found", "It may have been renamed.")
                ForgeButton("Back", onBack, filled = false)
            }
        }
        return
    }
    val pager = rememberPagerState { max(e.images.size, 1) }
    val motion = app.motionFor(e.id)
    var female by rememberSaveable { mutableStateOf(app.store.profile.gender == Gender.FEMALE) }
    var slow by rememberSaveable { mutableStateOf(false) }
    var angle by rememberSaveable { mutableIntStateOf(0) }
    val useKg = app.store.profile.useKg
    val history = app.store.historyFor(e.id)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(430.dp).sharedKey("ex-img-${e.id}")) {
                if (motion != null) {
                    Trainer(motion, female, Modifier.fillMaxSize(), angle = angle, speed = if (slow) 0.45f else 1f) {
                        ExerciseImage(e.imageUrl(0), Modifier.fillMaxSize(), scrim = true)
                    }
                } else {
                    HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                        ExerciseImage(e.imageUrl(page), Modifier.fillMaxSize(), scrim = true)
                    }
                }
                Box(Modifier.padding(start = 20.dp, top = statusTop() + 12.dp)) {
                    IconBox(Icons.AutoMirrored.Rounded.ArrowBack, onBack)
                }
                if (motion != null) {
                    Row(
                        Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MiniToggle(if (female) "Woman" else "Man", false) { female = !female }
                        MiniToggle("Slow", slow) { slow = !slow }
                        MiniToggle("Other side", angle == 1) { angle = 1 - angle }
                    }
                } else if (e.images.size > 1) {
                    Row(
                        Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in e.images.indices) {
                            Box(
                                Modifier
                                    .size(width = if (pager.currentPage == i) 28.dp else 12.dp, height = 4.dp)
                                    .background(if (pager.currentPage == i) C.volt else C.dim.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = 18.dp)) {
                Txt(e.name.uppercase(), T.h1, maxLines = 3)
                Spacer(Modifier.height(6.dp))
                Txt(equipmentLabel(e) + " · " + e.levelLabel, T.body, C.dim)
            }
        }
        if (motion != null) {
            item {
                ForgeButton(
                    "Turn & zoom in 3D", { onOpen3d(e.id) },
                    Modifier.padding(horizontal = 20.dp).padding(top = 16.dp).fillMaxWidth(),
                    icon = Icons.Rounded.ViewInAr
                )
            }
        }
        if (history.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp).padding(top = 26.dp)) {
                    SectionLabel("Your records")
                    Spacer(Modifier.height(12.dp))
                    val work = history.flatMap { it.second.sets }.filter { !it.warm }
                    val heavy = work.maxOfOrNull { it.weightKg } ?: 0f
                    val oneRm = work.maxOfOrNull { it.weightKg * (1f + it.reps / 30f) } ?: 0f
                    val reps = work.maxOfOrNull { it.reps } ?: 0
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        if (heavy > 0f) Mini("Heaviest", "${formatWeight(heavy, useKg)} ${unitLabel(useKg)}")
                        if (heavy > 0f) Mini("Best 1-rep max", "${formatWeight(oneRm, useKg)} ${unitLabel(useKg)}")
                        Mini("Most reps", "$reps")
                        Mini("Times done", "${history.size}")
                    }
                    Spacer(Modifier.height(16.dp))
                    history.take(5).forEach { (w, log) ->
                        Row(Modifier.padding(bottom = 8.dp)) {
                            Txt(workoutDate(w), T.small, C.dim, Modifier.width(96.dp), maxLines = 1)
                            Txt(
                                log.sets.joinToString("   ") { if (it.weightKg > 0f) "${formatWeight(it.weightKg, useKg)}×${it.reps}" else "${it.reps}" },
                                T.bodyM, C.text, Modifier.weight(1f), maxLines = 2
                            )
                        }
                    }
                }
            }
        }
        if (e.primary.isNotEmpty() || e.secondary.isNotEmpty()) {
            item {
                Column(Modifier.padding(top = 26.dp)) {
                    SectionLabel("Muscles worked", Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(12.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(e.primary) { m ->
                            Box(Modifier.clip(CutSm).background(C.volt).padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Txt(prettyMuscle(m).uppercase(), T.label, C.bg)
                            }
                        }
                        items(e.secondary) { m ->
                            Box(Modifier.clip(CutSm).border(1.dp, C.line, CutSm).padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Txt(prettyMuscle(m).uppercase(), T.label, C.dim)
                            }
                        }
                    }
                }
            }
        }
        if (e.steps.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp).padding(top = 28.dp)) {
                    SectionLabel("How to do it")
                    Spacer(Modifier.height(14.dp))
                    e.steps.forEachIndexed { i, step ->
                        Row(Modifier.padding(bottom = 16.dp)) {
                            Txt("${i + 1}", T.h2, C.volt, Modifier.width(32.dp))
                            Txt(step, T.body, C.text, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
