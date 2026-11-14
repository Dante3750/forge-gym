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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.forge.gym.LocalApp
import app.forge.gym.data.ALL_MUSCLES
import app.forge.gym.data.Exercise
import app.forge.gym.data.prettyMuscle
import app.forge.gym.sharedKey
import app.forge.gym.ui.C
import app.forge.gym.ui.Chip
import app.forge.gym.ui.CutXs
import app.forge.gym.ui.Empty
import app.forge.gym.ui.IconBox
import app.forge.gym.ui.ExerciseImage
import app.forge.gym.ui.Panel
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt
import app.forge.gym.ui.pressable

/** What the list is showing. Saved so coming back from an exercise keeps your place. */
class LibraryUi(query: String = "", muscle: String = "") {
    var query by mutableStateOf(query)
    var muscle by mutableStateOf(muscle)
    /** The filter the list was last scrolled for; a new filter jumps back to the top. */
    var scrolledFor: String = "$query|$muscle"

    companion object {
        val Saver = listSaver<LibraryUi, String>(
            save = { listOf(it.query, it.muscle) },
            restore = { LibraryUi(it[0], it[1]) }
        )
    }
}

@Composable
fun rememberLibraryUi(): LibraryUi = rememberSaveable(saver = LibraryUi.Saver) { LibraryUi() }

fun equipmentLabel(e: Exercise): String = when (e.equipment) {
    "none", "body only" -> "No equipment"
    else -> e.equipment.replaceFirstChar { it.uppercase() }
}

/** The whole app in one screen: every exercise, a search box and a muscle filter. */
@Composable
fun LibraryScreen(
    ui: LibraryUi, listState: LazyListState, onOpen: (String) -> Unit, onBody: () -> Unit,
    /** Set when this is the "add exercise" picker: shows a back button and a title instead of the greeting. */
    onBack: (() -> Unit)? = null
) {
    val app = LocalApp.current
    val all = app.exercises
    val name = app.store.profile.name
    // exercises that have a 3D demo come first
    val sorted = remember(all) { all.sortedWith(compareByDescending<Exercise> { app.motionFor(it.id) != null }.thenBy { it.name }) }
    val shown = remember(sorted, ui.query, ui.muscle) {
        val q = ui.query.trim().lowercase()
        sorted.filter { e ->
            (ui.muscle.isEmpty() || ui.muscle in e.primary) &&
                (q.isEmpty() || e.name.lowercase().contains(q) || e.primary.any { it.contains(q) } || e.equipment.contains(q))
        }
    }
    val key = "${ui.query}|${ui.muscle}"
    LaunchedEffect(key) {
        if (ui.scrolledFor != key) { ui.scrolledFor = key; listState.scrollToItem(0) }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = statusTop() + 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconBox(Icons.AutoMirrored.Rounded.ArrowBack, onBack)
                Spacer(Modifier.width(14.dp))
                Txt("ADD EXERCISE", T.h1)
            } else {
                Column(Modifier.weight(1f)) {
                    Txt(if (name.isBlank()) "HI" else "HI, ${name.uppercase()}", T.label, C.volt, maxLines = 1)
                    Txt("EXERCISES", T.hero)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        SearchField(ui.query, { ui.query = it }, if (all.isEmpty()) "Search exercises" else "Search ${all.size} exercises", Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onBack == null) item {
                Row(
                    Modifier.pressable(onClick = onBody).clip(CutXs).border(1.dp, C.volt, CutXs).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.AccessibilityNew, null, tint = C.volt, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Txt("PICK ON BODY", T.label, C.volt)
                }
            }
            item { Chip("All", ui.muscle.isEmpty(), { ui.muscle = "" }) }
            items(ALL_MUSCLES) { m ->
                Chip(prettyMuscle(m), ui.muscle == m, { ui.muscle = if (ui.muscle == m) "" else m })
            }
        }
        Spacer(Modifier.height(14.dp))
        if (all.isEmpty()) {
            Empty("Loading", "Getting the exercises ready.")
        } else if (shown.isEmpty()) {
            Empty("Nothing found", "Try a different word, or tap All.")
        } else {
            LazyColumn(
                Modifier.fillMaxSize(), state = listState,
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp,
                    bottom = 20.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "count") {
                    Txt(
                        (if (ui.muscle.isEmpty()) "${shown.size} EXERCISES" else "${shown.size} ${prettyMuscle(ui.muscle).uppercase()} EXERCISES"),
                        T.label, C.faint
                    )
                }
                items(shown, key = { it.id }) { e -> ExerciseRow(e) { onOpen(e.id) } }
            }
        }
    }
}

@Composable
fun ExerciseRow(e: Exercise, onClick: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExerciseImage(LocalApp.current.thumbFor(e), Modifier.size(84.dp).sharedKey("ex-img-${e.id}"))
            Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Txt(e.name, T.h3, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Txt(
                    e.primary.joinToString(", ") { prettyMuscle(it) }.ifEmpty { "Full body" } + " · " + equipmentLabel(e),
                    T.small, C.dim, maxLines = 1
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = C.faint, modifier = Modifier.padding(end = 10.dp).size(22.dp))
        }
    }
}
