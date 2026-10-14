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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.forge.gym.LocalApp
import app.forge.gym.data.ALL_MUSCLES
import app.forge.gym.data.PlanDay
import app.forge.gym.data.formatWeight
import app.forge.gym.data.unitLabel
import app.forge.gym.greeting
import app.forge.gym.ui.BodyMap3D
import app.forge.gym.ui.C
import app.forge.gym.ui.Cut
import app.forge.gym.ui.CutSm
import app.forge.gym.ui.CutXs
import app.forge.gym.ui.Empty
import app.forge.gym.ui.ExerciseImage
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.IconBox
import app.forge.gym.ui.Panel
import app.forge.gym.ui.SectionLabel
import app.forge.gym.ui.T
import app.forge.gym.ui.Tag
import app.forge.gym.ui.Txt
import app.forge.gym.ui.staggerIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

private val tips = listOf(
    "Log the weight you actually lifted. Next month's you will want the numbers.",
    "Sleep is the cheapest supplement there is. Seven hours beats one more set.",
    "Stop a rep before your form breaks. Not three after.",
    "A 20 minute walk on a rest day still counts as training.",
    "Warm up the first lift with two light sets, then start working.",
    "Add weight once you hit the top of the rep range on every set.",
    "Drink water before you feel thirsty. Rest days too."
)

@Composable
fun statusTop() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

@Composable
fun HomeScreen(
    onStart: (Int) -> Unit,
    onPlan: () -> Unit,
    onBody: () -> Unit,
    onWalk: () -> Unit,
    onProgress: () -> Unit,
    onSettings: () -> Unit
) {
    val app = LocalApp.current
    val store = app.store
    val p = store.profile
    val plan = app.plan()
    val dayIdx = app.nextDayIndex()
    val day = plan.getOrNull(dayIdx)
    val hour = remember { LocalTime.now().hour }
    val streak = store.streak()
    val heat = store.muscleHeat { app.exercise(it) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusTop() + 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().staggerIn(0),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(C.volt))
                    Spacer(Modifier.width(8.dp))
                    Txt("FORGE", T.h2)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StreakPill(streak)
                    Spacer(Modifier.width(10.dp))
                    IconBox(Icons.Rounded.Settings, onSettings)
                }
            }
        }
        item {
            Column(Modifier.staggerIn(1)) {
                Txt("${greeting(hour)},".uppercase(), T.label, C.dim)
                Txt(if (p.name.isBlank()) "ATHLETE" else p.name.uppercase(), T.hero, maxLines = 1)
            }
        }
        item {
            Box(Modifier.staggerIn(2)) {
                if (day == null) {
                    Panel(Modifier.fillMaxWidth()) { Empty("Building your plan", "One second.") }
                } else {
                    TodayCard(day, dayIdx, plan.size, store.trainedToday()) { onStart(dayIdx) }
                }
            }
        }
        item { Box(Modifier.staggerIn(3)) { WeekStrip() } }
        item {
            Row(Modifier.fillMaxWidth().staggerIn(4), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val steps = store.stepsToday()
                MiniStat(
                    "Steps", steps.toString().reversed().chunked(3).joinToString(",").reversed(), "",
                    (steps.toFloat() / p.stepGoal).coerceIn(0f, 1f), Modifier.weight(1f), onWalk
                )
                val ml = store.waterToday()
                Panel(Modifier.weight(1f)) {
                    Column(Modifier.padding(14.dp)) {
                        Txt("WATER", T.label, C.dim)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Txt("%.2f".format(ml / 1000f), T.numM)
                            Spacer(Modifier.width(4.dp))
                            Txt("L", T.label, C.dim, Modifier.padding(bottom = 3.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Bar((ml.toFloat() / p.waterGoalMl).coerceIn(0f, 1f))
                        Spacer(Modifier.height(10.dp))
                        IconBox(Icons.Rounded.Add, { store.addWater(250) }, Modifier.size(36.dp), filled = true)
                    }
                }
                val last = store.weights.lastOrNull()
                MiniStat(
                    "Weight", if (last == null) "—" else formatWeight(last.kg, p.useKg), unitLabel(p.useKg),
                    if (last == null || p.goalWeightKg <= 0f) 0f else 1f - (kotlin.math.abs(last.kg - p.goalWeightKg) / 20f).coerceIn(0f, 1f),
                    Modifier.weight(1f), onProgress
                )
            }
        }
        item {
            Panel(Modifier.fillMaxWidth().staggerIn(5), onClick = onBody) {
                Row(Modifier.padding(start = 18.dp, top = 16.dp, bottom = 16.dp)) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("Muscle heat")
                        Spacer(Modifier.height(10.dp))
                        Txt("WHERE YOU'VE BEEN TRAINING", T.h2)
                        Spacer(Modifier.height(6.dp))
                        Txt(
                            if (heat.isEmpty()) "Nothing logged this week yet. Your first session lights it up."
                            else "Last 7 days. Brighter means more sets.",
                            T.small, C.dim
                        )
                        Spacer(Modifier.height(14.dp))
                        Tag("Open 3D body", C.volt)
                    }
                    Box(Modifier.width(140.dp).height(230.dp)) {
                        BodyMap3D(
                            gender = p.gender, selected = null, heat = heat, onSelect = {},
                            modifier = Modifier.fillMaxSize(), interactive = false, autoSpin = true
                        )
                    }
                }
            }
        }
        if (plan.isNotEmpty()) {
            item {
                Column(Modifier.staggerIn(6)) {
                    SectionLabel("Your week", action = "See all", onAction = onPlan)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(plan) { i, d ->
                            DayChip(i, d, i == dayIdx) { onStart(i) }
                        }
                    }
                }
            }
        }
        item {
            val tip = tips[LocalDate.now().dayOfYear % tips.size]
            Box(
                Modifier
                    .fillMaxWidth()
                    .staggerIn(7)
                    .clip(CutSm)
                    .border(1.dp, C.line, CutSm)
                    .padding(18.dp)
            ) {
                Column {
                    Txt("COACH'S NOTE", T.label, C.volt)
                    Spacer(Modifier.height(6.dp))
                    Txt(tip, T.bodyM, C.text)
                }
            }
        }
    }
}

@Composable
private fun StreakPill(streak: Int) {
    Row(
        Modifier
            .clip(CutXs)
            .border(1.dp, if (streak > 0) C.ember else C.line, CutXs)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.LocalFireDepartment, null,
            tint = if (streak > 0) C.ember else C.faint, modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(4.dp))
        Txt(streak.toString(), T.h3, if (streak > 0) C.text else C.faint)
    }
}

@Composable
private fun TodayCard(day: PlanDay, idx: Int, total: Int, doneToday: Boolean, onStart: () -> Unit) {
    val app = LocalApp.current
    val sets = day.items.sumOf { it.sets }
    val minutes = (sets * 2.6f).toInt().coerceAtLeast(10)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(Cut)
            .background(C.surface)
            .border(1.dp, C.line, Cut)
    ) {
        Txt(
            "${idx + 1}", T.numXL.copy(fontSize = 150.sp, lineHeight = 130.sp), C.surface2,
            Modifier.align(Alignment.TopEnd).padding(end = 18.dp, top = 6.dp)
        )
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (doneToday) {
                    Icon(Icons.Rounded.Check, null, tint = C.volt, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Txt(
                    if (doneToday) "DONE TODAY · DAY ${idx + 1} OF $total" else "TODAY · DAY ${idx + 1} OF $total",
                    T.label, C.volt
                )
            }
            Spacer(Modifier.height(10.dp))
            Txt(day.title.uppercase(), T.hero, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Txt(day.focus, T.bodyM, C.dim, maxLines = 2)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Fact("${day.items.size}", "Moves")
                Fact("$sets", "Sets")
                Fact("~$minutes", "Min")
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                day.items.take(4).forEach { item ->
                    val e = app.exercise(item.exerciseId)
                    ExerciseImage(e?.imageUrl(0), Modifier.size(58.dp).clip(CutXs))
                }
            }
            Spacer(Modifier.height(20.dp))
            ForgeButton(
                if (doneToday) "Train again" else "Start workout", onStart,
                Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow
            )
        }
    }
}

@Composable
private fun Fact(value: String, label: String) {
    Column {
        Txt(value, T.num)
        Txt(label.uppercase(), T.label, C.dim)
    }
}

@Composable
private fun WeekStrip() {
    val store = LocalApp.current.store
    val days = store.activityDays()
    val monday = LocalDate.now().with(DayOfWeek.MONDAY)
    val today = LocalDate.now()
    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0 until 7) {
            val d = monday.plusDays(i.toLong())
            val done = d in days
            val isToday = d == today
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Txt(letters[i], T.label, if (isToday) C.text else C.faint)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (done) C.volt else C.surface)
                        .border(if (isToday) 2.dp else 1.dp, if (isToday) C.volt else C.line, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) Icon(Icons.Rounded.Check, null, tint = C.bg, modifier = Modifier.size(20.dp))
                    else Txt(d.dayOfMonth.toString(), T.small, if (isToday) C.text else C.faint)
                }
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float) {
    Box(Modifier.fillMaxWidth().height(4.dp).background(C.line)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(4.dp).background(C.volt))
    }
}

@Composable
private fun MiniStat(label: String, value: String, unit: String, progress: Float, modifier: Modifier, onClick: () -> Unit) {
    Panel(modifier, onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Txt(label.uppercase(), T.label, C.dim)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Txt(value, T.numM, maxLines = 1)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Txt(unit.uppercase(), T.label, C.dim, Modifier.padding(bottom = 3.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Bar(progress)
        }
    }
}

@Composable
private fun DayChip(i: Int, d: PlanDay, next: Boolean, onClick: () -> Unit) {
    Panel(Modifier.width(150.dp), accent = next, onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Txt("DAY ${i + 1}", T.label, if (next) C.bg else C.dim)
            Spacer(Modifier.height(18.dp))
            Txt(d.title.uppercase(), T.h2, if (next) C.bg else C.text, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Txt("${d.items.sumOf { it.sets }} sets", T.small, if (next) C.bg else C.dim)
        }
    }
}
