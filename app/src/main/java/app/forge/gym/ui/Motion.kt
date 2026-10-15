package app.forge.gym.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/** 3D trainer loops rendered by github.com/Dante3750/forge-motion and served from its media branch. */
object Motion {
    private const val BASE = "https://raw.githubusercontent.com/Dante3750/forge-motion/media/v1/"
    const val FRAMES = 32
    const val COLS = 8
    const val LOOP_SEC = 2.8f
    fun url(movement: String, female: Boolean) = BASE + movement + (if (female) "_f" else "_m") + ".webp"
}

private class Sheet(val image: ImageBitmap, val cell: Int, val bg: Color)

/**
 * Plays a rendered movement loop. Frames are drawn from one sprite sheet and cross-blended,
 * so the motion stays smooth at any [speed]. [fallback] shows while loading or if the loop is unavailable.
 */
@Composable
fun MotionPlayer(
    movement: String,
    female: Boolean,
    modifier: Modifier = Modifier,
    speed: Float = 1f,
    playing: Boolean = true,
    scrim: Boolean = true,
    fallback: @Composable () -> Unit = {}
) {
    val ctx = LocalContext.current
    val url = Motion.url(movement, female)
    var sheet by remember(url) { mutableStateOf<Sheet?>(null) }
    var failed by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url) {
        val req = ImageRequest.Builder(ctx).data(url).allowHardware(false).build()
        val r = ctx.imageLoader.execute(req)
        val bmp = ((r as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
        if (bmp == null) { failed = true; return@LaunchedEffect }
        val cell = bmp.width / Motion.COLS
        val px = bmp.getPixel(3, 3)
        sheet = Sheet(bmp.asImageBitmap(), cell, Color(px))
    }
    var phase by remember(url) { mutableFloatStateOf(0f) }
    val spd by rememberUpdatedState(speed)
    val play by rememberUpdatedState(playing)
    LaunchedEffect(url) {
        var last = 0L
        while (true) {
            withFrameNanos { t ->
                if (last != 0L && play) {
                    val dt = (t - last) / 1_000_000_000f
                    phase = (phase + dt * spd / Motion.LOOP_SEC) % 1f
                }
                last = t
            }
        }
    }
    val shown by animateFloatAsState(if (sheet != null) 1f else 0f, tween(500), label = "motionIn")
    val s = sheet
    Box(modifier.background(s?.bg ?: C.surface2)) {
        if (s == null || shown < 1f) Box(Modifier.fillMaxSize().alpha(1f - shown)) { fallback() }
        if (s != null) {
            Canvas(Modifier.fillMaxSize().alpha(shown)) {
                val f = phase * Motion.FRAMES
                val a = floor(f).toInt() % Motion.FRAMES
                val b = (a + 1) % Motion.FRAMES
                val mix = f - floor(f)
                val side = min(size.width, size.height).roundToInt()
                val dst = IntOffset(((size.width - side) / 2).roundToInt(), ((size.height - side) / 2).roundToInt())
                fun draw(i: Int, alpha: Float) {
                    val src = IntOffset((i % Motion.COLS) * s.cell, (i / Motion.COLS) * s.cell)
                    drawImage(s.image, src, IntSize(s.cell, s.cell), dst, IntSize(side, side), alpha = alpha)
                }
                draw(a, 1f)
                if (mix > 0.01f) draw(b, mix)
            }
        }
        if (scrim) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to C.bg.copy(alpha = 0.9f))))
        }
    }
}
