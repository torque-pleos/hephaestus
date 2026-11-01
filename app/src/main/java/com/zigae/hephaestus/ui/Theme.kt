package com.zigae.hephaestus.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val bg = Color(0xFF121418)
    val card = Color(0xFF1E2228)
    val cardHigh = Color(0xFF272C34)
    val text = Color(0xFFF2F4F7)
    val sub = Color(0xFF9AA3AF)
    val accent = Color(0xFFFF8A3D) // 대장간 불꽃
    val ok = Color(0xFF4CC38A)
    val soon = Color(0xFFF5B83D)
    val overdue = Color(0xFFFF5C5C)
    val none = Color(0xFF5B6470)
}

// 차량 화면은 멀리서 보므로 기본 글자를 크게 잡는다.
private val type = Typography(
    headlineMedium = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 22.sp),
    bodyMedium = TextStyle(fontSize = 20.sp),
    labelLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun HephTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.accent,
            onPrimary = Color.Black,
            background = Palette.bg,
            surface = Palette.card,
            surfaceVariant = Palette.cardHigh,
            onSurface = Palette.text,
            onSurfaceVariant = Palette.sub,
            secondaryContainer = Palette.accent.copy(alpha = 0.25f),
            onSecondaryContainer = Palette.text,
        ),
        typography = type,
        content = content,
    )
}
