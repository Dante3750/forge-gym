package app.forge.gym.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import app.forge.gym.data.Exercise
import kotlinx.coroutines.delay

private val muted = ColorFilter.colorMatrix(ColorMatrix().also { it.setToSaturation(0.12f) })

/** Exercise photo, desaturated so the stock images sit inside the dark palette. */
@Composable
fun ExerciseImage(url: String?, modifier: Modifier = Modifier, scrim: Boolean = false) {
    val ctx = LocalContext.current
    Box(modifier.background(C.surface2)) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(ctx).data(url).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = muted,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (scrim) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, C.bg.copy(alpha = 0.85f))))
            )
        }
    }
}

/** Flips between the start and end photo so the exercise reads as a movement. */
@Composable
fun ExerciseLoop(ex: Exercise, modifier: Modifier = Modifier, scrim: Boolean = true) {
    val n = ex.images.size
    var idx by remember(ex.id) { mutableIntStateOf(0) }
    LaunchedEffect(ex.id) {
        if (n > 1) {
            while (true) {
                delay(1100)
                idx = (idx + 1) % n
            }
        }
    }
    Crossfade(targetState = idx, animationSpec = tween(450), modifier = modifier, label = "loop") { i ->
        ExerciseImage(ex.imageUrl(i), Modifier.fillMaxSize(), scrim)
    }
}
