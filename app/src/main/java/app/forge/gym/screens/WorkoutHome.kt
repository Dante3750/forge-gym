package app.forge.gym.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.forge.gym.LocalApp
import app.forge.gym.data.Routine
import app.forge.gym.ui.C
import app.forge.gym.ui.ExerciseImage
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.Panel
import app.forge.gym.ui.SectionLabel
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt
import app.forge.gym.ui.pressable

/** The Workout tab: start an empty workout, or start one of your routines. */
@Composable
fun WorkoutHomeScreen(onLive: () -> Unit, onEditRoutine: (Long) -> Unit) {
    val app = LocalApp.current
    val store = app.store
    val name = store.profile.name
    val live = app.live

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusTop() + 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Txt(if (name.isBlank()) "HI" else "HI, ${name.uppercase()}", T.label, C.volt, maxLines = 1)
                Txt("WORKOUT", T.hero)
            }
        }
        item {
            if (live != null) {
                ForgeButton("Continue workout · ${live.doneSets} sets done", onLive, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
            } else {
                ForgeButton("Start empty workout", { app.startWorkout(); onLive() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
            }
        }
        item {
            Spacer(Modifier.height(6.dp))
            SectionLabel("Routines", action = "+ New routine", onAction = { onEditRoutine(0L) })
        }
        if (store.routines.isEmpty()) {
            item {
                Txt("A routine is a saved list of exercises. Make one for each day you train, then start it with one tap.", T.body, C.dim)
            }
        }
        items(store.routines.toList(), key = { it.id }) { r ->
            RoutineCard(r, live == null, { app.startWorkout(r); onLive() }, { onEditRoutine(r.id) })
        }
    }
}

@Composable
private fun RoutineCard(r: Routine, canStart: Boolean, onStart: () -> Unit, onEdit: () -> Unit) {
    val app = LocalApp.current
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(r.title, T.h2, C.text, Modifier.weight(1f), maxLines = 2)
                Box(Modifier.pressable(onClick = onEdit).padding(6.dp)) {
                    Icon(Icons.Rounded.Edit, null, tint = C.dim, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                r.items.take(5).forEach { item ->
                    ExerciseImage(app.thumbFor(app.exercise(item.exerciseId)), Modifier.size(52.dp))
                }
                if (r.items.size > 5) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { Txt("+${r.items.size - 5}", T.h3, C.dim) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Txt(r.items.joinToString(", ") { it.name }, T.small, C.dim, maxLines = 2)
            Spacer(Modifier.height(12.dp))
            ForgeButton("Start routine", onStart, Modifier.fillMaxWidth(), enabled = canStart)
        }
    }
}
