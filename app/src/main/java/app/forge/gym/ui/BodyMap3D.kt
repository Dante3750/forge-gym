package app.forge.gym.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.lerp
import app.forge.gym.data.Gender
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A real-time 3D body drawn straight onto a Compose Canvas.
 * Every part is an ellipsoid or a capsule in metres (x right, y up, z forward). Each frame the parts are
 * rotated about the vertical axis, projected, depth sorted and shaded. Ellipsoids and capsules keep their exact
 * silhouette under that rotation, so no mesh or model file is needed. Taps hit-test the same projected shapes,
 * so a muscle hidden behind the torso can't be selected.
 */
class YawState {
    var yaw by mutableFloatStateOf(0f)
    var flingJob: Job? = null

    suspend fun animateTo(target: Float) {
        animate(yaw, target, animationSpec = spring(dampingRatio = 0.75f, stiffness = 120f)) { v, _ -> yaw = v }
    }

    suspend fun fling(velocity: Float) {
        animateDecay(yaw, velocity, FloatExponentialDecaySpec(1.4f)) { v, _ -> yaw = v }
    }

    /** Nearest angle that shows the front (offset 0) or the back (offset 180). */
    fun nearest(offset: Float): Float = offset + 360f * ((yaw - offset) / 360f).roundToInt()
}

@Composable
fun rememberYawState(): YawState = remember { YawState() }

private class Part(
    val muscle: String?,
    val cap: Boolean,
    val x1: Float, val y1: Float, val z1: Float,
    val x2: Float, val y2: Float, val z2: Float,
    val a: Float, val b: Float, val c: Float
)

private fun ell(m: String?, x: Float, y: Float, z: Float, a: Float, b: Float, c: Float) =
    Part(m, false, x, y, z, 0f, 0f, 0f, a, b, c)

private fun cap(m: String?, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, r: Float) =
    Part(m, true, x1, y1, z1, x2, y2, z2, r, r, r)

private fun shapeFactor(y: Float, g: Gender): Float {
    val upper = if (g == Gender.MALE) 1.08f else 0.92f
    val hips = if (g == Gender.MALE) 0.96f else 1.12f
    return when {
        y >= 1.32f -> upper
        y >= 1.12f -> lerp(1f, upper, (y - 1.12f) / 0.20f)
        y >= 0.98f -> lerp(hips, 1f, (y - 0.98f) / 0.14f)
        y >= 0.80f -> hips
        y >= 0.60f -> lerp(1f, hips, (y - 0.60f) / 0.20f)
        else -> 1f
    }
}

private fun MutableList<Part>.sym(f: (Float) -> Part) {
    add(f(1f))
    add(f(-1f))
}

private fun buildBody(g: Gender): List<Part> {
    val raw = ArrayList<Part>()
    // skin (not selectable)
    raw += ell(null, 0f, 1.69f, 0.005f, .085f, .105f, .098f)
    raw += ell(null, 0f, 1.31f, 0f, .185f, .20f, .105f)
    raw += ell(null, 0f, 1.06f, 0f, .15f, .19f, .095f)
    raw += ell(null, 0f, .93f, 0f, .165f, .09f, .10f)
    raw.sym { s -> cap(null, s * .235f, 1.42f, 0f, s * .27f, 1.17f, 0f, .036f) }
    raw.sym { s -> ell(null, s * .315f, .88f, 0f, .032f, .05f, .025f) }
    raw.sym { s -> cap(null, s * .085f, .90f, 0f, s * .09f, .50f, 0f, .072f) }
    raw.sym { s -> cap(null, s * .09f, .50f, 0f, s * .092f, .09f, 0f, .046f) }
    raw.sym { s -> ell(null, s * .092f, .04f, .045f, .04f, .03f, .085f) }
    // muscles
    raw += cap("neck", 0f, 1.60f, .005f, 0f, 1.53f, .005f, .046f)
    raw.sym { s -> ell("traps", s * .075f, 1.51f, -.03f, .075f, .045f, .055f) }
    raw.sym { s -> ell("shoulders", s * .215f, 1.45f, 0f, .062f, .065f, .062f) }
    raw.sym { s -> ell("chest", s * .075f, 1.35f, .065f, .082f, .065f, .05f) }
    raw += ell("abdominals", 0f, 1.24f, .082f, .075f, .045f, .03f)
    raw += ell("abdominals", 0f, 1.15f, .082f, .075f, .045f, .03f)
    raw += ell("abdominals", 0f, 1.06f, .082f, .07f, .045f, .03f)
    raw.sym { s -> ell("abdominals", s * .125f, 1.14f, .05f, .034f, .10f, .04f) }
    raw.sym { s -> ell("lats", s * .135f, 1.30f, -.07f, .062f, .13f, .045f) }
    raw += ell("middle back", 0f, 1.34f, -.09f, .09f, .06f, .03f)
    raw += ell("lower back", 0f, 1.10f, -.085f, .09f, .06f, .03f)
    raw.sym { s -> ell("glutes", s * .075f, .94f, -.075f, .08f, .08f, .06f) }
    raw.sym { s -> ell("biceps", s * .252f, 1.30f, .035f, .04f, .085f, .034f) }
    raw.sym { s -> ell("triceps", s * .252f, 1.30f, -.035f, .04f, .085f, .034f) }
    raw.sym { s -> cap("forearms", s * .272f, 1.16f, 0f, s * .312f, .93f, 0f, .033f) }
    raw.sym { s -> ell("abductors", s * .165f, .96f, 0f, .04f, .07f, .05f) }
    raw.sym { s -> ell("adductors", s * .045f, .74f, 0f, .04f, .10f, .045f) }
    raw.sym { s -> ell("quadriceps", s * .088f, .72f, .048f, .062f, .165f, .045f) }
    raw.sym { s -> ell("hamstrings", s * .088f, .72f, -.048f, .062f, .165f, .045f) }
    raw.sym { s -> ell("calves", s * .09f, .33f, -.038f, .046f, .10f, .04f) }

    // male: broader shoulders. female: wider hips. Applied to x positions of every part.
    return raw.map { p ->
        if (p.cap) {
            Part(p.muscle, true, p.x1 * shapeFactor(p.y1, g), p.y1, p.z1, p.x2 * shapeFactor(p.y2, g), p.y2, p.z2, p.a, p.b, p.c)
        } else {
            val f = shapeFactor(p.y1, g)
            Part(p.muscle, false, p.x1 * f, p.y1, p.z1, 0f, 0f, 0f, p.a * f, p.b, p.c)
        }
    }
}

private class Proj(
    val part: Part,
    val cx: Float, val cy: Float, val rx: Float, val ry: Float,   // ellipse
    val ax: Float, val ay: Float, val bx: Float, val by: Float, val w: Float, // capsule
    val z: Float
)

private class Layout(val scale: Float, val ox: Float, val floorY: Float)

private fun layoutFor(size: Size): Layout {
    val scale = min(size.height * 0.95f / 1.82f, size.width * 0.92f / 0.80f)
    return Layout(scale, size.width / 2f, size.height * 0.965f)
}

private fun project(parts: List<Part>, size: Size, yawDeg: Float): Pair<List<Proj>, Layout> {
    val lay = layoutFor(size)
    val t = (yawDeg * PI / 180.0).toFloat()
    val cs = cos(t)
    val sn = sin(t)
    val out = ArrayList<Proj>(parts.size)
    for (p in parts) {
        if (!p.cap) {
            val cxm = p.x1 * cs + p.z1 * sn
            val zr = -p.x1 * sn + p.z1 * cs
            val rxm = sqrt((p.a * cs) * (p.a * cs) + (p.c * sn) * (p.c * sn))
            out += Proj(p, lay.ox + cxm * lay.scale, lay.floorY - p.y1 * lay.scale, rxm * lay.scale, p.b * lay.scale, 0f, 0f, 0f, 0f, 0f, zr)
        } else {
            val ax = p.x1 * cs + p.z1 * sn
            val bx = p.x2 * cs + p.z2 * sn
            val za = -p.x1 * sn + p.z1 * cs
            val zb = -p.x2 * sn + p.z2 * cs
            out += Proj(
                p, 0f, 0f, 0f, 0f,
                lay.ox + ax * lay.scale, lay.floorY - p.y1 * lay.scale,
                lay.ox + bx * lay.scale, lay.floorY - p.y2 * lay.scale,
                p.a * 2f * lay.scale, (za + zb) / 2f
            )
        }
    }
    out.sortBy { it.z }
    return Pair(out, lay)
}

private fun hit(pr: Proj, px: Float, py: Float): Boolean {
    if (!pr.part.cap) {
        val dx = (px - pr.cx) / pr.rx
        val dy = (py - pr.cy) / pr.ry
        return dx * dx + dy * dy <= 1f
    }
    val vx = pr.bx - pr.ax
    val vy = pr.by - pr.ay
    val len2 = vx * vx + vy * vy
    val tt = if (len2 == 0f) 0f else (((px - pr.ax) * vx + (py - pr.ay) * vy) / len2).coerceIn(0f, 1f)
    val qx = pr.ax + vx * tt
    val qy = pr.ay + vy * tt
    val dx = px - qx
    val dy = py - qy
    return dx * dx + dy * dy <= (pr.w / 2f) * (pr.w / 2f)
}

private fun Color.shade(f: Float) = Color(red * f, green * f, blue * f, alpha)

private val skinBase = Color(0xFF23272E)
private val muscleBase = Color(0xFF3A404A)
private val edgeColor = Color(0xFF0E1013)

private fun DrawScope.drawPart(pr: Proj, selected: Boolean, heat: Float) {
    val depth = ((pr.z + 0.14f) / 0.28f).coerceIn(0f, 1f)
    val isMuscle = pr.part.muscle != null
    var base = if (isMuscle) muscleBase else skinBase
    if (isMuscle && heat > 0f) base = colorLerp(base, C.voltDeep, heat * 0.85f)
    if (selected) base = C.volt
    val lit = colorLerp(base, Color.White, if (selected) 0.35f else 0.14f).shade(0.8f + 0.2f * depth)
    val body = base.shade(0.72f + 0.28f * depth)
    if (!pr.part.cap) {
        val tl = Offset(pr.cx - pr.rx, pr.cy - pr.ry)
        val sz = Size(pr.rx * 2f, pr.ry * 2f)
        if (selected) {
            drawOval(C.volt.copy(alpha = 0.20f), Offset(tl.x - 7f, tl.y - 7f), Size(sz.width + 14f, sz.height + 14f))
        }
        val brush = Brush.radialGradient(
            colors = listOf(lit, body),
            center = Offset(pr.cx - pr.rx * 0.3f, pr.cy - pr.ry * 0.38f),
            radius = max(pr.rx, pr.ry) * 1.6f
        )
        drawOval(brush, tl, sz)
        drawOval(edgeColor, tl, sz, style = Stroke(1.3f))
    } else {
        val a = Offset(pr.ax, pr.ay)
        val b = Offset(pr.bx, pr.by)
        if (selected) drawLine(C.volt.copy(alpha = 0.20f), a, b, pr.w + 14f, StrokeCap.Round)
        drawLine(edgeColor, a, b, pr.w + 2.6f, StrokeCap.Round)
        val minX = min(pr.ax, pr.bx) - pr.w / 2f
        val maxX = max(pr.ax, pr.bx) + pr.w / 2f
        val brush = Brush.horizontalGradient(listOf(body, lit, body), startX = minX, endX = maxX)
        drawLine(brush, a, b, pr.w, StrokeCap.Round)
    }
}

@Composable
fun BodyMap3D(
    gender: Gender,
    selected: String?,
    heat: Map<String, Float>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    autoSpin: Boolean = false,
    state: YawState = rememberYawState()
) {
    val parts = remember(gender) { buildBody(gender) }

    LaunchedEffect(Unit) {
        if (interactive && !autoSpin) {
            state.yaw = -75f
            state.animateTo(0f)
        }
    }

    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "spinValue"
    )

    var m = modifier
    if (interactive) {
        m = m
            .draggable(
                state = rememberDraggableState { d -> state.yaw += d * 0.55f },
                orientation = Orientation.Horizontal,
                onDragStarted = { state.flingJob?.cancel() },
                onDragStopped = { v -> state.flingJob = launch { state.fling(v * 0.45f) } }
            )
            .pointerInput(gender) {
                detectTapGestures { off ->
                    val (list, _) = project(parts, Size(size.width.toFloat(), size.height.toFloat()), state.yaw)
                    var found: String? = null
                    for (i in list.indices.reversed()) {
                        if (hit(list[i], off.x, off.y)) {
                            found = list[i].part.muscle
                            break
                        }
                    }
                    onSelect(found)
                }
            }
    }

    Canvas(m) {
        val yaw = if (autoSpin) spin else state.yaw
        val (list, lay) = project(parts, size, yaw)

        // turntable: soft pool of light and a ring whose ticks travel as the body turns
        val rxp = 0.40f * lay.scale
        val ryp = 0.085f * lay.scale
        drawOval(
            Brush.radialGradient(listOf(C.volt.copy(alpha = 0.16f), Color.Transparent), center = Offset(lay.ox, lay.floorY), radius = rxp * 1.2f),
            Offset(lay.ox - rxp * 1.2f, lay.floorY - ryp * 1.6f), Size(rxp * 2.4f, ryp * 3.2f)
        )
        drawOval(C.line, Offset(lay.ox - rxp, lay.floorY - ryp), Size(rxp * 2f, ryp * 2f), style = Stroke(1.5f))
        val ticks = 36
        for (i in 0 until ticks) {
            val phi = (i * 2.0 * PI / ticks + yaw * PI / 180.0).toFloat()
            val tx = lay.ox + rxp * cos(phi)
            val ty = lay.floorY + ryp * sin(phi)
            val near = (sin(phi) + 1f) / 2f
            drawCircle(C.volt.copy(alpha = 0.10f + 0.55f * near), 1.2f + 1.6f * near, Offset(tx, ty))
        }

        for (pr in list) {
            val muscle = pr.part.muscle
            drawPart(pr, muscle != null && muscle == selected, if (muscle != null) (heat[muscle] ?: 0f) else 0f)
        }
    }
}
