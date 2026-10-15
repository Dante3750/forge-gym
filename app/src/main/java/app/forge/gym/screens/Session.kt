package app.forge.gym.screens

import app.forge.gym.data.Gender

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.forge.gym.LocalApp
import app.forge.gym.buzz
import app.forge.gym.data.Exercise
import app.forge.gym.data.ExerciseLog
import app.forge.gym.data.PlanBuilder
import app.forge.gym.data.PlanDay
import app.forge.gym.data.PlanExercise
import app.forge.gym.data.SetLog
import app.forge.gym.data.WorkoutLog
import app.forge.gym.data.formatClock
import app.forge.gym.data.formatWeight
import app.forge.gym.data.unitLabel
import app.forge.gym.ui.C
import app.forge.gym.ui.Chip
import app.forge.gym.ui.CutSm
import app.forge.gym.ui.CutXs
import app.forge.gym.ui.Empty
import app.forge.gym.ui.ExerciseLoop
import app.forge.gym.ui.MotionPlayer
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.IconBox
import app.forge.gym.ui.Panel
import app.forge.gym.ui.Ring
import app.forge.gym.ui.Stepper
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt
import app.forge.gym.ui.staggerIn
import kotlinx.coroutines.delay
import kotlin.math.max

private class SessionEx(val ex: Exercise, val plan: PlanExercise) {
    val logged = mutableStateListOf<SetLog>()
}

@Composable
fun SessionScreen(dayIndex: Int, onClose: () -> Unit) {
    val app = LocalApp.current
    val day = remember(dayIndex, app.exercises) { app.plan().getOrNull(dayIndex) }
    val items = remember(day) {
        day?.items?.mapNotNull { pe -> app.exercise(pe.exerciseId)?.let { SessionEx(it, pe) } } ?: emptyList()
    }
    if (day == null || items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Empty("No workout here", "Pick a day from your plan.")
                ForgeButton("Back", onClose, filled = false)
            }
        }
    } else {
        SessionBody(day, items, onClose)
    }
}

@Composable
private fun SessionBody(day: PlanDay, items: List<SessionEx>, onClose: () -> Unit) {
    val app = LocalApp.current
    val store = app.store
    val ctx = LocalContext.current
    val view = LocalView.current
    val useKg = store.profile.useKg

    val startedAt = remember { System.currentTimeMillis() }
    var now by remember { mutableLongStateOf(startedAt) }
    var cur by remember { mutableIntStateOf(0) }
    var restLeft by remember { mutableIntStateOf(0) }
    var restTotal by remember { mutableIntStateOf(60) }
    var restKey by remember { mutableIntStateOf(0) }
    var showQuit by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    LaunchedEffect(restKey) {
        if (restLeft > 0) {
            while (restLeft > 0) {
                delay(1000)
                restLeft -= 1
            }
            buzz(ctx)
        }
    }

    val elapsed = ((now - startedAt) / 1000).toInt()
    val totalSets = items.sumOf { it.logged.size }

    fun save() {
        if (saved) return
        saved = true
        val logs = items.filter { it.logged.isNotEmpty() }.map { ExerciseLog(it.ex.id, it.ex.name, it.logged.toList()) }
        if (logs.isNotEmpty()) {
            store.addWorkout(WorkoutLog(startedAt, day.title, startedAt, elapsed, logs))
        }
    }

    BackHandler(true) {
        if (summary) {
            save()
            onClose()
        } else {
            showQuit = true
        }
    }

    // Personal records are worked out the moment the summary opens, before this session is saved.
    val prs = remember(summary) {
        if (!summary) emptyList<String>()
        else {
            val best = store.bestOneRm()
            items.mapNotNull { si ->
                val weighted = si.logged.filter { it.weightKg > 0f }
                if (weighted.isEmpty()) null
                else {
                    val est = weighted.maxOf { it.weightKg * (1f + it.reps / 30f) }
                    if (est > (best[si.ex.id] ?: 0f) + 0.01f) si.ex.name else null
                }
            }
        }
    }

    val safeCur = cur.coerceIn(0, items.lastIndex)
    val item = items[safeCur]
    var reps by remember(safeCur) { mutableIntStateOf(PlanBuilder.firstNumber(item.plan.reps)) }
    var weight by remember(safeCur) { mutableFloatStateOf(store.lastLogFor(item.ex.id)?.weightKg ?: 0f) }
    val stepKg = if (useKg) 2.5f else 5f / 2.20462f

    fun startRest(sec: Int) {
        if (sec <= 0) return
        restTotal = sec
        restLeft = sec
        restKey += 1
    }

    fun logSet() {
        item.logged.add(SetLog(reps, weight))
        buzz(ctx, false)
        val done = item.logged.size >= item.plan.sets
        if (done) {
            if (safeCur < items.lastIndex) {
                startRest(item.plan.restSec)
                cur = safeCur + 1
            } else {
                summary = true
            }
        } else {
            startRest(item.plan.restSec)
        }
    }

    Box(Modifier.fillMaxSize().background(C.bg)) {
        if (!summary) {
            Column(Modifier.fillMaxSize().padding(top = statusTop() + 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconBox(Icons.Rounded.Close, { showQuit = true })
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Txt(day.title.uppercase(), T.label, C.dim)
                        Txt(formatClock(elapsed), T.h1)
                    }
                    Chip("Finish", false, { summary = true })
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.forEachIndexed { i, si ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    if (si.logged.size >= si.plan.sets) C.volt
                                    else if (i == safeCur) C.dim else C.line
                                )
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    AnimatedContent(
                        targetState = safeCur,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> w } + fadeIn(tween(240))) togetherWith
                                    (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> -w } + fadeOut(tween(160)))
                            } else {
                                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> -w } + fadeIn(tween(240))) togetherWith
                                    (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> w } + fadeOut(tween(160)))
                            }
                        },
                        label = "exercise"
                    ) { c ->
                        val si = items[c.coerceIn(0, items.lastIndex)]
                        Box(Modifier.fillMaxWidth().height(300.dp)) {
                            val mv = LocalApp.current.motionFor(si.ex.id)
                            if (mv != null) {
                                MotionPlayer(mv, LocalApp.current.store.profile.gender == Gender.FEMALE, Modifier.fillMaxSize()) {
                                    ExerciseLoop(si.ex, Modifier.fillMaxSize())
                                }
                            } else ExerciseLoop(si.ex, Modifier.fillMaxSize())
                            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 14.dp)) {
                                Txt(si.ex.name.uppercase(), T.h1, maxLines = 2)
                                Txt(
                                    "${si.plan.sets} × ${si.plan.reps}" + if (si.plan.restSec > 0) " · REST ${si.plan.restSec}s" else "",
                                    T.bodyM, C.volt
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 0 until max(item.plan.sets, item.logged.size)) {
                            val log = item.logged.getOrNull(i)
                            val isCurrent = log == null && i == item.logged.size
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(CutXs)
                                    .background(if (log != null) C.volt else C.surface)
                                    .border(1.dp, if (isCurrent) C.volt else if (log != null) C.volt else C.line, CutXs),
                                contentAlignment = Alignment.Center
                            ) {
                                if (log != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Txt("${log.reps}", T.h3, C.bg)
                                        if (log.weightKg > 0f) Txt(formatWeight(log.weightKg, useKg), T.label, C.bg)
                                    }
                                } else {
                                    Txt("${i + 1}", T.h3, if (isCurrent) C.text else C.faint)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Panel(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Txt("REPS", T.label, C.dim)
                            Spacer(Modifier.height(8.dp))
                            Stepper(
                                reps.toString(), "",
                                { if (reps > 1) reps -= 1 },
                                { reps += 1 }
                            )
                            Spacer(Modifier.height(18.dp))
                            Txt(
                                if (item.ex.isBodyweight) "ADDED WEIGHT (OPTIONAL)" else "WEIGHT",
                                T.label, C.dim
                            )
                            Spacer(Modifier.height(8.dp))
                            Stepper(
                                formatWeight(weight, useKg), unitLabel(useKg),
                                { weight = max(0f, weight - stepKg) },
                                { weight += stepKg }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp).navigationBarsPadding()) {
                    ForgeButton(
                        if (item.logged.size >= item.plan.sets) "Add extra set" else "Log set ${item.logged.size + 1}",
                        { logSet() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Check
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ForgeButton("Back", { if (safeCur > 0) cur = safeCur - 1 }, Modifier.weight(1f), filled = false, enabled = safeCur > 0)
                        ForgeButton(
                            "Skip", { if (safeCur < items.lastIndex) cur = safeCur + 1 else summary = true },
                            Modifier.weight(1f), filled = false, icon = Icons.Rounded.SkipNext
                        )
                    }
                }
            }
        } else {
            SummaryUi(
                title = day.title, seconds = elapsed,
                volumeKg = items.sumOf { si -> si.logged.sumOf { (it.reps * it.weightKg).toDouble() } }.toFloat(),
                sets = totalSets, prs = prs, useKg = useKg,
                onSave = { save(); onClose() },
                onDiscard = onClose
            )
        }

        AnimatedVisibility(
            visible = restLeft > 0 && !summary,
            enter = fadeIn(tween(200)) + scaleIn(tween(320), initialScale = 0.92f),
            exit = fadeOut(tween(200))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(C.bg.copy(alpha = 0.97f))
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Txt("REST", T.label, C.dim)
                    Spacer(Modifier.height(16.dp))
                    Ring(restLeft.toFloat() / max(restTotal, 1), 270.dp, 10.dp) {
                        Txt(formatClock(restLeft), T.numXL)
                    }
                    Spacer(Modifier.height(28.dp))
                    Txt("UP NEXT", T.label, C.dim)
                    Spacer(Modifier.height(4.dp))
                    Txt(
                        if (item.logged.size in 1 until item.plan.sets) "Set ${item.logged.size + 1} · ${item.ex.name}" else item.ex.name,
                        T.h2, maxLines = 2
                    )
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ForgeButton("−15s", {
                            if (restLeft <= 15) {
                                restLeft = 0
                                restKey += 1
                            } else {
                                restLeft -= 15
                            }
                        }, filled = false)
                        ForgeButton("+15s", { restLeft += 15; restTotal += 15 }, filled = false)
                        ForgeButton("Skip", { restLeft = 0; restKey += 1 })
                    }
                }
            }
        }
    }

    if (showQuit) {
        AlertDialog(
            onDismissRequest = { showQuit = false },
            containerColor = C.surface,
            titleContentColor = C.text,
            textContentColor = C.dim,
            title = { Text("Quit this workout?", style = T.h2) },
            text = {
                Text(
                    if (totalSets > 0) "You've logged $totalSets sets. Finish to save them, or quit and lose them."
                    else "Nothing's logged yet.",
                    style = T.body
                )
            },
            confirmButton = {
                TextButton(onClick = { showQuit = false; if (totalSets > 0) summary = true else onClose() }) {
                    Text(if (totalSets > 0) "FINISH & SAVE" else "QUIT", style = T.label, color = C.volt)
                }
            },
            dismissButton = {
                Row {
                    if (totalSets > 0) {
                        TextButton(onClick = { showQuit = false; onClose() }) {
                            Text("QUIT", style = T.label, color = C.danger)
                        }
                    }
                    TextButton(onClick = { showQuit = false }) {
                        Text("KEEP GOING", style = T.label, color = C.dim)
                    }
                }
            }
        )
    }
}

@Composable
private fun SummaryUi(
    title: String,
    seconds: Int,
    volumeKg: Float,
    sets: Int,
    prs: List<String>,
    useKg: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = statusTop() + 24.dp, bottom = 16.dp)
            .navigationBarsPadding()
    ) {
        Column(Modifier.staggerIn(0)) {
            Txt(title.uppercase(), T.label, C.volt)
            Txt(if (sets > 0) "DONE." else "NOTHING LOGGED.", T.numXL.copy(fontSize = 72.sp), maxLines = 2)
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().staggerIn(1), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStat("Time", formatClock(seconds), "", Modifier.weight(1f))
            SummaryStat("Sets", sets.toString(), "", Modifier.weight(1f))
            SummaryStat("Volume", formatWeight(volumeKg, useKg), unitLabel(useKg), Modifier.weight(1f))
        }
        if (prs.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Panel(Modifier.fillMaxWidth().staggerIn(2), accent = true) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = C.bg, modifier = Modifier.height(22.dp).width(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Txt(if (prs.size == 1) "NEW PERSONAL BEST" else "${prs.size} NEW PERSONAL BESTS", T.label, C.bg)
                    }
                    Spacer(Modifier.height(8.dp))
                    prs.forEach { Txt(it, T.h3, C.bg) }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        if (sets > 0) {
            ForgeButton("Save workout", onSave, Modifier.fillMaxWidth(), icon = Icons.Rounded.Check)
            Spacer(Modifier.height(10.dp))
            ForgeButton("Discard", onDiscard, Modifier.fillMaxWidth(), filled = false, danger = true)
        } else {
            ForgeButton("Close", onDiscard, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, unit: String, modifier: Modifier) {
    Panel(modifier) {
        Column(Modifier.padding(14.dp)) {
            Txt(label.uppercase(), T.label, C.dim)
            Spacer(Modifier.height(6.dp))
            Txt(value, T.numM, maxLines = 1)
            if (unit.isNotEmpty()) Txt(unit.uppercase(), T.label, C.dim)
        }
    }
}
