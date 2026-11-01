package com.zigae.hephaestus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zigae.hephaestus.domain.Gear

val PanelShape = RoundedCornerShape(20.dp)
val ButtonShape = RoundedCornerShape(10.dp)

/** 흰 카드 (myHyundai 카드). */
@Composable
fun Panel(modifier: Modifier = Modifier, padding: Dp = 28.dp, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Palette.card)
            .padding(padding),
    ) { content() }
}

/** 네이비 머리 블록 + 흰 본문 카드 (myHyundai 월렛 카드). */
@Composable
fun HeaderPanel(header: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(PanelShape).background(Palette.card)) {
        Column(Modifier.fillMaxWidth().background(Palette.navy).padding(horizontal = 28.dp, vertical = 24.dp)) { header() }
        Column(Modifier.padding(28.dp)) { content() }
    }
}

/** 숫자 + 작은 단위. */
@Composable
fun BigNumber(value: String, unit: String, color: Color = Palette.text, small: Boolean = false) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = if (small) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayLarge, color = color)
        Spacer(Modifier.width(6.dp))
        Text(unit, style = MaterialTheme.typography.titleMedium, color = Palette.sub, modifier = Modifier.padding(bottom = if (small) 6.dp else 12.dp))
    }
}

/** 얇은 진행 막대. */
@Composable
fun ProgressLine(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Palette.cardHigh)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}

/** 기어 표시: 현재 기어만 네이비 칸. */
@Composable
fun GearIndicator(gear: Gear) {
    val current = when (gear) {
        Gear.PARK -> "P"
        Gear.REVERSE -> "R"
        Gear.NEUTRAL -> "N"
        Gear.DRIVE -> "D"
        Gear.UNKNOWN -> ""
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf("P", "R", "N", "D").forEach { g ->
            val on = g == current
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(if (on) Palette.navy else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(g, style = MaterialTheme.typography.labelLarge, color = if (on) Color.White else Palette.none)
            }
        }
    }
}

/** 회색 안내 배너 (myHyundai "3일 뒤 정비 예약이 있어요"). */
@Composable
fun InfoBanner(text: String, icon: ImageVector = Icons.Filled.Info, tint: Color = Palette.navy) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.cardHigh).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Palette.text)
    }
}

/** 요약 칸. */
@Composable
fun StatTile(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(PanelShape).background(Palette.card).padding(horizontal = 24.dp, vertical = 20.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.sub)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.displaySmall, color = color)
            Spacer(Modifier.width(4.dp))
            Text("건", style = MaterialTheme.typography.bodyLarge, color = Palette.sub, modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}

/** 아이콘 + 라벨 + 값 + 꺾쇠 목록 행 (myHyundai 마이카 메뉴). */
@Composable
fun ListRow(icon: ImageVector?, title: String, value: String? = null, valueColor: Color = Palette.sub, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, null, tint = Palette.text, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(18.dp))
        }
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        value?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = valueColor) }
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.none, modifier = Modifier.size(32.dp))
        }
    }
}

/** 선택 칩: 회색 바탕, 선택 시 네이비. */
@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(ButtonShape)
            .background(if (selected) Palette.navy else Palette.card)
            .border(1.dp, if (selected) Palette.navy else Palette.outline, ButtonShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 18.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = if (selected) Color.White else Palette.text)
    }
}

@Composable
fun SectionTitle(text: String, sub: String? = null, color: Color = Palette.text) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = color)
        sub?.let { Text(it, color = Palette.sub, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun Caption(text: String, color: Color = Palette.sub) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = color)
}
