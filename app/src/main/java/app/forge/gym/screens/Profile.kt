package app.forge.gym.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.forge.gym.LocalApp
import app.forge.gym.data.WorkoutLog
import app.forge.gym.data.dayOf
import app.forge.gym.data.formatClock
import app.forge.gym.data.formatWeight
import app.forge.gym.data.kgToDisplay
import app.forge.gym.data.prettyMuscle
import app.forge.gym.data.unitLabel
import app.forge.gym.ui.C
import app.forge.gym.ui.Empty
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.IconBox
import app.forge.gym.ui.Panel
import app.forge.gym.ui.SectionLabel
import app.forge.gym.ui.Segmented
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

fun workoutDate(w: WorkoutLog): String = dayOf(w.startedAt).format(dayFmt)

/** "12,400 kg" style total for a workout or for all time. */
fun volumeText(kg: Float, useKg: Boolean): String = "%,d %s".format(kg.kgToDisplay(useKg).toInt(), unitLabel(useKg))

/** The Profile tab: your numbers, how often you train, and every workout you logged. */
@Composable
fun ProfileScreen(onOpenWorkout: (Long) -> Unit, onEdit: () -> Unit) {
    val app = LocalApp.current
    val store = app.store
    val p = store.profile
    val workouts = store.workouts.toList()
    val useKg = p.useKg

    val weeks = remember(workouts) {
        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        (7 downTo 0).map { k ->
            val start = monday.minusWeeks(k.toLong())
            val end = start.plusDays(7)
            val n = workouts.count { val d = dayOf(it.startedAt); !d.isBefore(start) && d.isBefore(end) }
            "${start.dayOfMonth}/${start.monthValue}" to n
        }
    }
    val muscles = remember(workouts, app.exercises) {
        val since = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
        val m = HashMap<String, Int>()
        workouts.filter { it.startedAt >= since }.forEach { w ->
            w.exercises.forEach { log ->
                app.exercise(log.exerciseId)?.primary?.forEach { mu -> m[mu] = (m[mu] ?: 0) + log.sets.count { !it.warm } }
            }
        }
        m.entries.sortedByDescending { it.value }.take(6).map { it.key to it.value }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusTop() + 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Txt("PROFILE", T.label, C.volt)
                    Txt(p.name.uppercase().ifBlank { "YOU" }, T.hero, maxLines = 1)
                }
                IconBox(Icons.Rounded.Edit, onEdit)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${workouts.size}", "Workouts", Modifier.weight(1f))
                Stat("${store.workoutsThisWeek()}", "This week", Modifier.weight(1f))
                Stat("${store.streak()}", "Day streak", Modifier.weight(1f))
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    SectionLabel("Workouts per week")
                    Spacer(Modifier.height(12.dp))
                    val max = (weeks.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
                    Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        weeks.forEachIndexed { i, (label, n) ->
                            Column(
                                Modifier.weight(1f).fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom
                            ) {
                                Txt(if (n > 0) "$n" else "", T.label, C.dim)
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.fillMaxWidth().height((4 + 72 * n / max).dp).background(if (i == weeks.lastIndex) C.volt else C.surface2))
                                Spacer(Modifier.height(6.dp))
                                Txt(label, T.label.copy(fontSize = 10.sp, letterSpacing = 0.sp), C.faint, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
        if (muscles.isNotEmpty()) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        SectionLabel("Sets per muscle · last 30 days")
                        Spacer(Modifier.height(12.dp))
                        val max = muscles.first().second.coerceAtLeast(1)
                        muscles.forEach { (m, n) ->
                            Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Txt(prettyMuscle(m), T.bodyM, C.text, Modifier.width(96.dp), maxLines = 1)
                                Box(Modifier.weight(1f).height(10.dp).background(C.surface2)) {
                                    Box(Modifier.fillMaxWidth(n.toFloat() / max).height(10.dp).background(C.volt))
                                }
                                Txt("$n", T.label, C.dim, Modifier.width(36.dp).padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            Column {
                SectionLabel("Weights shown in")
                Spacer(Modifier.height(10.dp))
                Segmented(listOf("kg", "lb"), if (useKg) 0 else 1, { store.updateProfile(p.copy(useKg = it == 0)) })
            }
        }
        item {
            Spacer(Modifier.height(6.dp))
            SectionLabel("History")
        }
        if (workouts.isEmpty()) {
            item { Empty("No workouts yet", "Finish a workout and it shows up here.") }
        }
        items(workouts, key = { it.id }) { w ->
            Panel(Modifier.fillMaxWidth(), onClick = { onOpenWorkout(w.id) }) {
                Column(Modifier.padding(14.dp)) {
                    Txt(w.title, T.h2, maxLines = 1)
                    Txt(workoutDate(w).uppercase(), T.label, C.dim)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Mini("Time", formatClock(w.durationSec))
                        Mini("Volume", volumeText(w.volumeKg, useKg))
                        Mini("Sets", "${w.totalSets}")
                    }
                    Spacer(Modifier.height(10.dp))
                    w.exercises.take(3).forEach { e -> Txt("${e.sets.size} × ${e.name}", T.small, C.dim, maxLines = 1) }
                    if (w.exercises.size > 3) Txt("+ ${w.exercises.size - 3} more", T.small, C.faint)
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Panel(modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Txt(value, T.num, C.text, maxLines = 1)
            Txt(label.uppercase(), T.label, C.dim, maxLines = 1)
        }
    }
}

@Composable
fun Mini(label: String, value: String) {
    Column {
        Txt(label.uppercase(), T.label, C.faint)
        Txt(value, T.h3, C.text)
    }
}

/** One finished workout, set by set. */
@Composable
fun WorkoutDetailScreen(id: Long, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val app = LocalApp.current
    val store = app.store
    val useKg = store.profile.useKg
    val w = store.workouts.firstOrNull { it.id == id }
    var confirm by remember { mutableStateOf(false) }
    if (w == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Empty("Workout not found", "It may have been deleted.")
                ForgeButton("Back", onBack, filled = false)
            }
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusTop() + 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(Icons.AutoMirrored.Rounded.ArrowBack, onBack)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Txt(w.title.uppercase(), T.h1, maxLines = 2)
                    Txt(workoutDate(w).uppercase(), T.label, C.dim)
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth(), accent = true) {
                Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Txt("TIME", T.label, C.bg); Txt(formatClock(w.durationSec), T.numM, C.bg) }
                    Column { Txt("VOLUME", T.label, C.bg); Txt(volumeText(w.volumeKg, useKg), T.numM, C.bg) }
                    Column { Txt("SETS", T.label, C.bg); Txt("${w.totalSets}", T.numM, C.bg) }
                }
            }
        }
        items(w.exercises) { e ->
            Panel(Modifier.fillMaxWidth(), onClick = { onOpen(e.exerciseId) }) {
                Column(Modifier.padding(14.dp)) {
                    Txt(e.name, T.h3, C.volt, maxLines = 2)
                    Spacer(Modifier.height(8.dp))
                    var n = 0
                    e.sets.forEach { s ->
                        if (!s.warm) n++
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Txt(if (s.warm) "W" else "$n", T.bodyM, if (s.warm) C.ember else C.dim, Modifier.width(32.dp))
                            Txt(
                                if (s.weightKg > 0f) "${formatWeight(s.weightKg, useKg)} ${unitLabel(useKg)} × ${s.reps}" else "${s.reps} reps",
                                T.bodyM, C.text
                            )
                        }
                    }
                }
            }
        }
        item {
            ForgeButton(
                if (confirm) "Tap again to delete for good" else "Delete workout",
                { if (confirm) { store.deleteWorkout(w.id); onBack() } else confirm = true },
                Modifier.fillMaxWidth().navigationBarsPadding(), filled = confirm, danger = true
            )
        }
    }
}
