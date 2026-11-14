package app.forge.gym.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.forge.gym.LocalApp
import app.forge.gym.data.Gender
import app.forge.gym.data.prettyMuscle
import app.forge.gym.ui.C
import app.forge.gym.ui.Chip
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.IconBox
import app.forge.gym.ui.PhotoBodyMap
import app.forge.gym.ui.Segmented
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt
import app.forge.gym.ui.rememberYawState
import kotlinx.coroutines.launch

/** Tap a muscle on the turning body, then jump to its exercises. */
@Composable
fun BodyPickerScreen(onBack: () -> Unit, onPick: (String) -> Unit) {
    val app = LocalApp.current
    var female by rememberSaveable { mutableStateOf(app.store.profile.gender == Gender.FEMALE) }
    var sel by rememberSaveable { mutableStateOf("") }
    val yaw = rememberYawState()
    val scope = rememberCoroutineScope()
    val all = app.exercises
    val count = remember(sel, all) { if (sel.isEmpty()) 0 else all.count { sel in it.primary } }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = statusTop() + 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBox(Icons.AutoMirrored.Rounded.ArrowBack, onBack)
            Spacer(Modifier.width(14.dp))
            Column {
                Txt(if (sel.isEmpty()) "TAP A MUSCLE" else prettyMuscle(sel).uppercase(), T.h1, if (sel.isEmpty()) C.text else C.volt)
                Txt("Drag to turn the body", T.small, C.dim)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.weight(1f)) {
                Segmented(listOf("Man", "Woman"), if (female) 1 else 0, { female = it == 1 })
            }
            Chip("Front", false, { scope.launch { yaw.animateTo(yaw.nearest(0f)) } })
            Chip("Back", false, { scope.launch { yaw.animateTo(yaw.nearest(180f)) } })
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            PhotoBodyMap(
                gender = if (female) Gender.FEMALE else Gender.MALE,
                selected = sel.ifEmpty { null }, heat = emptyMap(),
                onSelect = { sel = it ?: "" },
                modifier = Modifier.fillMaxSize(), state = yaw
            )
        }
        ForgeButton(
            if (sel.isEmpty()) "Tap a muscle" else "See $count ${prettyMuscle(sel)} exercises",
            { if (sel.isNotEmpty()) onPick(sel) },
            Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).fillMaxWidth().navigationBarsPadding(),
            enabled = sel.isNotEmpty()
        )
    }
}
