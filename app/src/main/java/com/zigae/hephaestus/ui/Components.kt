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

/** 카드. */
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

/** 강조색 머리 블록 + 본문 카드. */
@Composable
fun HeaderPanel(header: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(PanelShape).background(Palette.card)) {
        Column(Modifier.fillMaxWidth().background(Palette.headerBg).padding(horizontal = 28.dp, vertical = 24.dp)) { header() }
        Column(Modifier.padding(28.dp)) { content() }
    }
}

/** 숫자 + 작은 단위. */
@Composable
fun BigNumber(value: String, unit: String, color: Color = Palette.text, small: Boolean = false) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = if (small) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayLarge, color = color)
        Spacer(Modifier.width(6.dp))
        Text(unit, style = MaterialTheme.typography.titleMedium, color = Palette.sub, modifier = Modifier.padding(bottom = if (small) 8.dp else 14.dp))
    }
}

/** 얇은 진행 막대. */
@Composable
fun ProgressLine(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Palette.cardHigh)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}

/** 기어 표시: 현재 기어만 강조색 칸. */
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
                Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(if (on) Palette.primary else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(g, style = MaterialTheme.typography.labelLarge, color = if (on) Palette.onPrimary else Palette.none)
            }
        }
    }
}

/**
 * 알림 배너: 색 컨테이너 면 + 진한 테두리 + 진한 글자.
 * [tone]이 null이면 회색 안내.
 */
@Composable
fun AlertBanner(text: String, tone: Tone? = null, icon: ImageVector = Icons.Filled.Info) {
    val bg = tone?.container ?: Palette.cardHigh
    val fg = tone?.onContainer ?: Palette.text
    val border = tone?.base?.copy(alpha = 0.5f) ?: Palette.cardHigh
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg)
            .border(1.5.dp, border, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tone?.base ?: Palette.sub, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = fg)
    }
}

/** 상태 배지: strong = 채움(기본색 + 그 위 글자색), weak = 연한 면(컨테이너 + 컨테이너 글자색). */
@Composable
fun StatusBadge(text: String, tone: Tone, strong: Boolean) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (strong) tone.on else tone.onContainer,
        modifier = Modifier.clip(ButtonShape).background(if (strong) tone.base else tone.container)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
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
            Text("건", style = MaterialTheme.typography.bodyLarge, color = Palette.sub, modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}

/** 아이콘 + 라벨 + 값 + 꺾쇠 목록 행. */
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

/** 선택 칩: 카드색 바탕, 선택 시 강조색. */
@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(ButtonShape)
            .background(if (selected) Palette.primary else Palette.card)
            .border(1.dp, if (selected) Palette.primary else Palette.outline, ButtonShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 18.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = if (selected) Palette.onPrimary else Palette.text)
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

// ---- 입력·시트 (차량 화면: 타이핑 대신 버튼) ----

/** 가운데 뜨는 큰 시트. */
@Composable
fun Sheet(onDismiss: () -> Unit, width: Dp = 980.dp, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(Modifier.width(width).clip(PanelShape).background(Palette.card).padding(36.dp), content = content)
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    androidx.compose.material3.Button(
        onClick = onClick, enabled = enabled, shape = ButtonShape, modifier = modifier.height(80.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Palette.primary, contentColor = Palette.onPrimary,
            disabledContainerColor = Palette.cardHigh, disabledContentColor = Palette.none,
        ),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick, shape = ButtonShape, modifier = modifier.height(80.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Palette.outline),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = Palette.text) }
}

/** 작은 조정 버튼 (−1,000 / +100 …). */
@Composable
fun StepButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(ButtonShape).background(Palette.cardHigh).clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.bodyLarge, color = Palette.text, maxLines = 1, softWrap = false) }
}

/** 주행거리 고르기: 현재 값에서 버튼으로 조정. 키보드 없음. */
@Composable
fun OdometerPicker(nowKm: Int, value: Int, onChange: (Int) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            BigNumber(java.text.NumberFormat.getIntegerInstance(java.util.Locale.KOREA).format(value), "km")
            Spacer(Modifier.width(20.dp))
            if (value != nowKm) {
                Text(
                    "지금보다 ${Format.km(nowKm - value)} 전",
                    style = MaterialTheme.typography.bodyLarge, color = Palette.sub,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            } else {
                Text("지금 주행거리", style = MaterialTheme.typography.bodyLarge, color = Palette.sub, modifier = Modifier.padding(bottom = 14.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(-10_000, -1_000, -100).forEach { d -> StepButton(Format.km(d).replace("km", "")) { onChange((value + d).coerceAtLeast(0)) } }
            listOf(100, 1_000, 10_000).forEach { d -> StepButton("+" + Format.km(d).replace("km", "")) { onChange(value + d) } }
        }
        if (value != nowKm) {
            Spacer(Modifier.height(12.dp))
            Row { StepButton("지금 주행거리(${Format.km(nowKm)})로 되돌리기") { onChange(nowKm) } }
        }
    }
}

/** 교체 주기 고르기: 거리·기간 프리셋 칩. */
@Composable
fun IntervalPicker(value: com.zigae.hephaestus.domain.Interval, default: com.zigae.hephaestus.domain.Interval, onChange: (com.zigae.hephaestus.domain.Interval) -> Unit) {
    val kms = listOf(0, 5_000, 7_000, 10_000, 15_000, 20_000, 40_000, 60_000, 100_000)
    val months = listOf(0, 6, 12, 24, 36, 48)
    Column {
        Caption("주행거리 기준")
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            kms.forEach { k ->
                SelectChip(if (k == 0) "안 씀" else Format.km(k), value.km == k) {
                    if (k > 0 || value.months > 0) onChange(com.zigae.hephaestus.domain.Interval(k, value.months))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Caption("기간 기준")
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            months.forEach { m ->
                SelectChip(if (m == 0) "안 씀" else if (m % 12 == 0) "${m / 12}년" else "${m}개월", value.months == m) {
                    if (m > 0 || value.km > 0) onChange(com.zigae.hephaestus.domain.Interval(value.km, m))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("기본값 ${Format.interval(default)}", style = MaterialTheme.typography.bodyMedium, color = Palette.sub)
    }
}
