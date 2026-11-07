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
import androidx.compose.material.icons.filled.Warning
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
fun dueStatuses(graph: AppGraph, data: AppData, mode: Powertrain): List<DueStatus> {
    val signals by graph.detector.signals.collectAsStateWithLifecycle()
    return Reminders.all(
        mode, { data.interval(it, mode)!! }, data.records, graph.detector.odometerKm(), System.currentTimeMillis(),
        warnings = signals.map { it.item }.toSet(),
    )
}

/** 소모품 전체: 한 면 안에서 단계별 묶음. 항목을 누르면 상세 시트에서 기록·주기 변경. */
@Composable
fun ItemsScreen(graph: AppGraph, data: AppData, mode: Powertrain) {
    val due = dueStatuses(graph, data, mode)
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) { if (message != null) { delay(5_000); message = null } }

    // 차량이 지금 경고하는 항목은 교체 단계와 상관없이 맨 위
    val groups = listOf(
        Triple("warning", "차량 경고", due.filter { it.warning }),
        Triple(DueLevel.OVERDUE.name, "교체 필요", due.filter { it.level == DueLevel.OVERDUE && !it.warning }),
        Triple(DueLevel.SOON.name, "곧 교체", due.filter { it.level == DueLevel.SOON && !it.warning }),
        Triple(DueLevel.OK.name, "여유 있음", due.filter { it.level == DueLevel.OK && !it.warning }),
        Triple(DueLevel.NO_RECORD.name, "기록 없음", due.filter { it.level == DueLevel.NO_RECORD && !it.warning }),
    ).filter { it.third.isNotEmpty() }

    Column {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 16.dp)) {
            Text("소모품", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("${mode.label} · ${due.size}", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
        }
        message?.let {
            SavedNote(it)
            Spacer(Modifier.height(16.dp))
        }
        Panel(padding = 0.dp) {
            LazyColumn {
                groups.forEachIndexed { gi, (key, title, rows) ->
                    item(key = "h-$key") {
                        if (gi > 0) Spacer(Modifier.height(12.dp))
                        GroupHeader(
                            title, rows.size,
                            color = when (key) { "warning" -> Palette.danger.base; DueLevel.NO_RECORD.name -> Palette.sub; else -> Palette.text },
                        )
                        Rule()
                    }
                    items(rows, key = { it.item.name }) { s ->
                        DueRow(s, Modifier.padding(horizontal = 28.dp)) { open = s.item.name }
                        if (s !== rows.last()) Rule(Modifier.padding(horizontal = 28.dp))
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

/** 목록 행: 항목 + 남은 거리 + 얇은 막대 + 마지막 교체. 색은 오른쪽 값 하나에만 쓴다. */
@Composable
fun DueRow(s: DueStatus, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tone = levelTone(s.level)
    Row(modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (s.warning) {
                    Icon(Icons.Filled.Warning, null, tint = Palette.danger.base, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(s.item.label, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    when {
                        s.warning -> "차량 경고" // 남은 거리보다 지금 차량이 알리는 상태가 우선
                        s.level == DueLevel.NO_RECORD -> "입력"
                        else -> Format.remaining(s)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        s.warning -> Palette.danger.base
                        s.level == DueLevel.NO_RECORD -> Palette.sub
                        s.level == DueLevel.OK -> Palette.text
                        s.level == DueLevel.SOON -> Palette.warning.onContainer
                        else -> Palette.danger.base
                    },
                )
            }
            s.progress?.let {
                Spacer(Modifier.height(12.dp))
                ProgressLine(it, tone?.base ?: Palette.none)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                (s.last?.let { "마지막 교체 ${Format.date(it.timeMillis)} · ${Format.km(it.odometerKm)}" + (it.shopName?.let { n -> " · $n" } ?: "") }
                    ?: "교체 주기 ${Format.interval(s.interval)}") + if (s.warning && s.level != DueLevel.NO_RECORD) " · ${Format.remaining(s)}" else "",
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
        onDone("${s.item.label} 기록됨 · ${Format.km(atKm)} · ${nextDueText(atKm, s.interval)}")
    }

    Sheet(onDismiss = onClose) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(s.item.label, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text(
                when (s.level) { DueLevel.OVERDUE -> "교체 필요"; DueLevel.SOON -> "곧 교체"; DueLevel.OK -> "여유 있음"; DueLevel.NO_RECORD -> "기록 없음" },
                style = MaterialTheme.typography.bodyLarge, color = levelTone(s.level)?.base ?: Palette.sub,
            )
        }
        Spacer(Modifier.height(24.dp))
        when (step) {
            Step.MAIN -> {
                if (s.level != DueLevel.NO_RECORD) {
                    Text(Format.remaining(s), style = MaterialTheme.typography.displaySmall, color = levelTone(s.level)?.base ?: Palette.text)
                    s.progress?.let { Spacer(Modifier.height(12.dp)); ProgressLine(it, levelTone(s.level)?.base ?: Palette.none) }
                    Spacer(Modifier.height(20.dp))
                } else {
                    Text("마지막 교체를 입력하면 다음 시기를 계산해요.", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
                    Spacer(Modifier.height(20.dp))
                }
                ListRow(null, "마지막 교체", s.last?.let { "${Format.date(it.timeMillis)} · ${Format.km(it.odometerKm)}" + (it.shopName?.let { n -> " · $n" } ?: "") } ?: "기록 없음")
                Rule()
                ListRow(null, "교체 주기", Format.interval(s.interval) + if (s.item in data.intervalOverrides) " (변경함)" else "", valueColor = Palette.text) { step = Step.INTERVAL }
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
                        onDone("${s.item.label} 교체 주기 ${Format.interval(iv)}로 변경")
                    }
                }
            }
        }
    }
}

