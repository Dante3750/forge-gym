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
    private const val BASE = "https://raw.githubusercontent.com/Dante3750/forge-motion/media/"
    const val FRAMES = 32
    const val COLS = 8
    const val LOOP_SEC = 2.8f

    /** Render revision from the media branch. Part of every URL, so a re-render replaces cached loops. */
    var rev by mutableStateOf("")

    /** Reads the current render revision (called once at start-up, off the main thread). */
    fun refreshRev(): String = runCatching {
        val c = java.net.URL(BASE + "v3/rev.txt?t=" + System.currentTimeMillis() / 60000).openConnection() as java.net.HttpURLConnection
        c.useCaches = false; c.connectTimeout = 6000; c.readTimeout = 6000
        c.inputStream.bufferedReader().use { it.readText().trim() }.take(32)
    }.getOrDefault("")

    private fun q(u: String) = if (rev.isEmpty()) u else "$u?r=$rev"

    /** One way to fetch a loop: its sprite sheets and how frames are laid out in them. */
    class Source(val urls: List<String>, val cols: Int, val perSheet: Int)

    /** Newest render first (HD or small), then older sets as fallbacks. */
    fun sources(movement: String, female: Boolean, angle: Int, hd: Boolean): List<Source> {
        val g = if (female) "_f" else "_m"
        val a = if (angle == 1) "_b" else ""
        val base = movement + g + a
        val v3 = if (hd) Source(listOf(q(BASE + "v3/" + base + "_0.webp"), q(BASE + "v3/" + base + "_1.webp")), 4, 16)
        else Source(listOf(q(BASE + "v3/" + base + "_s.webp")), 8, 32)
        val out = mutableListOf(v3, Source(listOf(q(BASE + "v2/" + base + ".webp")), 8, 32))
        if (angle == 0) out += Source(listOf(q(BASE + "v1/" + movement + g + ".webp")), 8, 32)
        return out
    }

    /** The same training goal done three ways: gym, dumbbells at home, and no equipment. */
    private val GROUPS = listOf(
        listOf("squat_bb", "squat_goblet", "squat_bw"),
        listOf("squat_front_bb", "squat_goblet", "squat_bw"),
        listOf("leg_press", "squat_goblet", "wall_sit"),
        listOf("leg_extension", "goblet_lunge", "wall_sit"),
        listOf("deadlift_bb", "deadlift_db", "glute_bridge"),
        listOf("deadlift_kb", "deadlift_db", "glute_bridge"),
        listOf("rdl_bb", "rdl_db", "glute_bridge_single"),
        listOf("good_morning", "rdl_db", "back_ext"),
        listOf("leg_curl", "rdl_db", "glute_bridge_single"),
        listOf("hip_thrust_bb", "glute_bridge", "glute_bridge_single"),
        listOf("lunge_bb", "lunge_db", "lunge"),
        listOf("split_squat", "goblet_lunge", "lunge"),
        listOf("step_up", "lunge_db", "jump_squat"),
        listOf("calf_raise", "calf_raise_seated"),
        listOf("bench_bb", "bench_db", "pushup"),
        listOf("bench_db", "floor_press_db", "pushup"),
        listOf("incline_bb", "incline_db", "pushup_incline"),
        listOf("fly_db", "cable_fly", "pushup"),
        listOf("cable_fly", "fly_db", "pushup_knee"),
        listOf("dips", "bench_dip", "diamond_pushup"),
        listOf("ohp_bb", "ohp_db", "pike_pushup"),
        listOf("seated_ohp_db", "ohp_db", "pike_pushup"),
        listOf("push_press", "ohp_db", "pike_pushup"),
        listOf("lateral_raise", "cable_lateral"),
        listOf("front_raise", "lateral_raise"),
        listOf("rear_fly", "face_pull"),
        listOf("upright_row", "lateral_raise"),
        listOf("shrug", "farmer_walk"),
        listOf("pulldown", "pullup", "pullover"),
        listOf("pullup", "pulldown", "row_db_single"),
        listOf("row_bb", "row_db", "row_db_single"),
        listOf("row_cable", "row_db_single", "row_db"),
        listOf("curl_bb", "curl_db", "hammer_curl"),
        listOf("cable_curl", "curl_db", "hammer_curl"),
        listOf("preacher_curl", "curl_db", "hammer_curl"),
        listOf("pushdown", "oh_ext", "bench_dip"),
        listOf("skullcrusher", "oh_ext", "diamond_pushup"),
        listOf("kickback", "oh_ext", "bench_dip"),
        listOf("crunch", "situp", "leg_raise"),
        listOf("hanging_leg_raise", "leg_raise", "mountain_climber"),
        listOf("plank", "mountain_climber", "ab_rollout"),
        listOf("russian_twist", "crunch", "plank"),
        listOf("clean", "kb_swing", "jump_squat"),
        listOf("snatch", "kb_swing", "jump_squat"),
        listOf("kb_swing", "deadlift_kb", "jump_squat"),
        listOf("run", "jump_rope", "jumping_jack"),
        listOf("bike", "row_erg", "jumping_jack"),
    )
    private val byMovement: Map<String, List<String>> = buildMap { GROUPS.forEach { g -> g.forEach { m -> putIfAbsent(m, g) } } }

    fun ways(movement: String): List<String> = byMovement[movement] ?: emptyList()

    private val MACHINES = setOf("leg_press", "leg_extension", "leg_curl", "pulldown", "row_cable", "pushdown", "face_pull", "cable_fly",
        "cable_curl", "cable_lateral", "dips", "pullup", "hanging_leg_raise", "bike", "row_erg", "calf_raise_seated", "preacher_curl")

    fun wayLabel(movement: String): String = when {
        movement.endsWith("_bb") || movement in setOf("good_morning", "clean", "snatch", "push_press", "skullcrusher", "shrug", "upright_row") -> "Gym · Barbell"
        movement in MACHINES || movement.startsWith("cable") -> "Gym · Machine"
        movement.contains("_db") || movement.contains("goblet") || movement in setOf("lateral_raise", "front_raise", "rear_fly", "fly_db",
            "oh_ext", "kickback", "hammer_curl", "pullover", "calf_raise", "farmer_walk", "step_up", "split_squat") -> "Home · Dumbbells"
        movement.contains("kb") -> "Kettlebell"
        else -> "No equipment"
    }
}

private class Sheet(val images: List<ImageBitmap>, val cell: Int, val cols: Int, val perSheet: Int, val bg: Color)

/**
 * Plays a rendered movement loop. Frames are drawn from one sprite sheet and cross-blended,
 * so the motion stays smooth at any [speed]. [fallback] shows while loading or if the loop is unavailable.
 */
@Composable
fun MotionPlayer(
    movement: String,
    female: Boolean,
    modifier: Modifier = Modifier,
    angle: Int = 0,
    hd: Boolean = true,
    speed: Float = 1f,
    playing: Boolean = true,
    scrim: Boolean = true,
    fallback: @Composable () -> Unit = {}
) {
    val ctx = LocalContext.current
    val sources = Motion.sources(movement, female, angle, hd)
    val url = sources.first().urls.first()
    var sheet by remember(url) { mutableStateOf<Sheet?>(null) }
    LaunchedEffect(url) {
        for (src in sources) {
            val bmps = ArrayList<android.graphics.Bitmap>()
            for (u in src.urls) {
                val req = ImageRequest.Builder(ctx).data(u).allowHardware(false).build()
                val r = ctx.imageLoader.execute(req)
                val b = ((r as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap ?: break
                bmps += b
            }
            if (bmps.size == src.urls.size) {
                val first = bmps[0]
                sheet = Sheet(bmps.map { it.asImageBitmap() }, first.width / src.cols, src.cols, src.perSheet, Color(first.getPixel(3, 3)))
                return@LaunchedEffect
            }
        }
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
                    val img = s.images[(i / s.perSheet).coerceAtMost(s.images.lastIndex)]
                    val k = i % s.perSheet
                    val src = IntOffset((k % s.cols) * s.cell, (k / s.cols) * s.cell)
                    drawImage(img, src, IntSize(s.cell, s.cell), dst, IntSize(side, side), alpha = alpha,
                        filterQuality = androidx.compose.ui.graphics.FilterQuality.High)
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
