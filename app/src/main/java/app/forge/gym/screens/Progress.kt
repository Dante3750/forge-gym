package app.forge.gym.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.forge.gym.LocalApp
import app.forge.gym.data.ScaleSources
import app.forge.gym.data.WeightEntry
import app.forge.gym.data.dayOf
import app.forge.gym.data.displayToKg
import app.forge.gym.data.formatClock
import app.forge.gym.data.formatWeight
import app.forge.gym.data.kgToDisplay
import app.forge.gym.data.unitLabel
import app.forge.gym.formatDay
import app.forge.gym.ui.C
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.Hairline
import app.forge.gym.ui.IconBox
import app.forge.gym.ui.LineChart
import app.forge.gym.ui.Panel
import app.forge.gym.ui.Ring
import app.forge.gym.ui.SectionLabel
import app.forge.gym.ui.Segmented
import app.forge.gym.ui.Stepper
import app.forge.gym.ui.T
import app.forge.gym.ui.Tag
import app.forge.gym.ui.Txt
import app.forge.gym.ui.WeekBars
import app.forge.gym.ui.pressable
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(onSettings: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusTop() + 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Txt("PROGRESS", T.h1, modifier = Modifier.weight(1f))
                IconBox(Icons.Rounded.Settings, onSettings)
            }
        }
        item { Segmented(listOf("Weight", "Stats", "Tools"), tab, { tab = it }) }
        when (tab) {
            0 -> weightPane()
            1 -> statsPane()
            else -> toolsPane()
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.weightPane() {
    item { WeightBlock() }
}

@Composable
private fun WeightBlock() {
    val store = LocalApp.current.store
    val p = store.profile
    val list = store.weights.toList()
    var dialog by remember { mutableStateOf(false) }
    val latest = list.lastOrNull()
    val first = list.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Txt("CURRENT WEIGHT", T.label, C.dim)
                Row(verticalAlignment = Alignment.Bottom) {
                    Txt(if (latest != null) formatWeight(latest.kg, p.useKg) else "--", T.numXL)
                    Spacer(Modifier.width(6.dp))
                    Txt(unitLabel(p.useKg).uppercase(), T.label, C.dim, Modifier.padding(bottom = 10.dp))
                }
                if (latest != null && first != null && list.size > 1) {
                    val d = latest.kg - first.kg
                    val sign = if (d > 0) "+" else ""
                    Txt("$sign${formatWeight(d, p.useKg)} ${unitLabel(p.useKg)} since ${formatDay(dayOf(first.time))}", T.small, if (d <= 0f) C.volt else C.ember)
                }
                if (latest != null && p.heightCm > 0) {
                    val m = p.heightCm / 100f
                    val bmi = latest.kg / (m * m)
                    val cat = when {
                        bmi < 18.5f -> "Under"
                        bmi < 25f -> "Healthy"
                        bmi < 30f -> "Over"
                        else -> "High"
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag("BMI ${"%.1f".format(bmi)} · $cat")
                        if (p.goalWeightKg > 0f) Tag("Goal ${formatWeight(p.goalWeightKg, p.useKg)} ${unitLabel(p.useKg)}", C.volt)
                    }
                }
            }
        }
        if (list.size >= 2) {
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    LineChart(list.takeLast(30).map { it.kg.kgToDisplay(p.useKg) }, goal = p.goalWeightKg.takeIf { it > 0f }?.kgToDisplay(p.useKg))
                }
            }
        }
        ForgeButton("Log weight", { dialog = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)

        SectionLabel("Weigh-in sources")
        Panel(Modifier.fillMaxWidth()) {
            Column {
                ScaleSources.all.forEachIndexed { i, s ->
                    if (i > 0) Hairline()
                    Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Txt(s.label, T.bodyM)
                            Txt(s.note, T.small, C.dim)
                        }
                        Tag(if (s.available) "Active" else "Soon", if (s.available) C.volt else C.dim)
                    }
                }
            }
        }

        if (list.isNotEmpty()) {
            SectionLabel("History")
            Panel(Modifier.fillMaxWidth()) {
                Column {
                    list.asReversed().take(20).forEachIndexed { i, w ->
                        if (i > 0) Hairline()
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Txt("${formatWeight(w.kg, p.useKg)} ${unitLabel(p.useKg)}", T.bodyM)
                                Txt(formatDay(dayOf(w.time)) + " · " + w.source, T.small, C.dim)
                            }
                            IconBox(Icons.Rounded.Delete, { store.deleteWeight(w.time) }, tint = C.dim)
                        }
                    }
                }
            }
        }
    }
    if (dialog) AddWeightDialog(latest) { dialog = false }
}

@Composable
private fun AddWeightDialog(latest: WeightEntry?, onDone: () -> Unit) {
    val store = LocalApp.current.store
    val useKg = store.profile.useKg
    var v by remember { mutableFloatStateOf((latest?.kg ?: 70f).kgToDisplay(useKg)) }
    Dialog(onDismissRequest = onDone) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Txt("LOG WEIGHT", T.h2)
                Stepper(formatWeightRaw(v), unitLabel(useKg), { v = (v - 0.1f).coerceAtLeast(20f) }, { v += 0.1f })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ForgeButton("Cancel", onDone, Modifier.weight(1f), filled = false)
                    ForgeButton("Save", {
                        store.addWeight(v.displayToKg(useKg))
                        onDone()
                    }, Modifier.weight(1f))
                }
            }
        }
    }
}

private fun formatWeightRaw(v: Float): String = "%.1f".format(v)

private fun androidx.compose.foundation.lazy.LazyListScope.statsPane() {
    item { StatsBlock() }
}

@Composable
private fun StatsBlock() {
    val app = LocalApp.current
    val store = app.store
    val p = store.profile
    val workouts = store.workouts.toList()
    val thisWeek = store.workoutsThisWeek()
    val today = LocalDate.now()
    val perDay = (6 downTo 0).map { off ->
        val d = today.minusDays(off.toLong())
        workouts.count { dayOf(it.startedAt) == d }.toFloat()
    }
    val labels = (6 downTo 0).map { today.minusDays(it.toLong()).dayOfWeek.name.first().toString() }
    val totalVol = workouts.sumOf { it.volumeKg.toDouble() }.toFloat()
    val totalSets = workouts.sumOf { it.totalSets }
    val best = store.bestOneRm().entries.sortedByDescending { it.value }.take(5)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Ring((thisWeek.toFloat() / p.daysPerWeek.coerceAtLeast(1)), 104.dp, 10.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Txt("$thisWeek/${p.daysPerWeek}", T.numM)
                        Txt("THIS WEEK", T.label, C.dim)
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column {
                    Txt("${store.streak()}", T.num, C.volt)
                    Txt("DAY STREAK", T.label, C.dim)
                    Spacer(Modifier.height(8.dp))
                    Txt("${workouts.size} workouts total", T.small, C.dim)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox("VOLUME", formatWeight(totalVol, p.useKg), unitLabel(p.useKg), Modifier.weight(1f))
            StatBox("SETS", "$totalSets", "", Modifier.weight(1f))
        }
        SectionLabel("Last 7 days")
        Panel(Modifier.fillMaxWidth()) { Box(Modifier.padding(16.dp)) { WeekBars(perDay, labels) } }

        SectionLabel("Personal records")
        if (best.isEmpty()) {
            Panel(Modifier.fillMaxWidth()) { Txt("Log a session and your best lifts show up here.", T.small, C.dim, Modifier.padding(16.dp)) }
        } else {
            Panel(Modifier.fillMaxWidth()) {
                Column {
                    best.forEachIndexed { i, (id, kg) ->
                        if (i > 0) Hairline()
                        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Txt(app.exercise(id)?.name ?: id, T.bodyM, modifier = Modifier.weight(1f), maxLines = 1)
                            Txt("${formatWeight(kg, p.useKg)} ${unitLabel(p.useKg)}", T.numM, C.volt)
                        }
                    }
                }
            }
        }

        SectionLabel("Badges")
        val badges = listOf(
            Triple("First rep", workouts.isNotEmpty(), "Log a workout"),
            Triple("Five down", workouts.size >= 5, "5 workouts"),
            Triple("Habit", store.streak() >= 3, "3 day streak"),
            Triple("Iron", totalVol >= 10000f, "10,000 kg moved"),
            Triple("Walker", store.walks.size >= 3, "3 walks")
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            badges.forEach { (n, got, hint) ->
                Panel(Modifier.weight(1f), accent = got) {
                    Column(Modifier.padding(horizontal = 6.dp, vertical = 12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Txt(n.uppercase(), T.label, if (got) C.bg else C.dim, align = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
                    }
                }
            }
        }

        if (workouts.isNotEmpty()) {
            SectionLabel("History")
            Panel(Modifier.fillMaxWidth()) {
                Column {
                    workouts.asReversed().take(15).forEachIndexed { i, w ->
                        if (i > 0) Hairline()
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Txt(w.title, T.bodyM, maxLines = 1)
                                Txt("${formatDay(dayOf(w.startedAt))} · ${formatClock(w.durationSec)} · ${w.totalSets} sets", T.small, C.dim)
                            }
                            IconBox(Icons.Rounded.Delete, { store.deleteWorkout(w.id) }, tint = C.dim)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, unit: String, modifier: Modifier) {
    Panel(modifier) {
        Column(Modifier.padding(16.dp)) {
            Txt(label, T.label, C.dim)
            Row(verticalAlignment = Alignment.Bottom) {
                Txt(value, T.num)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Txt(unit.uppercase(), T.label, C.dim, Modifier.padding(bottom = 5.dp))
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.toolsPane() {
    item { ToolsBlock() }
}

@Composable
private fun ToolsBlock() {
    val useKg = LocalApp.current.store.profile.useKg
    var w by remember { mutableFloatStateOf(if (useKg) 60f else 135f) }
    var reps by remember { mutableIntStateOf(8) }
    var target by remember { mutableFloatStateOf(if (useKg) 100f else 225f) }
    val unit = unitLabel(useKg)
    val step = if (useKg) 2.5f else 5f
    val oneRm = if (reps <= 1) w else w * (1f + reps / 30f)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionLabel("One rep max")
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Stepper(formatWeightRaw(w), unit, { w = (w - step).coerceAtLeast(0f) }, { w += step })
                Stepper("$reps", "reps", { reps = (reps - 1).coerceAtLeast(1) }, { reps = (reps + 1).coerceAtMost(30) })
                Hairline()
                Row(verticalAlignment = Alignment.Bottom) {
                    Txt("EST. 1RM", T.label, C.dim, Modifier.weight(1f))
                    Txt("${oneRm.roundToInt()}", T.num, C.volt)
                    Spacer(Modifier.width(4.dp))
                    Txt(unit.uppercase(), T.label, C.dim, Modifier.padding(bottom = 5.dp))
                }
                listOf(95, 85, 75, 65).forEach { pct ->
                    Row {
                        Txt("$pct%", T.small, C.dim, Modifier.weight(1f))
                        Txt("${(oneRm * pct / 100f).roundToInt()} $unit", T.small)
                    }
                }
            }
        }
        SectionLabel("Plate calculator")
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Stepper(formatWeightRaw(target), unit, { target = (target - step * 2).coerceAtLeast(0f) }, { target += step * 2 })
                val bar = if (useKg) 20f else 45f
                val plates = if (useKg) listOf(25f, 20f, 15f, 10f, 5f, 2.5f, 1.25f) else listOf(45f, 35f, 25f, 10f, 5f, 2.5f)
                var side = ((target - bar) / 2f).coerceAtLeast(0f)
                val out = mutableListOf<String>()
                for (pl in plates) {
                    var n = 0
                    while (side + 0.001f >= pl) { side -= pl; n++ }
                    if (n > 0) out.add("$n × ${if (pl % 1f == 0f) pl.toInt().toString() else pl.toString()}")
                }
                Txt("Bar ${bar.roundToInt()} $unit, each side:", T.small, C.dim)
                Txt(if (out.isEmpty()) "Just the bar" else out.joinToString("   "), T.h3, C.volt)
                if (side > 0.01f) Txt("Leftover ${"%.2f".format(side * 2)} $unit can't be loaded.", T.small, C.ember)
            }
        }
    }
}
