package app.forge.gym.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import app.forge.gym.LocalApp
import app.forge.gym.data.Gender
import app.forge.gym.data.Goal
import app.forge.gym.data.Level
import app.forge.gym.data.Place
import app.forge.gym.ui.C
import app.forge.gym.ui.CutSm
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.IconBox
import app.forge.gym.ui.Panel
import app.forge.gym.ui.SectionLabel
import app.forge.gym.ui.Segmented
import app.forge.gym.ui.Stepper
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt

@Composable
fun NameField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= 24) onChange(it) },
        singleLine = true,
        textStyle = T.h3.copy(color = C.text),
        cursorBrush = SolidColor(C.volt),
        keyboardOptions = KeyboardOptions(),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().clip(CutSm).border(1.dp, C.line, CutSm).padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (value.isEmpty()) Txt("Your name", T.h3, C.faint)
                inner()
            }
        }
    )
}

@Composable
fun OptionRow(title: String, blurb: String, selected: Boolean, onClick: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), accent = selected, onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Txt(title.uppercase(), T.h3, if (selected) C.bg else C.text)
            Txt(blurb, T.small, if (selected) C.bg else C.dim)
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalApp.current
    val store = app.store
    val p = store.profile
    var confirmReset by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusTop() + 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(Icons.AutoMirrored.Rounded.ArrowBack, onBack)
                Spacer(Modifier.width(14.dp))
                Txt("SETTINGS", T.h1)
            }
        }
        item { SectionLabel("You") }
        item { NameField(p.name, { store.updateProfile(p.copy(name = it)) }) }
        item { Segmented(Gender.values().map { it.label }, p.gender.ordinal, { store.updateProfile(p.copy(gender = Gender.values()[it])) }) }
        item { SectionLabel("Level") }
        items3(Level.values().toList()) { l -> OptionRow(l.label, l.blurb, p.level == l) { store.updateProfile(p.copy(level = l)) } }
        item { SectionLabel("Goal") }
        items3(Goal.values().toList()) { g -> OptionRow(g.label, g.blurb, p.goal == g) { store.updateProfile(p.copy(goal = g)) } }
        item { SectionLabel("Where you train") }
        item { Segmented(Place.values().map { it.label }, p.place.ordinal, { store.updateProfile(p.copy(place = Place.values()[it])) }) }
        item { SectionLabel("Days per week") }
        item {
            Stepper("${p.daysPerWeek}", "days", { store.updateProfile(p.copy(daysPerWeek = (p.daysPerWeek - 1).coerceAtLeast(2))) },
                { store.updateProfile(p.copy(daysPerWeek = (p.daysPerWeek + 1).coerceAtMost(6))) })
        }
        item { SectionLabel("Height") }
        item {
            Stepper("${p.heightCm}", "cm", { store.updateProfile(p.copy(heightCm = (p.heightCm - 1).coerceAtLeast(120))) },
                { store.updateProfile(p.copy(heightCm = (p.heightCm + 1).coerceAtMost(220))) })
        }
        item { SectionLabel("Units") }
        item { Segmented(listOf("kg", "lb"), if (p.useKg) 0 else 1, { store.updateProfile(p.copy(useKg = it == 0)) }) }
        item { SectionLabel("Daily targets") }
        item {
            Stepper("${p.stepGoal}", "steps", { store.updateProfile(p.copy(stepGoal = (p.stepGoal - 500).coerceAtLeast(1000))) },
                { store.updateProfile(p.copy(stepGoal = (p.stepGoal + 500).coerceAtMost(40000))) })
        }
        item {
            Stepper("${p.waterGoalMl}", "ml water", { store.updateProfile(p.copy(waterGoalMl = (p.waterGoalMl - 250).coerceAtLeast(500))) },
                { store.updateProfile(p.copy(waterGoalMl = (p.waterGoalMl + 250).coerceAtMost(6000))) })
        }
        item {
            val shown = if (p.goalWeightKg <= 0f) 0f else p.goalWeightKg * (if (p.useKg) 1f else 2.20462f)
            Stepper(if (p.goalWeightKg <= 0f) "off" else "%.1f".format(shown), if (p.useKg) "kg goal" else "lb goal",
                { val k = if (p.goalWeightKg <= 0f) 70f else p.goalWeightKg; store.updateProfile(p.copy(goalWeightKg = (k - 0.5f).coerceAtLeast(30f))) },
                { val k = if (p.goalWeightKg <= 0f) 70f else p.goalWeightKg; store.updateProfile(p.copy(goalWeightKg = (k + 0.5f).coerceAtMost(250f))) })
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            ForgeButton("Shuffle my plan", { store.updateProfile(p.copy(planSeed = p.planSeed + 1)) }, Modifier.fillMaxWidth(), filled = false)
        }
        item {
            ForgeButton("Erase all data", { confirmReset = true }, Modifier.fillMaxWidth(), filled = false, danger = true)
        }
        item { Txt("Forge keeps everything on this phone. No account, no server.", T.small, C.faint) }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = C.surface,
            title = { Text("Erase everything?", color = C.text) },
            text = { Text("Workouts, weights, walks and your profile will be deleted.", color = C.dim) },
            confirmButton = { TextButton({ store.resetAll(); confirmReset = false }) { Text("Erase", color = C.danger) } },
            dismissButton = { TextButton({ confirmReset = false }) { Text("Cancel", color = C.dim) } }
        )
    }
}

private fun <T> androidx.compose.foundation.lazy.LazyListScope.items3(list: List<T>, content: @Composable (T) -> Unit) {
    items(list.size) { content(list[it]) }
}
