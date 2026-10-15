package app.forge.gym.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import app.forge.gym.LocalApp
import app.forge.gym.data.ALL_MUSCLES
import app.forge.gym.data.Exercise
import app.forge.gym.data.Gender
import app.forge.gym.data.Level
import app.forge.gym.data.Place
import app.forge.gym.data.equipment
import app.forge.gym.data.isHome
import app.forge.gym.data.prettyMuscle
import app.forge.gym.sharedKey
import app.forge.gym.ui.BodyMap3D
import app.forge.gym.ui.C
import app.forge.gym.ui.Chip
import app.forge.gym.ui.CutSm
import app.forge.gym.ui.CutXs
import app.forge.gym.ui.Empty
import app.forge.gym.ui.ExerciseImage
import app.forge.gym.ui.Panel
import app.forge.gym.ui.Segmented
import app.forge.gym.ui.T
import app.forge.gym.ui.Tag
import app.forge.gym.ui.Txt
import app.forge.gym.ui.pressable
import app.forge.gym.ui.rememberYawState
import kotlinx.coroutines.launch

/** Filter and view state for Explore. Saved across navigation so coming back from a detail page keeps your place. */
class ExploreUi(seg: Int = 0, query: String = "", muscle: String = "", level: String = "", equip: String = "", favOnly: Boolean = false, body: String = "") {
    var seg by mutableIntStateOf(seg)
    var query by mutableStateOf(query)
    var muscle by mutableStateOf(muscle)
    var level by mutableStateOf(level)
    var equip by mutableStateOf(equip)
    var favOnly by mutableStateOf(favOnly)
    var body by mutableStateOf(body)

    companion object {
        val Saver = listSaver<ExploreUi, Any>(
            save = { listOf(it.seg, it.query, it.muscle, it.level, it.equip, it.favOnly, it.body) },
            restore = { ExploreUi(it[0] as Int, it[1] as String, it[2] as String, it[3] as String, it[4] as String, it[5] as Boolean, it[6] as String) }
        )
    }
}

@Composable
fun rememberExploreUi(): ExploreUi = rememberSaveable(saver = ExploreUi.Saver) { ExploreUi() }

@Composable
fun ExploreScreen(ui: ExploreUi, listState: LazyListState, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp).padding(top = statusTop() + 12.dp)) {
            Txt("EXPLORE", T.hero)
            Spacer(Modifier.height(12.dp))
            Segmented(listOf("Library", "3D body"), ui.seg, { ui.seg = it })
        }
        Spacer(Modifier.height(12.dp))
        AnimatedContent(
            targetState = ui.seg,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "exploreSeg",
            modifier = Modifier.weight(1f)
        ) { s ->
            if (s == 0) LibraryPane(ui, listState, onOpen) else BodyPane(ui, onOpen)
        }
    }
}

// ---------------------------------------------------------------- library

private val equipChips = listOf(
    "Body only" to "body only", "Dumbbell" to "dumbbell", "Barbell" to "barbell",
    "Machine" to "machine", "Cable" to "cable", "Kettlebell" to "kettlebells", "Bands" to "bands"
)

@Composable
private fun LibraryPane(ui: ExploreUi, listState: LazyListState, onOpen: (String) -> Unit) {
    val app = LocalApp.current
    val store = app.store
    val favs = store.favorites.toList()
    val all = app.exercises
    val shown = remember(all, ui.query, ui.muscle, ui.level, ui.equip, ui.favOnly, favs) {
        val q = ui.query.trim().lowercase()
        all.filter { e ->
            (q.isEmpty() || e.name.lowercase().contains(q) || e.primary.any { it.contains(q) }) &&
                (ui.muscle.isEmpty() || ui.muscle in e.primary) &&
                (ui.level.isEmpty() || e.level == ui.level) &&
                (ui.equip.isEmpty() || e.equipment == ui.equip) &&
                (!ui.favOnly || e.id in favs)
        }
    }

    Column(Modifier.fillMaxSize()) {
        SearchField(ui.query, { ui.query = it }, "Search ${all.size} exercises", Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Chip("All muscles", ui.muscle.isEmpty(), { ui.muscle = "" }) }
            items(ALL_MUSCLES) { m ->
                Chip(prettyMuscle(m), ui.muscle == m, { ui.muscle = if (ui.muscle == m) "" else m })
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Chip("Saved", ui.favOnly, { ui.favOnly = !ui.favOnly }) }
            item { Chip("Beginner", ui.level == "beginner", { ui.level = if (ui.level == "beginner") "" else "beginner" }) }
            item { Chip("Inter.", ui.level == "intermediate", { ui.level = if (ui.level == "intermediate") "" else "intermediate" }) }
            item { Chip("Hard", ui.level == "expert", { ui.level = if (ui.level == "expert") "" else "expert" }) }
            items(equipChips) { (label, v) ->
                Chip(label, ui.equip == v, { ui.equip = if (ui.equip == v) "" else v })
            }
        }
        Spacer(Modifier.height(12.dp))
        Txt(
            "${shown.size} EXERCISES", T.label, C.faint,
            Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(8.dp))
        if (all.isEmpty()) {
            Empty("Loading", "Unpacking the exercise library.")
        } else if (shown.isEmpty()) {
            Empty("Nothing here", "Try a different muscle or clear a filter.")
        } else {
            LazyColumn(
                Modifier.fillMaxSize(), state = listState,
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(shown, key = { it.id }) { e ->
                    ExerciseRow(e, e.id in favs, { onOpen(e.id) }, { store.toggleFavorite(e.id) })
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CutSm)
            .background(C.surface)
            .border(1.dp, C.line, CutSm)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = C.dim, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Txt(hint, T.body, C.faint)
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = T.body.copy(color = C.text), cursorBrush = SolidColor(C.volt),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Box(Modifier.pressable { onChange("") }) {
                Icon(Icons.Rounded.Close, null, tint = C.dim, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ExerciseRow(e: Exercise, fav: Boolean, onClick: () -> Unit, onFav: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExerciseImage(e.imageUrl(0), Modifier.size(88.dp).sharedKey("ex-img-${e.id}"))
            Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Txt(e.name, T.h3, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Txt(
                    e.primary.joinToString(" · ") { prettyMuscle(it) }.ifEmpty { "Full body" },
                    T.small, C.dim, maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(e.levelLabel, if (e.level == "expert") C.ember else C.dim)
                    Tag(if (e.equipment == "none") "No gear" else e.equipment)
                }
            }
            Box(Modifier.padding(end = 14.dp).pressable(onClick = onFav)) {
                Icon(
                    if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null,
                    tint = if (fav) C.volt else C.faint, modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------- 3D body

private fun suitsProfile(e: Exercise, level: Level, place: Place): Boolean {
    if (e.category != "strength" && e.category != "powerlifting" && e.category != "plyometrics") return false
    val eqOk = if (place.isHome) e.equipment in place.equipment
    else e.equipment != "other" && e.equipment != "foam roll"
    val lvOk = when (level) {
        Level.BEGINNER -> e.level == "beginner"
        Level.INTERMEDIATE -> e.level != "expert"
        Level.HARD -> true
    }
    return eqOk && lvOk
}

@Composable
private fun BodyPane(ui: ExploreUi, onOpen: (String) -> Unit) {
    val app = LocalApp.current
    val store = app.store
    val p = store.profile
    var gender by remember { mutableStateOf(p.gender) }
    val yaw = rememberYawState()
    val scope = rememberCoroutineScope()
    val heat = store.muscleHeat { app.exercise(it) }
    val sel = ui.body.ifEmpty { null }

    val list = remember(sel, app.exercises, p.level, p.place) {
        if (sel == null) emptyList()
        else app.exercises
            .filter { sel in it.primary && suitsProfile(it, p.level, p.place) }
            .sortedWith(compareByDescending<Exercise> { it.isCompound }.thenBy { it.name })
            .take(24)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.weight(1f)) {
                Segmented(
                    listOf("Male", "Female"), if (gender == Gender.MALE) 0 else 1,
                    { gender = if (it == 0) Gender.MALE else Gender.FEMALE }
                )
            }
            Chip("Front", false, { scope.launch { yaw.animateTo(yaw.nearest(0f)) } })
            Chip("Back", false, { scope.launch { yaw.animateTo(yaw.nearest(180f)) } })
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            BodyMap3D(
                gender = gender, selected = sel, heat = heat,
                onSelect = { ui.body = it ?: "" },
                modifier = Modifier.fillMaxSize(), state = yaw
            )
            Column(Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 12.dp)) {
                Txt(if (sel == null) "TAP A MUSCLE" else prettyMuscle(sel).uppercase(), T.h1, if (sel == null) C.dim else C.volt)
                Txt(
                    if (sel == null) "Drag to spin the body." else "${list.size} moves for your level",
                    T.small, C.dim
                )
            }
            if (heat.isNotEmpty()) {
                Box(Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = 16.dp)) {
                    Tag("Glow = trained this week", C.volt)
                }
            }
        }
        AnimatedVisibility(
            visible = sel != null && list.isNotEmpty(),
            enter = slideInVertically(tween(320)) { it } + fadeIn(tween(240)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160))
        ) {
            LazyRow(
                Modifier.padding(bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list, key = { it.id }) { e ->
                    Panel(Modifier.width(150.dp), onClick = { onOpen(e.id) }) {
                        Column {
                            ExerciseImage(e.imageUrl(0), Modifier.fillMaxWidth().height(96.dp), scrim = true)
                            Column(Modifier.padding(10.dp)) {
                                Txt(e.name, T.h3, maxLines = 2)
                                Spacer(Modifier.height(4.dp))
                                Txt(e.levelLabel.uppercase(), T.label, C.dim)
                            }
                        }
                    }
                }
            }
        }
    }
}
