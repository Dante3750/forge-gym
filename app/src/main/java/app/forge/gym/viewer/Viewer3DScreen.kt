package app.forge.gym.viewer

import android.view.Choreographer
import android.view.TextureView
import androidx.compose.foundation.background
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.forge.gym.LocalApp
import app.forge.gym.data.Gender
import app.forge.gym.screens.statusTop
import app.forge.gym.ui.C
import app.forge.gym.ui.Chip
import app.forge.gym.ui.IconBox
import app.forge.gym.ui.Ring
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Downloads 3D files once and keeps them on the phone (keyed by the published revision). */
object Models3D {
    private const val BASE = "https://raw.githubusercontent.com/Dante3750/forge-motion/media/v3d/"
    @Volatile private var rev: String? = null

    private fun revision(): String {
        rev?.let { return it }
        val r = runCatching {
            val c = URL(BASE + "rev.txt?t=" + System.currentTimeMillis() / 60000).openConnection() as HttpURLConnection
            c.useCaches = false; c.connectTimeout = 8000; c.readTimeout = 8000
            c.inputStream.bufferedReader().use { it.readText().trim() }
        }.getOrDefault("0")
        rev = r; return r
    }

    /** Returns a direct buffer with the file, reporting download progress 0..1. */
    suspend fun fetch(ctx: android.content.Context, name: String, progress: (Float) -> Unit): ByteBuffer? = withContext(Dispatchers.IO) {
        val r = revision()
        val dir = File(ctx.cacheDir, "forge3d").apply { mkdirs() }
        val file = File(dir, "$r-$name")
        if (!file.exists()) {
            dir.listFiles()?.filter { it.name.endsWith("-$name") }?.forEach { it.delete() }
            val ok = runCatching {
                val c = URL(BASE + name + "?r=" + r).openConnection() as HttpURLConnection
                c.connectTimeout = 15000; c.readTimeout = 30000
                val total = c.contentLength.toFloat()
                val tmp = File(dir, "$name.part")
                c.inputStream.use { inp ->
                    tmp.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024); var done = 0L
                        while (true) {
                            val n = inp.read(buf); if (n < 0) break
                            out.write(buf, 0, n); done += n
                            if (total > 0) progress((done / total).coerceIn(0f, 1f))
                        }
                    }
                }
                tmp.renameTo(file)
            }.isSuccess
            if (!ok) return@withContext null
        }
        progress(1f)
        val bytes = file.readBytes()
        ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).also { it.flip() }
    }
}

@Composable
fun Viewer3DScreen(id: String, onBack: () -> Unit) {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val e = app.exercise(id)
    val movement = app.motionFor(id)
    var female by rememberSaveable { mutableStateOf(app.store.profile.gender == Gender.FEMALE) }
    var preset by rememberSaveable { mutableStateOf(ForgeViewer.Preset.ANGLE) }
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var playing by rememberSaveable { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("Loading 3D model") }
    var viewer by remember { mutableStateOf<ForgeViewer?>(null) }
    val spd by rememberUpdatedState(if (playing) speed else 0f)

    Box(Modifier.fillMaxSize().background(C.bg)) {
        AndroidView(
            factory = { c ->
                com.google.android.filament.utils.Utils.init()
                TextureView(c).also { tv ->
                    val v = ForgeViewer(tv, 0x0A0B0D)
                    tv.setOnTouchListener(v)
                    viewer = v
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        // render loop
        DisposableEffect(viewer) {
            val v = viewer
            val ch = Choreographer.getInstance()
            var last = 0L
            var t = 0.0
            val cb = object : Choreographer.FrameCallback {
                override fun doFrame(now: Long) {
                    ch.postFrameCallback(this)
                    if (last != 0L) t += (now - last) / 1e9 * spd
                    last = now
                    v?.render(now, t)
                }
            }
            if (v != null) ch.postFrameCallback(cb)
            onDispose { ch.removeFrameCallback(cb) }
        }
        // the shared human for this body type, then this exercise's motion
        LaunchedEffect(viewer, female) {
            val v = viewer ?: return@LaunchedEffect
            status = "Loading 3D model"
            val g = if (female) "f" else "m"
            val model = Models3D.fetch(ctx, "model_$g.glb") { progress = it * 0.8f }
            if (model == null) { status = "Couldn't download the 3D model. Check your connection."; return@LaunchedEffect }
            v.setModel(model)
            status = "Loading the movement"
            val mv = movement
            val anim = if (mv != null) Models3D.fetch(ctx, "${mv}_$g.glb") { progress = 0.8f + it * 0.2f } else null
            if (anim != null) v.setMotion(anim)
            status = ""
        }
        LaunchedEffect(viewer, preset) { viewer?.setPreset(preset) }

        // top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = statusTop() + 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBox(Icons.AutoMirrored.Rounded.ArrowBack, onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Txt((e?.name ?: "3D").uppercase(), T.h3, maxLines = 1)
                Txt("Drag to turn · pinch to zoom", T.small, C.dim)
            }
        }
        if (status.isNotEmpty()) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Ring(progress, 72.dp, 6.dp) { Txt("${(progress * 100).toInt()}%", T.label) }
                Spacer(Modifier.height(12.dp))
                Txt(status, T.small, C.dim)
            }
        }
        // controls
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Front", preset == ForgeViewer.Preset.FRONT, { preset = ForgeViewer.Preset.FRONT })
                Chip("3/4", preset == ForgeViewer.Preset.ANGLE, { preset = ForgeViewer.Preset.ANGLE })
                Chip("Side", preset == ForgeViewer.Preset.SIDE, { preset = ForgeViewer.Preset.SIDE })
                Chip("Back", preset == ForgeViewer.Preset.BACK, { preset = ForgeViewer.Preset.BACK })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(if (playing) "Pause" else "Play", !playing, { playing = !playing })
                Chip("0.5×", speed < 1f, { speed = if (speed < 1f) 1f else 0.5f })
                Chip(if (female) "Women" else "Men", false, { female = !female })
            }
        }
    }
}
