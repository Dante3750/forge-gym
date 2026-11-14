package app.forge.gym.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.forge.gym.LocalApp
import app.forge.gym.data.LiveExercise
import app.forge.gym.data.LiveSet
import app.forge.gym.data.LiveWorkout
import app.forge.gym.data.SetLog
import app.forge.gym.data.formatClock
import app.forge.gym.data.formatWeight
import app.forge.gym.data.parseNumber
import app.forge.gym.data.unitLabel
import app.forge.gym.ui.C
import app.forge.gym.ui.Chip
import app.forge.gym.ui.CutXs
import app.forge.gym.ui.ExerciseImage
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.IconBox
import app.forge.gym.ui.Panel
import app.forge.gym.ui.SectionLabel
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt
import app.forge.gym.ui.pressable
import kotlinx.coroutines.delay

/** The workout you are doing now: tick sets off as you go, with last time's numbers beside each one. */
@Composable
fun LiveWorkoutScreen(onBack: () -> Unit, onAdd: () -> Unit, onOpen: (String) -> Unit, onFinished: (Long) -> Unit) {
    val app = LocalApp.current
    val w = app.live
    if (w == null) {
        Box(Modifier.fillMaxSize())
        return
    }
    val useKg = app.store.profile.useKg
    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(500) }
    }
    var confirmDiscard by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf("") }
    val resting = w.restEndsAt > now
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(resting) {
        if (!resting && w.restEndsAt != 0L) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            w.restEndsAt = 0L
        }
    }
    LaunchedEffect(w) {
        while (true) { delay(4000); app.saveLive() }
    }
    DisposableEffect(w) { onDispose { app.saveLive() } }

    val volume = w.exercises.sumOf { e -> e.sets.filter { it.done && !it.warm }.sumOf { (parseNumber(it.weight) * parseNumber(it.reps)).toDouble() } }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = statusTop() + 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBox(Icons.Rounded.KeyboardArrowDown, onBack)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Txt(formatClock(((now - w.startedAt) / 1000L).toInt().coerceAtLeast(0)), T.numM, C.volt)
                Txt("${w.doneSets} sets · ${volume.toInt()} ${unitLabel(useKg)}", T.small, C.dim, maxLines = 1)
            }
            ForgeButton("Finish", {
                val id = app.finishLive()
                if (id == null) hint = "Tick at least one set before you finish." else onFinished(id)
            })
        }
        if (hint.isNotEmpty()) Txt(hint, T.small, C.ember, Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp))
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { TitleField(w.title, { w.title = it }, "Workout name") }
            if (w.exercises.isEmpty()) {
                item { Txt("Add your first exercise to get going.", T.body, C.dim, Modifier.padding(vertical = 8.dp)) }
            }
            itemsIndexed(w.exercises.toList()) { i, ex ->
                ExerciseBlock(w, ex, useKg, now, onOpen, { w.exercises.removeAt(i); app.saveLive() }, { hint = it }, { app.saveLive() })
            }
            item { ForgeButton("Add exercise", onAdd, Modifier.fillMaxWidth(), filled = false, icon = Icons.Rounded.Add) }
            item {
                Column {
                    Spacer(Modifier.height(6.dp))
                    SectionLabel("Rest timer after each set")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Off", 60 to "1:00", 90 to "1:30", 120 to "2:00", 180 to "3:00").forEach { (sec, label) ->
                            Chip(label, w.restSec == sec, { w.restSec = sec; app.saveLive() })
                        }
                    }
                }
            }
            item {
                if (!confirmDiscard) {
                    ForgeButton("Discard workout", { confirmDiscard = true }, Modifier.fillMaxWidth(), filled = false, danger = true)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ForgeButton("Keep going", { confirmDiscard = false }, Modifier.weight(1f), filled = false)
                        ForgeButton("Yes, discard", { app.discardLive(); onBack() }, Modifier.weight(1f), danger = true)
                    }
                }
            }
        }
        if (resting) RestBar(w, now)
    }
}

@Composable
private fun RestBar(w: LiveWorkout, now: Long) {
    val left = ((w.restEndsAt - now) / 1000L).toInt().coerceAtLeast(0)
    val frac = if (w.restTotalSec > 0) (left.toFloat() / w.restTotalSec).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth().background(C.surface).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(C.line)) {
            Box(Modifier.fillMaxWidth(frac).height(4.dp).background(C.volt))
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Txt("REST", T.label, C.dim)
                Txt(formatClock(left), T.numM, C.volt)
            }
            Chip("-15", false, { w.restEndsAt -= 15_000L })
            Chip("+15", false, { w.restEndsAt += 15_000L; w.restTotalSec += 15 })
            Chip("Skip", false, { w.restEndsAt = 0L })
        }
    }
}

@Composable
private fun ExerciseBlock(
    w: LiveWorkout, ex: LiveExercise, useKg: Boolean, now: Long,
    onOpen: (String) -> Unit, onRemove: () -> Unit, onHint: (String) -> Unit, onChanged: () -> Unit
) {
    val app = LocalApp.current
    val prev = remember(ex.exerciseId, app.store.workouts.size) { app.store.lastSetsFor(ex.exerciseId) }
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).pressable { onOpen(ex.exerciseId) }, verticalAlignment = Alignment.CenterVertically) {
                    ExerciseImage(app.thumbFor(app.exercise(ex.exerciseId)), Modifier.size(48.dp))
                    Spacer(Modifier.width(12.dp))
                    Txt(ex.name, T.h3, C.volt, maxLines = 2)
                }
                Box(Modifier.pressable(onClick = onRemove).padding(8.dp)) {
                    Icon(Icons.Rounded.Close, null, tint = C.dim, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt("SET", T.label, C.faint, Modifier.width(38.dp), align = TextAlign.Center)
                Txt("LAST TIME", T.label, C.faint, Modifier.weight(1.25f), align = TextAlign.Center)
                Txt(unitLabel(useKg).uppercase(), T.label, C.faint, Modifier.weight(1f), align = TextAlign.Center)
                Txt("REPS", T.label, C.faint, Modifier.weight(1f), align = TextAlign.Center)
                Spacer(Modifier.width(40.dp))
            }
            var number = 0
            ex.sets.toList().forEachIndexed { i, s ->
                if (!s.warm) number++
                val p = prev.getOrNull(i) ?: prev.lastOrNull()
                Spacer(Modifier.height(6.dp))
                SetRow(s, if (s.warm) "W" else "$number", p, useKg, onHint) {
                    if (s.done && w.restSec > 0) {
                        w.restTotalSec = w.restSec
                        w.restEndsAt = System.currentTimeMillis() + w.restSec * 1000L
                    }
                    onChanged()
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.weight(1f).pressable { ex.sets.add(LiveSet(warm = false)); onChanged() }.clip(CutXs).background(C.surface2).padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) { Txt("+ ADD SET", T.label, C.text) }
                if (ex.sets.size > 1) {
                    Box(
                        Modifier.pressable { ex.sets.removeAt(ex.sets.lastIndex); onChanged() }.clip(CutXs).border(1.dp, C.line, CutXs).padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Txt("REMOVE LAST", T.label, C.dim) }
                }
            }
        }
    }
}

@Composable
private fun SetRow(s: LiveSet, label: String, prev: SetLog?, useKg: Boolean, onHint: (String) -> Unit, onToggled: () -> Unit) {
    val prevW = if (prev != null && prev.weightKg > 0f) formatWeight(prev.weightKg, useKg) else ""
    val prevR = if (prev != null) "${prev.reps}" else ""
    Row(
        Modifier.fillMaxWidth().clip(CutXs).background(if (s.done) C.volt.copy(alpha = 0.10f) else C.surface),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // tap the set number to mark it as a warm-up
        Box(Modifier.width(38.dp).pressable { s.warm = !s.warm }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
            Txt(label, T.h3, if (s.warm) C.ember else C.text)
        }
        Txt(
            if (prev == null) "–" else if (prevW.isEmpty()) "$prevR reps" else "$prevW × $prevR",
            T.bodyM, C.dim, Modifier.weight(1.25f), maxLines = 1, align = TextAlign.Center
        )
        NumField(s.weight, { s.weight = it }, prevW.ifEmpty { "0" }, true, Modifier.weight(1f))
        NumField(s.reps, { s.reps = it }, prevR.ifEmpty { "0" }, false, Modifier.weight(1f))
        Box(
            Modifier.size(40.dp).pressable {
                if (s.done) {
                    s.done = false
                    onToggled()
                } else {
                    // ticking an empty row repeats last time's numbers
                    if (s.weight.isBlank() && prevW.isNotEmpty()) s.weight = prevW
                    if (s.reps.isBlank() && prevR.isNotEmpty()) s.reps = prevR
                    if (parseNumber(s.reps) <= 0f) {
                        onHint("Type the reps you did, then tick the set.")
                    } else {
                        onHint("")
                        s.done = true
                        onToggled()
                    }
                }
            }.clip(CutXs).background(if (s.done) C.volt else C.surface2),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Check, null, tint = if (s.done) C.bg else C.dim, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun NumField(value: String, onChange: (String) -> Unit, hint: String, decimal: Boolean, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = { v -> if (v.length <= 6 && v.all { it.isDigit() || (decimal && (it == '.' || it == ',')) }) onChange(v) },
        singleLine = true,
        textStyle = T.h3.copy(color = C.text, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(C.volt),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().clip(CutXs).background(C.surface2).padding(vertical = 9.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (value.isEmpty()) Txt(hint, T.h3, C.faint, maxLines = 1)
                inner()
            }
        }
    )
}
