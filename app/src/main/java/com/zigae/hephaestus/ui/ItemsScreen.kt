package com.zigae.hephaestus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zigae.hephaestus.AppGraph
import com.zigae.hephaestus.data.AppData
import com.zigae.hephaestus.domain.DueLevel
import com.zigae.hephaestus.domain.DueStatus
import com.zigae.hephaestus.domain.Interval
import com.zigae.hephaestus.domain.MaintenanceItem
import com.zigae.hephaestus.domain.Powertrain
import com.zigae.hephaestus.domain.Reminders
import kotlinx.coroutines.delay

/** 교체 단계 → 상태 색 세트 (여유 = 초록, 곧 = 노랑·주황, 지남 = 빨강). 기록 없음은 null. */
@Composable
fun levelTone(level: DueLevel): Tone? = when (level) {
    DueLevel.OVERDUE -> Palette.danger
    DueLevel.SOON -> Palette.warning
    DueLevel.OK -> Palette.success
    DueLevel.NO_RECORD -> null
}

@Composable
fun dueStatuses(graph: AppGraph, data: AppData, mode: Powertrain): List<DueStatus> =
    Reminders.all(mode, { data.interval(it, mode)!! }, data.records, graph.detector.odometerKm(), System.currentTimeMillis())

/** 소모품 전체: 단계별 묶음. 항목을 누르면 상세 시트에서 기록·주기 변경. */
@Composable
fun ItemsScreen(graph: AppGraph, data: AppData, mode: Powertrain) {
    val due = dueStatuses(graph, data, mode)
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) { if (message != null) { delay(5_000); message = null } }

    val sections = listOf(
        DueLevel.OVERDUE to "교체 필요",
        DueLevel.SOON to "곧 교체",
        DueLevel.OK to "여유 있음",
        DueLevel.NO_RECORD to "기록 없음 · 마지막 교체를 입력하면 알려드려요",
    )
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("소모품", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("${mode.label} · ${due.size}개 항목", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
        }
        Spacer(Modifier.height(16.dp))
        message?.let {
            AlertBanner(it, Palette.success, Icons.Filled.CheckCircle)
            Spacer(Modifier.height(16.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            sections.forEach { (level, title) ->
                val rows = due.filter { it.level == level }
                if (rows.isEmpty()) return@forEach
                item(key = level.name) {
                    Panel(padding = 0.dp) {
                        Row(Modifier.padding(horizontal = 28.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            levelTone(level)?.let { StatusBadge("${rows.size}", it, strong = level == DueLevel.OVERDUE); Spacer(Modifier.width(12.dp)) }
                            Text(title, style = MaterialTheme.typography.titleMedium, color = if (level == DueLevel.NO_RECORD) Palette.sub else Palette.text)
                        }
                        HorizontalDivider(color = Palette.outline)
                        rows.forEachIndexed { i, s ->
                            DueRow(s, Modifier.padding(horizontal = 28.dp)) { open = s.item.name }
                            if (i < rows.lastIndex) HorizontalDivider(color = Palette.outline, modifier = Modifier.padding(horizontal = 28.dp))
                        }
                    }
                }
            }
        }
    }

    open?.let { key ->
        due.firstOrNull { it.item.name == key }?.let { s ->
            ItemSheet(graph, data, mode, s, onDone = { msg -> message = msg; open = null }, onClose = { open = null })
        }
    }
}

/** 목록 행: 항목 + 남은 거리 + 얇은 막대 + 마지막 교체. */
@Composable
fun DueRow(s: DueStatus, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tone = levelTone(s.level)
    Row(modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.item.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    if (s.level == DueLevel.NO_RECORD) "입력하기" else Format.remaining(s),
                    style = MaterialTheme.typography.titleMedium,
                    color = when (s.level) {
                        DueLevel.NO_RECORD -> Palette.info.base
                        DueLevel.OK -> Palette.text
                        DueLevel.SOON -> Palette.warning.onContainer
                        DueLevel.OVERDUE -> Palette.danger.base
                    },
                )
            }
            s.progress?.let {
                Spacer(Modifier.height(12.dp))
                ProgressLine(it, tone?.base ?: Palette.none)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                s.last?.let { "마지막 교체 ${Format.date(it.timeMillis)} · ${Format.km(it.odometerKm)}" + (it.shopName?.let { n -> " · $n" } ?: "") }
                    ?: "교체 주기 ${Format.interval(s.interval)}",
                color = Palette.sub, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.none, modifier = Modifier.size(36.dp))
    }
}

private enum class Step { MAIN, ODO, INTERVAL }

/**
 * 항목 상세 시트. 기본 동작은 "지금 교체했어요" 한 번.
 * 다른 주행거리·교체 주기는 버튼으로만 조정한다(차 안에서 타이핑 없음).
 */
@Composable
fun ItemSheet(graph: AppGraph, data: AppData, mode: Powertrain, s: DueStatus, onDone: (String) -> Unit, onClose: () -> Unit) {
    val nowKm = graph.detector.odometerKm()
    var step by rememberSaveable(s.item.name) { mutableStateOf(Step.MAIN) }
    var km by rememberSaveable(s.item.name) { mutableStateOf(nowKm) }
    var iv by remember(s.item.name) { mutableStateOf(s.interval) }
    val default = s.item.defaultInterval(mode)!!

    fun nextDueText(atKm: Int, interval: Interval) = when {
        interval.km > 0 -> "다음 교체 ${Format.km(atKm + interval.km)}"
        interval.months > 0 -> "다음 교체 ${interval.months}개월 뒤"
        else -> ""
    }

    fun record(atKm: Int) {
        graph.repo.addRecords(listOf(s.item), atKm, System.currentTimeMillis(), null)
        onDone("${s.item.label} 기록했어요 · ${Format.km(atKm)} · ${nextDueText(atKm, s.interval)}")
    }

    Sheet(onDismiss = onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.item.label, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            when (s.level) {
                DueLevel.OVERDUE -> StatusBadge("교체 필요", Palette.danger, strong = true)
                DueLevel.SOON -> StatusBadge("곧 교체", Palette.warning, strong = false)
                DueLevel.OK -> StatusBadge("여유 있음", Palette.success, strong = false)
                DueLevel.NO_RECORD -> {}
            }
        }
        Spacer(Modifier.height(24.dp))
        when (step) {
            Step.MAIN -> {
                if (s.level != DueLevel.NO_RECORD) {
                    Text(Format.remaining(s), style = MaterialTheme.typography.displaySmall, color = levelTone(s.level)?.base ?: Palette.text)
                    s.progress?.let { Spacer(Modifier.height(12.dp)); ProgressLine(it, levelTone(s.level)?.base ?: Palette.none) }
                    Spacer(Modifier.height(20.dp))
                } else {
                    Text("마지막으로 교체한 시점을 알려주시면 다음 교체 시기를 챙겨드려요.", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
                    Spacer(Modifier.height(20.dp))
                }
                ListRow(null, "마지막 교체", s.last?.let { "${Format.date(it.timeMillis)} · ${Format.km(it.odometerKm)}" + (it.shopName?.let { n -> " · $n" } ?: "") } ?: "기록 없음")
                HorizontalDivider(color = Palette.outline)
                ListRow(null, "교체 주기", Format.interval(s.interval) + if (s.item in data.intervalOverrides) " (변경함)" else "", valueColor = Palette.info.base) { step = Step.INTERVAL }
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PrimaryButton(if (s.level == DueLevel.NO_RECORD) "지금 주행거리로 입력 · ${Format.km(nowKm)}" else "지금 교체했어요 · ${Format.km(nowKm)}", Modifier.weight(1.3f)) { record(nowKm) }
                    SecondaryButton("다른 주행거리로", Modifier.weight(1f)) { km = nowKm; step = Step.ODO }
                }
                TextButton(onClick = onClose, modifier = Modifier.padding(top = 8.dp).align(Alignment.End)) {
                    Text("닫기", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
                }
            }
            Step.ODO -> {
                Caption("교체한 시점의 주행거리")
                Spacer(Modifier.height(12.dp))
                OdometerPicker(nowKm, km) { km = it }
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SecondaryButton("뒤로", Modifier.weight(1f)) { step = Step.MAIN }
                    PrimaryButton("${Format.km(km)}에 교체한 걸로 기록", Modifier.weight(1.6f)) { record(km) }
                }
            }
            Step.INTERVAL -> {
                Caption("교체 주기")
                Spacer(Modifier.height(12.dp))
                IntervalPicker(iv, default) { iv = it }
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SecondaryButton("뒤로", Modifier.weight(1f)) { iv = s.interval; step = Step.MAIN }
                    SecondaryButton("기본값으로", Modifier.weight(1f)) { iv = default }
                    PrimaryButton("${Format.interval(iv)}로 저장", Modifier.weight(1.4f)) {
                        graph.repo.update { d ->
                            d.copy(intervalOverrides = if (iv == default) d.intervalOverrides - s.item else d.intervalOverrides + (s.item to iv))
                        }
                        onDone("${s.item.label} 교체 주기를 ${Format.interval(iv)}로 바꿨어요")
                    }
                }
            }
        }
    }
}

