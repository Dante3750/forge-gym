package app.forge.gym.viewer

import android.view.Choreographer
import android.view.TextureView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import app.forge.gym.ui.C
import app.forge.gym.ui.MotionPlayer

/**
 * The live 3D human doing [movement]: the same avatar as the full 3D screen, drawn in real time
 * (smooth at the phone's refresh rate, sharp at any size). [interactive] lets the user drag/pinch it.
 * [onState] reports download progress (0..1) and whether the human is showing.
 */
@Composable
fun Live3D(
    movement: String,
    female: Boolean,
    modifier: Modifier = Modifier,
    preset: ForgeViewer.Preset = ForgeViewer.Preset.ANGLE,
    speed: Float = 1f,
    interactive: Boolean = false,
    onState: (progress: Float, showing: Boolean) -> Unit = { _, _ -> }
) {
    val ctx = LocalContext.current
    var viewer by remember { mutableStateOf<ForgeViewer?>(null) }
    val spd by rememberUpdatedState(speed)
    val report by rememberUpdatedState(onState)
    AndroidView(
        factory = { c ->
            com.google.android.filament.utils.Utils.init()
            TextureView(c).also { tv ->
                val v = ForgeViewer(tv, 0x0A0B0D, lite = !interactive)
                if (interactive) tv.setOnTouchListener(v)
                viewer = v
            }
        },
        modifier = modifier
    )
    DisposableEffect(viewer) {
        val v = viewer
        val ch = Choreographer.getInstance()
        var last = 0L
        var t = 0.0
        var shown = false
        val cb = object : Choreographer.FrameCallback {
            override fun doFrame(now: Long) {
                ch.postFrameCallback(this)
                if (last != 0L) t += (now - last) / 1e9 * spd
                last = now
                v?.render(now, t)
                val s = v?.showing == true
                if (s != shown) { shown = s; report(1f, s) }
            }
        }
        if (v != null) ch.postFrameCallback(cb)
        onDispose { ch.removeFrameCallback(cb) }
    }
    LaunchedEffect(viewer, female, movement) {
        val v = viewer ?: return@LaunchedEffect
        val g = if (female) "f" else "m"
        Models3D.fetch(ctx, "studio_ibl.ktx") {}?.let { v.setEnvironment(it) }
        val model = Models3D.fetch(ctx, "model_$g.glb") { report(it * 0.85f, false) } ?: return@LaunchedEffect
        v.setModel(model)
        val anim = Models3D.fetch(ctx, "${movement}_$g.glb") { report(0.85f + it * 0.15f, false) } ?: return@LaunchedEffect
        v.setMotion(anim)
    }
    LaunchedEffect(viewer, preset) { viewer?.setPreset(preset) }
}

/**
 * The trainer everywhere in the app: the live 3D human when the phone can draw it, fading in over the
 * HD video loop (which shows while the 3D model loads, and on phones without a capable graphics chip).
 */
@Composable
fun Trainer(
    movement: String,
    female: Boolean,
    modifier: Modifier = Modifier,
    angle: Int = 0,
    speed: Float = 1f,
    playing: Boolean = true,
    scrim: Boolean = true,
    live: Boolean = true,
    fallback: @Composable () -> Unit = {}
) {
    var showing by remember(movement, female) { mutableStateOf(false) }
    val a by animateFloatAsState(if (showing) 1f else 0f, tween(450), label = "live3d")
    Box(modifier.background(C.bg)) {
        if (a < 1f) MotionPlayer(movement, female, Modifier.fillMaxSize(), angle = angle, speed = speed, playing = playing, scrim = false, fallback = fallback)
        if (live && GpuProbe.embedded) {
            Live3D(
                movement, female, Modifier.fillMaxSize().alpha(a),
                preset = if (angle == 1) ForgeViewer.Preset.FRONT else ForgeViewer.Preset.ANGLE,
                speed = if (playing) speed else 0f
            ) { _, s -> showing = s }
        }
        if (scrim) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to C.bg.copy(alpha = 0.9f))))
    }
}
