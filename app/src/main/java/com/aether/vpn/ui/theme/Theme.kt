package com.aether.vpn.ui.theme

import androidx.compose.material3.MaterialTheme
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
import com.aether.vpn.R

// ============================================================================
//  Palette
//
//  A four-step ink ramp so surfaces can stack without a single border, and a
//  text ramp that is warm-neutral rather than pure grey — that is what reads as
//  expensive instead of as a developer default.
// ============================================================================

val Ink        = Color(0xFF05060D)
val InkSoft    = Color(0xFF090B16)
val Surface1   = Color(0xFF0D1020)
val Surface2   = Color(0xFF141830)
val Surface3   = Color(0xFF1B2040)
val Surface4   = Color(0xFF232A50)

val Hairline       = Color(0x12FFFFFF)
val HairlineStrong = Color(0x26FFFFFF)
val HairlineTop    = Color(0x40FFFFFF)

// Signature accents
val Cyan       = Color(0xFF45E3F2)
val CyanDeep   = Color(0xFF0FA5C2)
val Indigo     = Color(0xFF6C7BFF)
val Violet     = Color(0xFF9B6BFF)
val VioletDeep = Color(0xFF5B2FD1)
val Mint       = Color(0xFF34E5A4)
val MintDeep   = Color(0xFF0BA97A)
val Amber      = Color(0xFFFFC46B)
val Rose       = Color(0xFFFF5F7E)
val Sky        = Color(0xFF5AA9FF)
val Platinum   = Color(0xFFE8ECF8)

val TextPrimary   = Color(0xFFF7F8FD)
val TextSecondary = Color(0xFFA6AECF)
val TextTertiary  = Color(0xFF6E7699)
val TextFaint     = Color(0xFF4A5170)

val BrandGradient     = listOf(Cyan, Indigo, Violet)
val ConnectedGradient = listOf(Mint, Cyan)
val WarmGradient      = listOf(Amber, Rose)
val PlatinumGradient  = listOf(Color(0xFFFFFFFF), Color(0xFFB9C2E0))

// ============================================================================
//  Geometry — one scale, used everywhere, so nothing looks hand-placed.
// ============================================================================

object Radius {
    val xs = 10.dp
    val sm = 14.dp
    val md = 20.dp
    val lg = 26.dp
    val xl = 32.dp
    val pill = 100.dp
}

object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    val huge = 40.dp
}

// ============================================================================
//  Type — Inter, with a tight display ramp and generous body leading.
// ============================================================================

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold)
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.ExtraBold,
        fontSize = 56.sp, letterSpacing = (-2.4).sp, lineHeight = 58.sp
    ),
    displayMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, letterSpacing = (-1.4).sp, lineHeight = 44.sp
    ),
    displaySmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Bold,
        fontSize = 31.sp, letterSpacing = (-0.9).sp, lineHeight = 35.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, letterSpacing = (-0.8).sp, lineHeight = 33.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, letterSpacing = (-0.5).sp, lineHeight = 27.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 18.5.sp, letterSpacing = (-0.3).sp, lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 16.5.sp, letterSpacing = (-0.15).sp, lineHeight = 21.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 14.5.sp, lineHeight = 19.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp, lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.5.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp, lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 9.5.sp, letterSpacing = 1.2.sp
    )
)

private val AppColors = darkColorScheme(
    primary = Cyan,
    onPrimary = Ink,
    primaryContainer = CyanDeep,
    secondary = Violet,
    onSecondary = Color.White,
    secondaryContainer = VioletDeep,
    tertiary = Mint,
    background = Ink,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    outline = HairlineStrong,
    outlineVariant = Hairline,
    error = Rose,
    onError = Color.White
)

@Composable
fun AetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = AppTypography,
        content = content
    )
}
