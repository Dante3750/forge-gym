package app.forge.gym.ui

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.forge.gym.R

/** One accent (volt) on near-black. Ember is reserved for streaks, red for destructive actions. */
object C {
    val bg = Color(0xFF0A0B0D)
    val surface = Color(0xFF131519)
    val surface2 = Color(0xFF1B1E23)
    val line = Color(0xFF272B32)
    val text = Color(0xFFF3F5EF)
    val dim = Color(0xFF8E959F)
    val faint = Color(0xFF5A606A)
    val volt = Color(0xFFC9FF2F)
    val voltDeep = Color(0xFF7C9F12)
    val ember = Color(0xFFFF6A2B)
    val danger = Color(0xFFFF4D4D)
}

val Display = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold)
)

val BodyFont = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold)
)

object T {
    val numXL = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 92.sp, lineHeight = 84.sp, letterSpacing = (-1).sp)
    val hero = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 48.sp, letterSpacing = (-0.5).sp)
    val num = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 40.sp)
    val numM = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 28.sp)
    val h1 = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 34.sp)
    val h2 = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 26.sp)
    val h3 = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 21.sp)
    val label = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 1.6.sp)
    val btn = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = 1.2.sp)
    val body = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    val bodyM = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp)
    val small = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
}

/** Cut corners (top-right, bottom-left) are the one shape motif used on cards, buttons and chips. */
val Cut = CutCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 0.dp, bottomStart = 16.dp)
val CutSm = CutCornerShape(topStart = 0.dp, topEnd = 10.dp, bottomEnd = 0.dp, bottomStart = 10.dp)
val CutXs = CutCornerShape(topStart = 0.dp, topEnd = 6.dp, bottomEnd = 0.dp, bottomStart = 6.dp)

@Composable
fun ForgeTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = C.volt,
        onPrimary = C.bg,
        background = C.bg,
        onBackground = C.text,
        surface = C.surface,
        onSurface = C.text,
        surfaceVariant = C.surface2,
        onSurfaceVariant = C.dim,
        outline = C.line
    )
    val type = Typography(
        bodyLarge = T.body,
        bodyMedium = T.body,
        bodySmall = T.small,
        titleLarge = T.h2,
        titleMedium = T.h3,
        labelLarge = T.btn
    )
    MaterialTheme(colorScheme = scheme, typography = type) {
        Surface(color = C.bg, contentColor = C.text, content = content)
    }
}
