package app.forge.gym.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import kotlinx.coroutines.delay

@Composable
fun Txt(
    text: String,
    style: TextStyle = T.body,
    color: Color = C.text,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = align
    )
}

/** Springy press feedback instead of a ripple. */
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

/** Fade + rise on first appearance, staggered by index. */
fun Modifier.staggerIn(index: Int): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index * 60L).coerceAtMost(500L))
        a.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    this.graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 40f
    }
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var m = modifier
    if (onClick != null) m = m.pressable(onClick = onClick)
    m = m
        .clip(CutSm)
        .background(if (accent) C.volt else C.surface)
        .border(1.dp, if (accent) C.volt else C.line, CutSm)
    Box(m, content = content)
}

@Composable
fun ForgeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    danger: Boolean = false
) {
    val bg = when {
        !enabled -> C.surface2
        filled && danger -> C.danger
        filled -> C.volt
        else -> Color.Transparent
    }
    val fg = when {
        !enabled -> C.faint
        filled -> C.bg
        danger -> C.danger
        else -> C.text
    }
    var m = modifier.pressable(enabled = enabled, onClick = onClick).clip(Cut).background(bg)
    if (!filled) m = m.border(1.dp, if (danger) C.danger else C.line, Cut)
    Row(
        m.padding(horizontal = 22.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Txt(text.uppercase(), T.btn, fg)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (selected) C.volt else C.surface, tween(200), label = "chipBg")
    val fg by animateColorAsState(if (selected) C.bg else C.dim, tween(200), label = "chipFg")
    Box(
        modifier
            .pressable(onClick = onClick)
            .clip(CutXs)
            .background(bg)
            .border(1.dp, if (selected) C.volt else C.line, CutXs)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Txt(text.uppercase(), T.label, fg)
    }
}

@Composable
fun Tag(text: String, color: Color = C.dim) {
    Box(
        Modifier
            .clip(CutXs)
            .border(1.dp, C.line, CutXs)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Txt(text.uppercase(), T.label.copy(fontSize = 11.sp), color)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 14.dp, height = 3.dp).background(C.volt))
            Spacer(Modifier.width(8.dp))
            Txt(text.uppercase(), T.label, C.dim)
        }
        if (action != null) {
            Box(Modifier.pressable(onClick = onAction)) { Txt(action.uppercase(), T.label, C.volt) }
        }
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CutSm)
            .background(C.surface)
            .border(1.dp, C.line, CutSm)
            .padding(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            val bg by animateColorAsState(if (sel) C.volt else Color.Transparent, tween(220), label = "segBg")
            val fg by animateColorAsState(if (sel) C.bg else C.dim, tween(220), label = "segFg")
            Box(
                Modifier
                    .weight(1f)
                    .clip(CutXs)
                    .background(bg)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) { Txt(label.uppercase(), T.label, fg) }
        }
    }
}

@Composable
fun Ring(
    progress: Float,
    diameter: Dp,
    stroke: Dp,
    modifier: Modifier = Modifier,
    color: Color = C.volt,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "ring")
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val topLeft = Offset(sw / 2, sw / 2)
            val arc = Size(this.size.width - sw, this.size.height - sw)
            drawArc(C.line, -90f, 360f, false, topLeft = topLeft, size = arc, style = Stroke(sw))
            if (p > 0.001f) {
                drawArc(color, -90f, 360f * p, false, topLeft = topLeft, size = arc, style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

@Composable
fun Stepper(
    value: String,
    unit: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        StepBtn(Icons.Rounded.Remove) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onMinus() }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.Bottom) {
                Txt(value, T.num)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Txt(unit.uppercase(), T.label, C.dim, Modifier.padding(bottom = 6.dp))
                }
            }
        }
        StepBtn(Icons.Rounded.Add) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPlus() }
    }
}

@Composable
private fun StepBtn(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .pressable(onClick = onClick)
            .clip(CutSm)
            .background(C.surface2)
            .border(1.dp, C.line, CutSm),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = C.text, modifier = Modifier.size(24.dp)) }
}

@Composable
fun IconBox(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = C.text, filled: Boolean = false) {
    Box(
        modifier
            .size(44.dp)
            .pressable(onClick = onClick)
            .clip(CutXs)
            .background(if (filled) C.volt else C.surface)
            .border(1.dp, if (filled) C.volt else C.line, CutXs),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = if (filled) C.bg else tint, modifier = Modifier.size(22.dp)) }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(C.line))
}

@Composable
fun Empty(title: String, body: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Txt(title.uppercase(), T.h2, C.text, align = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Txt(body, T.body, C.dim, align = TextAlign.Center)
        }
    }
}
