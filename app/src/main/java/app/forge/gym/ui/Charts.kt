package app.forge.gym.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
private fun rememberReveal(): Float {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val k by animateFloatAsState(if (go) 1f else 0f, tween(900, easing = FastOutSlowInEasing), label = "reveal")
    return k
}

/** Seven-ish bars with labels; the last (or [highlight]) bar is volt, the rest are dim. */
@Composable
fun WeekBars(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    highlight: Int = values.lastIndex,
    goal: Float? = null,
    height: androidx.compose.ui.unit.Dp = 120.dp
) {
    val k = rememberReveal()
    val top = max(values.maxOrNull() ?: 1f, goal ?: 0f).coerceAtLeast(1f)
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val n = values.size
            if (n == 0) return@Canvas
            val gap = 10.dp.toPx()
            val bw = (size.width - gap * (n - 1)) / n
            if (goal != null) {
                val gy = size.height - size.height * (goal / top)
                drawLine(C.faint, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx())
            }
            values.forEachIndexed { i, v ->
                val h = (size.height * (v / top) * k).coerceAtLeast(if (v > 0f) 4.dp.toPx() else 2.dp.toPx())
                val x = i * (bw + gap)
                val c = if (i == highlight) C.volt else if (v > 0f) C.dim.copy(alpha = 0.55f) else C.line
                drawRoundRect(c, Offset(x, size.height - h), Size(bw, h), CornerRadius(3.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { i, l ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Txt(l.uppercase(), T.label.copy(fontSize = 11.sp), if (i == highlight) C.text else C.faint, align = TextAlign.Center)
                }
            }
        }
    }
}

/** Smooth line with a fill underneath, drawn in on first appearance. */
@Composable
fun LineChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
    goal: Float? = null,
    height: androidx.compose.ui.unit.Dp = 170.dp
) {
    val k = rememberReveal()
    Canvas(modifier.fillMaxWidth().height(height)) {
        if (points.isEmpty()) return@Canvas
        val all = if (goal != null && goal > 0f) points + goal else points
        var lo = all.minOrNull() ?: 0f
        var hi = all.maxOrNull() ?: 1f
        if (hi - lo < 1f) { hi += 0.5f; lo -= 0.5f }
        val pad = (hi - lo) * 0.18f
        lo -= pad; hi += pad
        val padX = 6.dp.toPx()
        val w = size.width - padX * 2
        fun px(i: Int): Float = if (points.size == 1) size.width / 2f else padX + w * i / (points.size - 1)
        fun py(v: Float): Float = size.height - size.height * ((v - lo) / (hi - lo))

        if (goal != null && goal > 0f) {
            val gy = py(goal)
            var x = 0f
            while (x < size.width) {
                drawLine(C.faint, Offset(x, gy), Offset(minOf(x + 8.dp.toPx(), size.width), gy), strokeWidth = 1.dp.toPx())
                x += 14.dp.toPx()
            }
        }
        if (points.size == 1) {
            drawCircle(C.volt.copy(alpha = 0.25f), 14.dp.toPx(), Offset(px(0), py(points[0])))
            drawCircle(C.volt, 6.dp.toPx(), Offset(px(0), py(points[0])))
            return@Canvas
        }
        val line = Path()
        line.moveTo(px(0), py(points[0]))
        for (i in 1 until points.size) {
            val x0 = px(i - 1); val y0 = py(points[i - 1])
            val x1 = px(i); val y1 = py(points[i])
            val mx = (x0 + x1) / 2f
            line.cubicTo(mx, y0, mx, y1, x1, y1)
        }
        val fill = Path()
        fill.addPath(line)
        fill.lineTo(px(points.lastIndex), size.height)
        fill.lineTo(px(0), size.height)
        fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(C.volt.copy(alpha = 0.28f * k), Color.Transparent)))

        val pm = PathMeasure()
        pm.setPath(line, false)
        val part = Path()
        pm.getSegment(0f, pm.length * k, part, true)
        drawPath(part, C.volt, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        val lx = px(points.lastIndex); val ly = py(points.last())
        drawCircle(C.volt.copy(alpha = 0.22f * k), 13.dp.toPx(), Offset(lx, ly))
        drawCircle(C.volt, 5.dp.toPx() * k, Offset(lx, ly))
    }
}
