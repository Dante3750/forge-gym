package app.forge.gym.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.forge.gym.data.Gender
import app.forge.gym.viewer.Models3D
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The realistic human turning on a turntable, with tappable muscles.
 * Frames are photo renders of the same human as the trainer (24 angles). Under each frame is a muscle-ID image:
 * every pixel says which muscle is there, so taps hit-test exactly and the selected / trained muscles are painted
 * straight onto the photo. Falls back to the drawn body until the renders have downloaded.
 */
private class BodyMapData(
    val beauty: BitmapRegionDecoder, val mask: BitmapRegionDecoder,
    val muscles: List<String>, val frames: Int, val cols: Int, val w: Int, val h: Int
) {
    private val cache = LinkedHashMap<Int, Pair<ImageBitmap, IntArray>>(8, 0.75f, true)

    fun rect(f: Int) = Rect((f % cols) * w, (f / cols) * h, (f % cols) * w + w, (f / cols) * h + h)

    @Synchronized
    fun frame(f: Int): Pair<ImageBitmap, IntArray> {
        cache[f]?.let { return it }
        val o = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val b = beauty.decodeRegion(rect(f), o)
        val m = mask.decodeRegion(rect(f), o)
        val px = IntArray(w * h); m.getPixels(px, 0, w, 0, 0, w, h); m.recycle()
        for (i in px.indices) px[i] = (px[i] shr 16) and 0xFF      // grey value = muscle id (0 = none)
        val v = b.asImageBitmap() to px
        cache[f] = v
        while (cache.size > 6) cache.remove(cache.keys.first())
        return v
    }
}

private suspend fun loadBodyMap(ctx: android.content.Context, g: String): BodyMapData? = withContext(Dispatchers.IO) {
    runCatching {
        val meta = Models3D.fetchFile(ctx, "bodymap.json") ?: return@runCatching null
        val b = Models3D.fetchFile(ctx, "bodymap_$g.webp") ?: return@runCatching null
        val m = Models3D.fetchFile(ctx, "bodymask_$g.png") ?: return@runCatching null
        val j = JSONObject(meta.readText())
        val arr = j.getJSONArray("muscles")
        @Suppress("DEPRECATION")
        BodyMapData(
            BitmapRegionDecoder.newInstance(b.absolutePath, false)!!, BitmapRegionDecoder.newInstance(m.absolutePath, false)!!,
            List(arr.length()) { arr.getString(it) }, j.getInt("frames"), j.getInt("cols"), j.getInt("w"), j.getInt("h")
        )
    }.getOrNull()
}

@Composable
fun PhotoBodyMap(
    gender: Gender,
    selected: String?,
    heat: Map<String, Float>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    state: YawState = rememberYawState(),
    autoSpin: Boolean = false
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var data by remember(gender) { mutableStateOf<BodyMapData?>(null) }
    LaunchedEffect(gender) { data = loadBodyMap(ctx, if (gender == Gender.FEMALE) "f" else "m") }
    val d = data
    if (d == null) {
        BodyMap3D(gender, selected, heat, onSelect, modifier, interactive = !autoSpin, autoSpin = autoSpin, state = state)
        return
    }
    val spin by androidx.compose.animation.core.rememberInfiniteTransition(label = "spin").animateFloat(
        0f, -360f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(18000, easing = androidx.compose.animation.core.LinearEasing)), label = "spin"
    )
    val yawNow = if (autoSpin) spin else state.yaw
    // yaw (degrees) -> two neighbouring frames and the blend between them
    val step = 360f / d.frames
    val pos = ((-yawNow / step) % d.frames + d.frames) % d.frames
    val f0 = floor(pos).toInt() % d.frames
    val f1 = (f0 + 1) % d.frames
    val t = pos - floor(pos)
    val near = if (t < 0.5f) f0 else f1

    var shown by remember(d) { mutableStateOf<Triple<Int, Pair<ImageBitmap, IntArray>, Pair<ImageBitmap, IntArray>>?>(null) }
    var overlay by remember(d) { mutableStateOf<Pair<Int, ImageBitmap>?>(null) }
    LaunchedEffect(d, f0) {
        val a = withContext(Dispatchers.Default) { d.frame(f0) }
        val b = withContext(Dispatchers.Default) { d.frame(f1) }
        shown = Triple(f0, a, b)
    }
    LaunchedEffect(d, near, selected, heat) {
        val ids = withContext(Dispatchers.Default) { d.frame(near).second }
        val sel = selected?.let { d.muscles.indexOf(it) + 1 } ?: -1
        val heatById = FloatArray(d.muscles.size + 1) { i -> if (i == 0) 0f else heat[d.muscles[i - 1]] ?: 0f }
        val volt = C.volt
        val bmp = withContext(Dispatchers.Default) {
            val px = IntArray(ids.size)
            val vr = (volt.red * 255).toInt(); val vg = (volt.green * 255).toInt(); val vb = (volt.blue * 255).toInt()
            for (i in ids.indices) {
                val id = ids[i]
                if (id == 0) continue
                val a = when {
                    id == sel -> 0.62f
                    heatById[id] > 0f -> 0.07f + 0.20f * heatById[id]
                    else -> 0f
                }
                if (a > 0f) px[i] = ((a * 255).toInt() shl 24) or (vr shl 16) or (vg shl 8) or vb
            }
            // a crisp edge around the selected muscle so it reads at a glance
            if (sel > 0) {
                val w = d.w
                for (y in 1 until d.h - 1) for (x in 1 until w - 1) {
                    val i = y * w + x
                    if (ids[i] == sel && (ids[i - 1] != sel || ids[i + 1] != sel || ids[i - w] != sel || ids[i + w] != sel))
                        px[i] = (0xF0 shl 24) or (vr shl 16) or (vg shl 8) or vb
                }
            }
            Bitmap.createBitmap(px, d.w, d.h, Bitmap.Config.ARGB_8888).asImageBitmap()
        }
        overlay = near to bmp
    }

    fun fit(size: Size): Pair<Float, Offset> {
        val s = min(size.width / d.w, size.height / d.h)
        return s to Offset((size.width - d.w * s) / 2f, size.height - d.h * s)
    }

    Box(
        if (autoSpin) modifier else modifier
            .draggable(
                state = rememberDraggableState { dx -> state.yaw += dx * 0.55f },
                orientation = Orientation.Horizontal,
                onDragStarted = { state.flingJob?.cancel() },
                onDragStopped = { v -> state.flingJob = scope.launch { state.fling(v * 0.45f) } }
            )
            .pointerInput(d, near) {
                detectTapGestures { off ->
                    val (s, o) = fit(Size(size.width.toFloat(), size.height.toFloat()))
                    val x = ((off.x - o.x) / s).roundToInt(); val y = ((off.y - o.y) / s).roundToInt()
                    val ids = d.frame(near).second
                    // a finger is wide: take the most common muscle in a small disc around the tap
                    val counts = IntArray(d.muscles.size + 1)
                    val r = (14 / s).roundToInt().coerceAtLeast(4)
                    for (yy in y - r..y + r) for (xx in x - r..x + r) {
                        if (xx < 0 || yy < 0 || xx >= d.w || yy >= d.h) continue
                        if ((xx - x) * (xx - x) + (yy - y) * (yy - y) > r * r) continue
                        counts[ids[yy * d.w + xx]]++
                    }
                    var best = 0
                    for (i in 1 until counts.size) if (counts[i] > counts[best] || (best == 0 && counts[i] > 0)) best = i
                    onSelect(if (best == 0) null else d.muscles[best - 1])
                }
            }
    ) {
        Canvas(Modifier.matchParentSize()) {
            val (s, o) = fit(size)
            val dst = IntSize((d.w * s).roundToInt(), (d.h * s).roundToInt())
            val at = IntOffset(o.x.roundToInt(), o.y.roundToInt())
            // turntable glow under the feet
            val fy = o.y + d.h * s * 0.965f
            drawOval(
                Brush.radialGradient(listOf(C.volt.copy(alpha = 0.14f), Color.Transparent), center = Offset(size.width / 2, fy), radius = d.w * s * 0.42f),
                Offset(size.width / 2 - d.w * s * 0.42f, fy - d.w * s * 0.09f), Size(d.w * s * 0.84f, d.w * s * 0.18f)
            )
            val sh = shown ?: return@Canvas
            drawImage(sh.second.first, dstOffset = at, dstSize = dst)
            if (t > 0.02f) drawImage(sh.third.first, dstOffset = at, dstSize = dst, alpha = t)
            overlay?.let { (f, img) -> if (f == near) drawImage(img, dstOffset = at, dstSize = dst) }
        }
    }
}
