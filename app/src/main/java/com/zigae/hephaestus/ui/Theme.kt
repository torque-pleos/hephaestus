package com.zigae.hephaestus.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 색 하나를 "기본색 + 그 위 글자색 + 컨테이너 면 + 컨테이너 위 글자색" 한 세트로 정의한다.
 * 세트 단위로 정의하면 배경·글자 대비(Pleos 기준 4.5:1 이상)를 한 곳에서 관리할 수 있다.
 */
data class Tone(val base: Color, val on: Color, val container: Color, val onContainer: Color)

/** 앱 색 토큰. 주간은 남색 강조, 야간(Pleos 야간 모드)은 청록 강조. */
data class AppColors(
    val night: Boolean,
    val bg: Color,
    val rail: Color,
    val card: Color,
    val cardHigh: Color, // 카드 안의 회색 면(칩·입력)
    val outline: Color,
    val text: Color,
    val sub: Color,
    val none: Color, // 비활성·기록 없음
    val primary: Tone,
    val info: Tone,
    val success: Tone,
    val warning: Tone,
    val danger: Tone,
)

val DayColors = AppColors(
    night = false,
    bg = Color(0xFFEEF0F3),
    rail = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardHigh = Color(0xFFF3F4F6),
    outline = Color(0xFFDADDE2),
    text = Color(0xFF111317),
    sub = Color(0xFF5F656E),
    none = Color(0xFFB4B9C1),
    primary = Tone(Color(0xFF002C5F), Color.White, Color(0xFFE6EBF2), Color(0xFF002C5F)),
    info = Tone(Color(0xFF1E66F5), Color.White, Color(0xFFE5EEFE), Color(0xFF0C3E9E)),
    success = Tone(Color(0xFF187C33), Color.White, Color(0xFFE6F3EA), Color(0xFF105F26)),
    warning = Tone(Color(0xFFE5850A), Color(0xFF2B1900), Color(0xFFFDF0DB), Color(0xFF7A4300)),
    danger = Tone(Color(0xFFD1352F), Color.White, Color(0xFFFBE7E6), Color(0xFF8C1F1B)),
)

val NightColors = AppColors(
    night = true,
    bg = Color(0xFF0E1013),
    rail = Color(0xFF16191D),
    card = Color(0xFF1A1D22),
    cardHigh = Color(0xFF24282F),
    outline = Color(0xFF30353D),
    text = Color(0xFFF1F3F5),
    sub = Color(0xFF9EA5AF),
    none = Color(0xFF5A606A),
    primary = Tone(Color(0xFF2FB6D4), Color(0xFF032A33), Color(0xFF113740), Color(0xFFA6E6F4)),
    info = Tone(Color(0xFF6E9FFF), Color(0xFF071A3D), Color(0xFF17253F), Color(0xFFBCD1FF)),
    success = Tone(Color(0xFF4CC36B), Color(0xFF06260F), Color(0xFF15301E), Color(0xFF9FE5B2)),
    warning = Tone(Color(0xFFFFB547), Color(0xFF2E1D00), Color(0xFF3A2A10), Color(0xFFFFD79A)),
    danger = Tone(Color(0xFFFF6B66), Color(0xFF3A0705), Color(0xFF3D1A19), Color(0xFFFFB6B3)),
)

val LocalAppColors = staticCompositionLocalOf { DayColors }

/** 화면 코드에서 쓰는 이름. 현재 주간/야간 토큰을 읽는다. */
object Palette {
    val bg: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.bg
    val rail: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.rail
    val card: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.card
    val cardHigh: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.cardHigh
    val outline: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.outline
    val text: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.text
    val sub: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.sub
    val none: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.none
    val primary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.primary.base
    val onPrimary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.primary.on
    val info: Tone @Composable @ReadOnlyComposable get() = LocalAppColors.current.info
    val success: Tone @Composable @ReadOnlyComposable get() = LocalAppColors.current.success
    val warning: Tone @Composable @ReadOnlyComposable get() = LocalAppColors.current.warning
    val danger: Tone @Composable @ReadOnlyComposable get() = LocalAppColors.current.danger
    val primaryTone: Tone @Composable @ReadOnlyComposable get() = LocalAppColors.current.primary

    /** 큰 강조 면(질문 카드 머리). 야간에는 눈부심을 줄이려 진한 컨테이너 면을 쓴다. */
    val headerBg: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.let { if (it.night) it.primary.container else it.primary.base }
    val onHeader: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.let { if (it.night) it.primary.onContainer else it.primary.on }
}

// 차량 화면은 멀리서 보므로 웹 기준보다 크게, 행간은 글자 크기의 1.4배.
private fun style(size: Int, weight: FontWeight, tracking: TextUnit = 0.sp) =
    TextStyle(fontSize = size.sp, lineHeight = (size * 1.4f).sp, fontWeight = weight, letterSpacing = tracking)

private val type = Typography(
    displayLarge = style(76, FontWeight.Bold, (-1).sp),
    displaySmall = style(48, FontWeight.Bold),
    headlineMedium = style(44, FontWeight.Bold),
    headlineSmall = style(34, FontWeight.Bold), // 대화상자 제목
    titleLarge = style(34, FontWeight.Bold),
    titleMedium = style(30, FontWeight.SemiBold),
    bodyLarge = style(28, FontWeight.Normal),
    bodyMedium = style(25, FontWeight.Normal),
    labelLarge = style(28, FontWeight.SemiBold),
    labelMedium = style(24, FontWeight.Medium),
)

/** 시스템 주간/야간(Pleos 야간 모드 → uiMode night)을 따라 토큰을 바꾼다. */
@Composable
fun HephTheme(night: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (night) NightColors else DayColors
    val scheme = if (night) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(
            colorScheme = scheme.copy(
                primary = c.primary.base,
                onPrimary = c.primary.on,
                background = c.bg,
                surface = c.card,
                surfaceVariant = c.cardHigh,
                surfaceContainerHigh = c.card,
                surfaceContainerHighest = c.cardHigh,
                onSurface = c.text,
                onSurfaceVariant = c.sub,
                outline = c.outline,
                outlineVariant = c.outline,
                secondaryContainer = c.primary.base,
                onSecondaryContainer = c.primary.on,
            ),
            typography = type,
            content = content,
        )
    }
}
