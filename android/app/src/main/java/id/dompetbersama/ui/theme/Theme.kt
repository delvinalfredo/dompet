package id.dompetbersama.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.dompetbersama.R

/*
 * Hallmark theme: Furrow (dealt 2/3, seed aplikasi-android-pencatat-keuangan-987e·574bde41·2026-11-18·r0).
 * Dials: ENERGY low, RHYTHM steady ruled lists, MOTION none beyond platform defaults.
 * Signature: the recorded balance set at display size over a 2px ink total rule.
 * Tangerine is a fill with ink on it, never a text colour. Green marks money in.
 */
object Dompet {
    val Paper = Color(0xFFF5F1EA)
    val Paper2 = Color(0xFFFFFDFA)
    val Paper3 = Color(0xFFE9E4D9)
    val Ink = Color(0xFF14110F)
    val Ink2 = Color(0xFF2C2825)
    val Muted = Color(0xFF58514C)
    val Rule = Color(0xFFDBD7CE)
    val Accent = Color(0xFFFC6B33)
    val AccentEdge = Color(0xFFC9572B)
    val AccentSoft = Color(0xFFFED8C4)
    val Green = Color(0xFF246E3A)
    val GreenSoft = Color(0xFFD3EDD7)
    val Danger = Color(0xFFA3261B)
}

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: Int) =
    Font(resId, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val DisplayFamily = FontFamily(variable(R.font.red_hat_display, 600))
val TextFamily = FontFamily(variable(R.font.red_hat_text, 400), variable(R.font.red_hat_text, 500))
val MonoFamily = FontFamily(variable(R.font.red_hat_mono, 400), variable(R.font.red_hat_mono, 500))

private val colors = lightColorScheme(
    primary = Dompet.Accent,
    onPrimary = Dompet.Ink,
    primaryContainer = Dompet.AccentSoft,
    onPrimaryContainer = Dompet.Ink,
    secondary = Dompet.Green,
    onSecondary = Dompet.Paper2,
    secondaryContainer = Dompet.GreenSoft,
    onSecondaryContainer = Dompet.Ink,
    background = Dompet.Paper,
    onBackground = Dompet.Ink,
    surface = Dompet.Paper,
    onSurface = Dompet.Ink,
    surfaceVariant = Dompet.Paper3,
    onSurfaceVariant = Dompet.Muted,
    surfaceContainerLowest = Dompet.Paper2,
    surfaceContainerLow = Dompet.Paper2,
    surfaceContainer = Dompet.Paper2,
    surfaceContainerHigh = Dompet.Paper2,
    surfaceContainerHighest = Dompet.Paper3,
    outline = Dompet.Muted,
    outlineVariant = Dompet.Rule,
    error = Dompet.Danger,
    inverseSurface = Dompet.Ink,
    inverseOnSurface = Dompet.Paper2,
)

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight(600), fontSize = 44.sp, lineHeight = 44.sp, letterSpacing = (-1.5).sp),
    headlineMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight(600), fontSize = 28.sp, lineHeight = 30.sp, letterSpacing = (-0.9).sp),
    headlineSmall = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight(600), fontSize = 22.sp, lineHeight = 24.sp, letterSpacing = (-0.6).sp),
    titleMedium = TextStyle(fontFamily = TextFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = TextFamily, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = TextFamily, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = TextFamily, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = TextFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = TextFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = TextFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

/** Figures: amounts, dates, times. Tabular so columns line up. */
val Figure = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, fontFeatureSettings = "tnum")
val FigureSmall = TextStyle(fontFamily = MonoFamily, fontSize = 12.sp, fontFeatureSettings = "tnum")

@Composable
fun DompetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
