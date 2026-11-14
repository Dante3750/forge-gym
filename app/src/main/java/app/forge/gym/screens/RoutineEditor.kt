package app.forge.gym.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.forge.gym.LocalApp
import app.forge.gym.data.Routine
import app.forge.gym.data.RoutineItem
import app.forge.gym.ui.C
import app.forge.gym.ui.ExerciseImage
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.IconBox
import app.forge.gym.ui.Panel
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt
import app.forge.gym.ui.pressable

/** The routine being made or changed. Lives above the screens so the exercise picker can add to it. */
class RoutineDraft {
    var id by mutableStateOf(0L)
    var title by mutableStateOf("")
    val items = mutableStateListOf<RoutineItem>()

    fun load(r: Routine?) {
        id = r?.id ?: 0L
        title = r?.title ?: ""
        items.clear()
        if (r != null) items.addAll(r.items)
    }
}

@Composable
fun RoutineEditorScreen(draft: RoutineDraft, onBack: () -> Unit, onAdd: () -> Unit) {
    val store = LocalApp.current.store
    val app = LocalApp.current
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = statusTop() + 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBox(Icons.AutoMirrored.Rounded.ArrowBack, onBack)
            Spacer(Modifier.width(14.dp))
            Txt(if (draft.id == 0L) "NEW ROUTINE" else "EDIT ROUTINE", T.h1)
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                TitleField(draft.title, { draft.title = it }, "Routine name, e.g. Chest day")
            }
            if (draft.items.isEmpty()) {
                item { Txt("Add the exercises you do on this day, in order.", T.body, C.dim, Modifier.padding(vertical = 8.dp)) }
            }
            itemsIndexed(draft.items.toList()) { i, item ->
                Panel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExerciseImage(app.thumbFor(app.exercise(item.exerciseId)), Modifier.size(64.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Txt(item.name, T.h3, maxLines = 2)
                            Spacer(Modifier.size(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SmallBtn(Icons.Rounded.Remove) { if (item.sets > 1) draft.items[i] = item.copy(sets = item.sets - 1) }
                                Txt("${item.sets} sets", T.bodyM, C.text, Modifier.padding(horizontal = 12.dp))
                                SmallBtn(Icons.Rounded.Add) { if (item.sets < 10) draft.items[i] = item.copy(sets = item.sets + 1) }
                            }
                        }
                        Box(Modifier.pressable { draft.items.removeAt(i) }.padding(14.dp)) {
                            Icon(Icons.Rounded.Close, null, tint = C.dim, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            item {
                ForgeButton("Add exercise", onAdd, Modifier.fillMaxWidth(), filled = false, icon = Icons.Rounded.Add)
            }
            if (draft.id != 0L) {
                item {
                    ForgeButton("Delete routine", { store.deleteRoutine(draft.id); onBack() }, Modifier.fillMaxWidth(), filled = false, danger = true)
                }
            }
        }
        ForgeButton(
            "Save routine",
            {
                store.saveRoutine(Routine(if (draft.id == 0L) System.currentTimeMillis() else draft.id, draft.title.trim().ifEmpty { "My routine" }, draft.items.toList()))
                onBack()
            },
            Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).fillMaxWidth().navigationBarsPadding(),
            enabled = draft.items.isNotEmpty()
        )
    }
}

@Composable
fun SmallBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconBox(icon, onClick, Modifier.size(34.dp))
}
