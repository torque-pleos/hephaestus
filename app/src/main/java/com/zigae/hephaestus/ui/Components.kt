package com.zigae.hephaestus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zigae.hephaestus.domain.Gear
import kotlin.math.roundToInt

val PanelShape = RoundedCornerShape(28.dp)

/** 레퍼런스의 둥근 패널. [glow]면 라임 테두리 + 은은한 빛. */
@Composable
fun Panel(modifier: Modifier = Modifier, glow: Boolean = false, padding: Dp = 28.dp, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (glow) Palette.cardHigh else Palette.card),
        shape = PanelShape,
        border = BorderStroke(if (glow) 2.dp else 1.dp, if (glow) Palette.accent else Palette.outline),
        modifier = modifier
            .fillMaxWidth()
            .then(if (glow) Modifier.shadow(24.dp, PanelShape, ambientColor = Palette.accent, spotColor = Palette.accent) else Modifier),
    ) {
        Column(Modifier.padding(padding)) { content() }
    }
}

/** 계기판식 숫자: 큰 값 + 작은 단위. */
@Composable
fun BigNumber(value: String, unit: String, color: Color = Palette.text, small: Boolean = false) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = if (small) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayLarge, color = color)
        Spacer(Modifier.width(8.dp))
        Text(unit, style = MaterialTheme.typography.titleMedium, color = Palette.sub, modifier = Modifier.padding(bottom = if (small) 8.dp else 16.dp))
    }
}

/** 레퍼런스 배터리 게이지처럼 칸으로 나뉜 진행 막대. */
@Composable
fun SegmentBar(progress: Float, color: Color, modifier: Modifier = Modifier, segments: Int = 12) {
    val filled = (progress.coerceIn(0f, 1f) * segments).roundToInt()
    Row(modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(segments) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i < filled) color else Palette.cardHigh),
            )
        }
    }
}

/** P R N D 표시. 현재 기어만 라임. */
@Composable
fun GearIndicator(gear: Gear) {
    val current = when (gear) {
        Gear.PARK -> "P"
        Gear.REVERSE -> "R"
        Gear.NEUTRAL -> "N"
        Gear.DRIVE -> "D"
        Gear.UNKNOWN -> ""
    }
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf("P", "R", "N", "D").forEach { g ->
            val on = g == current
            Text(
                g,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Normal),
                color = if (on) Palette.accent else Palette.none,
            )
        }
    }
}

/** 둥근 테두리 라벨. */
@Composable
fun Pill(text: String, color: Color = Palette.accent, filled: Boolean = false, icon: ImageVector? = null) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (filled) color else Color.Transparent)
            .border(1.5.dp, color, CircleShape)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, null, tint = if (filled) Palette.onAccent else color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (filled) Palette.onAccent else color)
    }
}

/** 요약 수치 칸 (레퍼런스의 Energy Consumption 박스). */
@Composable
fun StatTile(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.card)
            .border(1.dp, Palette.outline, RoundedCornerShape(20.dp))
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Text(value, style = MaterialTheme.typography.displaySmall, color = color)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.sub)
    }
}

/** 원형 아이콘 (Android Auto 레일 스타일). */
@Composable
fun IconCircle(icon: ImageVector, selected: Boolean, size: Dp = 72.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (selected) Palette.accent else Palette.cardHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (selected) Palette.onAccent else Palette.text, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun SectionTitle(text: String, sub: String? = null, color: Color = Palette.text) {
    Column(Modifier.padding(top = 4.dp, bottom = 12.dp)) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = color)
        sub?.let { Text(it, color = Palette.sub, style = MaterialTheme.typography.bodyMedium) }
    }
}

/** 작은 대문자 머리글 (계기판 라벨 느낌). */
@Composable
fun Caption(text: String, color: Color = Palette.sub) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = color)
}
