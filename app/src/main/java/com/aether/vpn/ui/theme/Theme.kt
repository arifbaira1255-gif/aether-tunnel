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
import androidx.compose.ui.unit.sp
import com.aether.vpn.R

// ---- Palette -------------------------------------------------------------
// A deeper, richer base than a flat black: three ink levels so surfaces can stack
// without borders, plus a warm-neutral text ramp that reads as expensive rather
// than as pure grey.

val Ink = Color(0xFF04050C)
val InkSoft = Color(0xFF080A16)
val Surface1 = Color(0xFF0E1122)
val Surface2 = Color(0xFF151932)
val Surface3 = Color(0xFF1C2140)

val Hairline = Color(0x14FFFFFF)
val HairlineStrong = Color(0x2EFFFFFF)
val HairlineTop = Color(0x3DFFFFFF)

val Cyan = Color(0xFF3FE0F0)
val CyanDeep = Color(0xFF12A8C4)
val Violet = Color(0xFF8B5CF6)
val VioletDeep = Color(0xFF5B2FD1)
val Indigo = Color(0xFF6366F1)
val Mint = Color(0xFF2FE3A0)
val MintDeep = Color(0xFF0FA97A)
val Amber = Color(0xFFFFC46B)
val Rose = Color(0xFFFF5C7A)
val Sky = Color(0xFF5AA9FF)

val TextPrimary = Color(0xFFF6F7FC)
val TextSecondary = Color(0xFFA2AACB)
val TextTertiary = Color(0xFF6A7295)
val TextFaint = Color(0xFF474E6B)

val BrandGradient = listOf(Cyan, Indigo, Violet)
val ConnectedGradient = listOf(Mint, Cyan)
val WarmGradient = listOf(Amber, Rose)

// ---- Type ----------------------------------------------------------------

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
        fontSize = 52.sp, letterSpacing = (-2.0).sp, lineHeight = 56.sp
    ),
    displayMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Bold,
        fontSize = 38.sp, letterSpacing = (-1.2).sp, lineHeight = 42.sp
    ),
    displaySmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, letterSpacing = (-0.8).sp, lineHeight = 34.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Bold,
        fontSize = 27.sp, letterSpacing = (-0.7).sp, lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp, letterSpacing = (-0.4).sp, lineHeight = 26.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, letterSpacing = (-0.2).sp, lineHeight = 23.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 16.5.sp, letterSpacing = (-0.1).sp, lineHeight = 21.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 14.5.sp, lineHeight = 19.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp, lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.sp
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
        fontSize = 9.5.sp, letterSpacing = 1.1.sp
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
