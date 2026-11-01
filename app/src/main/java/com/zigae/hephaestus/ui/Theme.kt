package com.zigae.hephaestus.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 디자인 레퍼런스: myHyundai 앱(앱스토어 공식 스크린샷) + Pleos Connect 기본 UI(밝은 톤).
 * - 연회색 배경 + 흰 카드, 현대 네이비 단일 주색, 절제된 상태색
 * - 아이콘 + 라벨 + 꺾쇠 목록 행, 회색 안내 배너, 네이비 채움 버튼
 * HMG 디자인 시스템(Storybook) 토큰을 확인하면 이 값들을 교체한다.
 */
object Palette {
    val bg = Color(0xFFF2F3F5)
    val rail = Color(0xFFFFFFFF)
    val card = Color(0xFFFFFFFF)
    val cardHigh = Color(0xFFF2F3F5) // 카드 안의 회색 영역(배너·칩)
    val outline = Color(0xFFE3E5E8)
    val text = Color(0xFF16181C)
    val sub = Color(0xFF6E737B)
    val navy = Color(0xFF002C5F) // 현대 네이비
    val blue = Color(0xFF1F6FEB) // 강조(선택 막대·링크)
    val accent = navy
    val onAccent = Color(0xFFFFFFFF)
    val ok = Color(0xFF1F6FEB)
    val soon = Color(0xFFEE8A1C)
    val overdue = Color(0xFFE5484D)
    val none = Color(0xFFC4C8CE)
}

// 차량 화면은 멀리서 보므로 글자를 크게. 숫자는 과하지 않은 굵기.
private val type = Typography(
    displayLarge = TextStyle(fontSize = 76.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold), // 대화상자 제목
    titleLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 28.sp),
    bodyMedium = TextStyle(fontSize = 25.sp),
    labelLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun HephTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.navy,
            onPrimary = Color.White,
            background = Palette.bg,
            surface = Palette.card,
            surfaceVariant = Palette.cardHigh,
            surfaceContainerHigh = Palette.card,
            surfaceContainerHighest = Palette.cardHigh,
            onSurface = Palette.text,
            onSurfaceVariant = Palette.sub,
            outline = Palette.outline,
            outlineVariant = Palette.outline,
            secondaryContainer = Palette.navy,
            onSecondaryContainer = Color.White,
        ),
        typography = type,
        content = content,
    )
}
