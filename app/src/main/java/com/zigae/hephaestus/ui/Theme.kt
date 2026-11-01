package com.zigae.hephaestus.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 디자인 레퍼런스 (Figma Community):
 * - Car Dashboard UI Concept: 거의 검정 배경 + 형광 라임 단일 강조색, 큰 숫자·작은 단위, 칸 나뉜 게이지, P R N D 표시
 * - Android Auto Design System / Car App Templates: 왼쪽 원형 아이콘 레일, 제목 + 색 보조정보 목록 행, 큰 라운드 패널
 * - Uber Maintenance Tracking: 검정 + 라임 강조의 정비 추적 화면
 */
object Palette {
    val bg = Color(0xFF07090B)
    val rail = Color(0xFF0E1114)
    val card = Color(0xFF14181C)
    val cardHigh = Color(0xFF1C2126)
    val outline = Color(0xFF2A3036)
    val text = Color(0xFFF3F5F2)
    val sub = Color(0xFF8C949B)
    val accent = Color(0xFFC6F432) // 형광 라임
    val onAccent = Color(0xFF0B0E05)
    val ok = Color(0xFF3DD9C1) // 청록 (레퍼런스의 보조 게이지 색)
    val soon = Color(0xFFFFB547)
    val overdue = Color(0xFFFF5A5F)
    val none = Color(0xFF4A5157)
}

// 차량 화면은 멀리서 보므로 글자를 크게, 숫자는 계기판처럼 얇고 크게.
private val type = Typography(
    displayLarge = TextStyle(fontSize = 96.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp),
    displaySmall = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontSize = 46.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.SemiBold), // 대화상자 제목
    titleLarge = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 31.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 28.sp),
    bodyMedium = TextStyle(fontSize = 25.sp),
    labelLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp),
)

@Composable
fun HephTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.accent,
            onPrimary = Palette.onAccent,
            background = Palette.bg,
            surface = Palette.card,
            surfaceVariant = Palette.cardHigh,
            surfaceContainerHigh = Palette.cardHigh,
            onSurface = Palette.text,
            onSurfaceVariant = Palette.sub,
            outline = Palette.outline,
            outlineVariant = Palette.outline,
            secondaryContainer = Palette.accent,
            onSecondaryContainer = Palette.onAccent,
        ),
        typography = type,
        content = content,
    )
}
