package app.forge.gym.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import app.forge.gym.ui.C
import app.forge.gym.ui.CutSm
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt
import app.forge.gym.ui.pressable

@Composable
fun statusTop() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

@Composable
fun NameField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= 24) onChange(it) },
        singleLine = true,
        textStyle = T.h2.copy(color = C.text),
        cursorBrush = SolidColor(C.volt),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().clip(CutSm).background(C.surface).border(1.dp, if (value.isBlank()) C.line else C.volt, CutSm)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                if (value.isEmpty()) Txt("Your name", T.h2, C.faint)
                inner()
            }
        }
    )
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CutSm)
            .background(C.surface)
            .border(1.dp, C.line, CutSm)
            .padding(horizontal = 14.dp, vertical = 14.dp),
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

/** A plain one-line text box (routine and workout names). */
@Composable
fun TitleField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= 40) onChange(it) },
        singleLine = true,
        textStyle = T.h2.copy(color = C.text),
        cursorBrush = SolidColor(C.volt),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().clip(CutSm).background(C.surface).border(1.dp, C.line, CutSm).padding(horizontal = 14.dp, vertical = 12.dp)) {
                if (value.isEmpty()) Txt(hint, T.h2, C.faint, maxLines = 1)
                inner()
            }
        }
    )
}
