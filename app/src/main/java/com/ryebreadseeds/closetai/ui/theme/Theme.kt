package com.ryebreadseeds.closetai.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object ClosetColors {
    val Ink = Color(0xFF14101A)
    val InkMid = Color(0xFF1C1624)
    val Plum = Color(0xFF2A2035)
    val Rose = Color(0xFFE8B4B8)
    val RoseDeep = Color(0xFFC98A90)
    val Cream = Color(0xFFF5E6E8)
    val Mint = Color(0xFF9ED9C5)
    val Card = Color.White.copy(alpha = 0.08f)
    val CardStroke = Color.White.copy(alpha = 0.12f)
    val TextPrimary = Color(0xFFF8F4F6)
    val TextSecondary = Color.White.copy(alpha = 0.62f)
    val Danger = Color(0xFFFF7A86)

    val BackgroundBrush = Brush.verticalGradient(
        colors = listOf(Ink, InkMid, Plum.copy(alpha = 0.95f))
    )
    val CardBrush = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f))
    )
}

private val DarkScheme = darkColorScheme(
    primary = ClosetColors.Rose,
    onPrimary = ClosetColors.Ink,
    secondary = ClosetColors.Mint,
    onSecondary = ClosetColors.Ink,
    tertiary = ClosetColors.RoseDeep,
    background = ClosetColors.Ink,
    surface = ClosetColors.InkMid,
    surfaceVariant = ClosetColors.Plum,
    onBackground = ClosetColors.TextPrimary,
    onSurface = ClosetColors.TextPrimary,
    onSurfaceVariant = ClosetColors.TextSecondary,
    error = ClosetColors.Danger
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, color = ClosetColors.TextPrimary),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = ClosetColors.TextPrimary),
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp, color = ClosetColors.TextPrimary),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, color = ClosetColors.TextPrimary),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, color = ClosetColors.TextPrimary),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = ClosetColors.TextSecondary),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, color = ClosetColors.TextPrimary)
)

@Composable
fun ClosetAiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, typography = AppTypography, content = content)
}

@Composable
fun ClosetBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClosetColors.BackgroundBrush)
    ) { content() }
}

fun Modifier.closetCard(corner: Int = 20): Modifier {
    val shape = RoundedCornerShape(corner.dp)
    return this
        .clip(shape)
        .background(ClosetColors.CardBrush, shape)
        .border(1.dp, ClosetColors.CardStroke, shape)
}
