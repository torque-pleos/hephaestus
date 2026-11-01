package com.zigae.hephaestus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigae.hephaestus.AppGraph
import com.zigae.hephaestus.data.AppData
import com.zigae.hephaestus.domain.DueLevel
import com.zigae.hephaestus.domain.DueStatus
import com.zigae.hephaestus.domain.Gear
import com.zigae.hephaestus.domain.MaintenanceItem
import com.zigae.hephaestus.domain.Place
import com.zigae.hephaestus.domain.Powertrain
import com.zigae.hephaestus.domain.Reminders
import com.zigae.hephaestus.domain.ShopSource
import com.zigae.hephaestus.domain.VisitPrompt
import java.text.NumberFormat
import java.util.Locale

/** 교체 단계 → 상태 색 세트 (여유 = 초록, 곧 = 노랑·주황, 지남 = 빨강). 기록 없음은 null. */
@Composable
fun levelTone(level: DueLevel): Tone? = when (level) {
    DueLevel.OVERDUE -> Palette.danger
    DueLevel.SOON -> Palette.warning
    DueLevel.OK -> Palette.success
    DueLevel.NO_RECORD -> null
}

@Composable
fun HomeScreen(graph: AppGraph, data: AppData, mode: Powertrain) {
    val vehicle by graph.vehicle.state.collectAsStateWithLifecycle()
    val navi by graph.navi.state.collectAsStateWithLifecycle()
    val status by graph.detector.status.collectAsStateWithLifecycle()
    val odo = (vehicle.odometerKm ?: 0f).toInt() + data.demoOdometerOffsetKm
    val due = Reminders.all(mode, { data.interval(it, mode)!! }, data.records, odo, System.currentTimeMillis())
    var editing by remember { mutableStateOf<DueStatus?>(null) }

    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        LazyColumn(Modifier.weight(0.45f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(mode.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        GearIndicator(vehicle.gear)
                    }
                    Spacer(Modifier.height(12.dp))
                    Caption("총 주행거리")
                    BigNumber(NumberFormat.getIntegerInstance(Locale.KOREA).format(odo), "km")
                    Spacer(Modifier.height(16.dp))
                    val parked = vehicle.gear == Gear.PARK
                    val place = navi.address ?: "위치 확인 중"
                    AlertBanner(
                        when {
                            !parked && vehicle.gear == Gear.UNKNOWN -> "차량 정보를 불러오는 중이에요"
                            !parked -> "주행 중이에요"
                            status.shop != null -> "${status.shop!!.name}에 정차 중이에요"
                            status.resolving -> "$place · 정비소인지 확인하고 있어요"
                            else -> "$place · ${status.parkedSince?.let { Format.time(it) + "부터 " } ?: ""}정차 중"
                        },
                        tone = if (status.shop != null && parked) Palette.info else null,
                        icon = if (status.shop != null && parked) Icons.Filled.Build else if (parked) Icons.Filled.LocationOn else Icons.Filled.Info,
                    )
                }
            }
            items(data.prompts.sortedByDescending { it.createdAt }, key = { it.id }) { p ->
                PromptCard(p, mode, onSave = { items ->
                    graph.repo.addRecords(items, p.odometerKm, p.createdAt, p.shopName)
                    if (p.source == ShopSource.DESTINATION || p.source == ShopSource.MAP_SEARCH) {
                        graph.repo.addPlace(p.shopName, p.at, Place.Kind.MY_SHOP) // 다음부터는 바로 알아본다
                    }
                    graph.repo.dismissPrompt(p.id)
                    graph.notifier.cancelVisit(p.id)
                }, onSkip = {
                    graph.repo.dismissPrompt(p.id)
                    graph.notifier.cancelVisit(p.id)
                }, onNotShop = {
                    graph.repo.addPlace(p.shopName, p.at, Place.Kind.IGNORED)
                    graph.repo.dismissPrompt(p.id)
                    graph.notifier.cancelVisit(p.id)
                })
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatTile(due.count { it.level == DueLevel.OVERDUE }.toString(), "교체 시기 지남", Palette.danger.base, Modifier.weight(1f))
                    StatTile(due.count { it.level == DueLevel.SOON }.toString(), "곧 교체", Palette.warning.onContainer, Modifier.weight(1f))
                    StatTile(data.records.size.toString(), "정비 기록", Palette.text, Modifier.weight(1f))
                }
            }
        }
        Column(Modifier.weight(0.55f).fillMaxHeight().clip(PanelShape).background(Palette.card).padding(horizontal = 28.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("소모품 관리", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text("교체가 급한 순", style = MaterialTheme.typography.bodyMedium, color = Palette.sub)
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(due, key = { it.item.name }) { s ->
                    DueRow(s) { editing = s }
                    HorizontalDivider(color = Palette.outline)
                }
            }
        }
    }

    editing?.let { s ->
        RecordDialog(s, odo, onDismiss = { editing = null }) { km ->
            graph.repo.addRecords(listOf(s.item), km, System.currentTimeMillis(), null)
            editing = null
        }
    }
}

/** 네이비 머리 블록 카드: 어디서 출발했는지 + 받은 정비 선택. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptCard(
    p: VisitPrompt,
    mode: Powertrain,
    onSave: (Set<MaintenanceItem>) -> Unit,
    onSkip: () -> Unit,
    onNotShop: () -> Unit,
) {
    // 주간/야간 전환 때 화면이 다시 만들어져도 고른 항목을 유지한다
    var selectedKeys by rememberSaveable(p.id) { mutableStateOf(emptyList<String>()) }
    val selected = selectedKeys.mapNotNull(MaintenanceItem::fromKey).toSet()
    HeaderPanel(header = {
        Text("${Format.time(p.createdAt)} · ${Format.km(p.odometerKm)}", style = MaterialTheme.typography.bodyMedium, color = Palette.onHeader.copy(alpha = 0.75f))
        Spacer(Modifier.height(4.dp))
        Text("${p.shopName}에서 출발하시네요", style = MaterialTheme.typography.titleLarge, color = Palette.onHeader)
    }) {
        Text("어떤 정비를 받으셨나요?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(20.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MaintenanceItem.forMode(mode).forEach { item ->
                val on = item in selected
                SelectChip(item.label, on) { selectedKeys = if (on) selectedKeys - item.name else selectedKeys + item.name }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onSkip, shape = ButtonShape, modifier = Modifier.weight(1f).height(76.dp)) {
                Text("정비 안 받음", style = MaterialTheme.typography.labelLarge, color = Palette.text)
            }
            Button(
                onClick = { onSave(selected) },
                enabled = selected.isNotEmpty(),
                shape = ButtonShape,
                modifier = Modifier.weight(1.4f).height(76.dp),
                colors = ButtonDefaults.buttonColors(disabledContainerColor = Palette.cardHigh, disabledContentColor = Palette.none),
            ) {
                Text(if (selected.isEmpty()) "받은 정비를 골라 주세요" else "${selected.size}개 기록하기", style = MaterialTheme.typography.labelLarge)
            }
        }
        if (p.source != ShopSource.MY_SHOP && p.source != ShopSource.DEMO) {
            TextButton(onClick = onNotShop, modifier = Modifier.padding(top = 8.dp)) {
                Text("여기는 정비소가 아니에요", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
            }
        }
    }
}

/** 목록 행: 항목 + 남은 거리 + 얇은 막대 + 마지막 교체. */
@Composable
private fun DueRow(s: DueStatus, onClick: () -> Unit) {
    val tone = levelTone(s.level)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.item.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                when (s.level) {
                    DueLevel.OVERDUE -> StatusBadge("교체 필요", Palette.danger, strong = true)
                    DueLevel.SOON -> StatusBadge("곧 교체", Palette.warning, strong = false)
                    else -> {}
                }
                if (s.level == DueLevel.OVERDUE || s.level == DueLevel.SOON) Spacer(Modifier.width(14.dp))
                Text(
                    if (s.level == DueLevel.NO_RECORD) "기록 없음" else Format.remaining(s),
                    style = MaterialTheme.typography.titleMedium,
                    color = when (s.level) {
                        DueLevel.NO_RECORD -> Palette.none
                        DueLevel.OK -> Palette.text
                        DueLevel.SOON -> Palette.warning.onContainer
                        DueLevel.OVERDUE -> Palette.danger.base
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            s.progress?.let {
                ProgressLine(it, tone?.base ?: Palette.none)
                Spacer(Modifier.height(12.dp))
            }
            Text(
                s.last?.let { "마지막 교체 ${Format.date(it.timeMillis)} · ${Format.km(it.odometerKm)}" + (it.shopName?.let { n -> " · $n" } ?: "") }
                    ?: "교체 주기 ${Format.interval(s.interval)} · 마지막 교체를 입력해 주세요",
                color = Palette.sub, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.none, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun RecordDialog(s: DueStatus, currentKm: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf(currentKm.toString()) }
    val km = text.filter { it.isDigit() }.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.card,
        title = { Text(if (s.last == null) "${s.item.label} 마지막 교체" else "${s.item.label} 교체 기록") },
        text = {
            Column {
                Text("교체한 시점의 주행거리를 입력해 주세요 (현재 ${Format.km(currentKm)})", color = Palette.sub)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(7) },
                    suffix = { Text("km") },
                    singleLine = true, textStyle = MaterialTheme.typography.titleMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = { Button(onClick = { km?.let(onSave) }, enabled = km != null, shape = ButtonShape) { Text("기록") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = Palette.sub) } },
    )
}

/** 설정 화면에서 쓰는 카드 (예전 HCard 호환). */
@Composable
fun HCard(@Suppress("UNUSED_PARAMETER") highlight: Boolean = false, content: @Composable () -> Unit) = Panel(content = content)
