package app.forge.gym.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import app.forge.gym.LocalApp
import app.forge.gym.data.PlanDay
import app.forge.gym.ui.C
import app.forge.gym.ui.CutXs
import app.forge.gym.ui.Empty
import app.forge.gym.ui.ExerciseImage
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.Hairline
import app.forge.gym.ui.IconBox
import app.forge.gym.ui.Panel
import app.forge.gym.ui.T
import app.forge.gym.ui.Tag
import app.forge.gym.ui.Txt
import app.forge.gym.ui.pressable
import app.forge.gym.ui.staggerIn

@Composable
fun PlanScreen(onBack: () -> Unit, onStart: (Int) -> Unit, onOpen: (String) -> Unit) {
    val app = LocalApp.current
    val store = app.store
    val p = store.profile
    val plan = app.plan()
    var open by rememberSaveable { mutableIntStateOf(app.nextDayIndex()) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusTop() + 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(Icons.AutoMirrored.Rounded.ArrowBack, onBack)
                Spacer(Modifier.width(14.dp))
                Column {
                    Txt("YOUR WEEK", T.h1)
                    Txt("${p.level.label} · ${p.goal.label} · ${p.daysPerWeek} days", T.small, C.dim)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag(p.gender.label)
                Tag(p.place.label)
                Tag("${plan.size} sessions")
            }
        }
        item {
            ForgeButton(
                "Shuffle exercises", { store.updateProfile(p.copy(planSeed = p.planSeed + 1)) },
                Modifier.fillMaxWidth(), filled = false, icon = Icons.Rounded.Refresh
            )
        }
        if (plan.isEmpty()) {
            item { Empty("Building your plan", "One second.") }
        }
        itemsIndexed(plan) { i, d ->
            DayCard(i, d, open == i, app.nextDayIndex() == i, { open = if (open == i) -1 else i }, onStart, onOpen,
                Modifier.staggerIn(i))
        }
    }
}

@Composable
private fun DayCard(
    i: Int,
    d: PlanDay,
    expanded: Boolean,
    next: Boolean,
    onToggle: () -> Unit,
    onStart: (Int) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier
) {
    val app = LocalApp.current
    val rot by animateFloatAsState(if (expanded) 180f else 0f, tween(260), label = "chev")
    Panel(modifier.fillMaxWidth(), onClick = onToggle) {
        Column {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt("DAY ${i + 1}", T.label, if (next) C.volt else C.dim)
                        if (next) {
                            Spacer(Modifier.width(8.dp))
                            Tag("Up next", C.volt)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Txt(d.title.uppercase(), T.h2)
                    Txt("${d.focus} · ${d.items.sumOf { it.sets }} sets", T.small, C.dim, maxLines = 1)
                }
                Icon(Icons.Rounded.ExpandMore, null, tint = C.dim, modifier = Modifier.size(26.dp).rotate(rot))
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(280)) + fadeIn(tween(240)),
                exit = shrinkVertically(tween(220)) + fadeOut(tween(160))
            ) {
                Column {
                    Hairline()
                    d.items.forEach { pe ->
                        val e = app.exercise(pe.exerciseId)
                        if (e != null) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .pressable { onOpen(e.id) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ExerciseImage(e.imageUrl(0), Modifier.size(52.dp).clip(CutXs))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Txt(e.name, T.bodyM, maxLines = 2)
                                    Txt(
                                        "${pe.sets} × ${pe.reps}" + if (pe.restSec > 0) " · rest ${pe.restSec}s" else "",
                                        T.small, C.dim
                                    )
                                }
                            }
                        }
                    }
                    Column(Modifier.padding(16.dp)) {
                        ForgeButton("Start day ${i + 1}", { onStart(i) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
                    }
                }
            }
        }
    }
}
