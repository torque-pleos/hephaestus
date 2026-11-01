package com.zigae.hephaestus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.HorizontalDivider
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
import com.zigae.hephaestus.domain.ShopSource
import com.zigae.hephaestus.domain.VisitPrompt
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

/**
 * 홈 = "지금 할 일". 왼쪽: 정비 기록 질문(있으면 맨 위) + 차량 상태. 오른쪽: 신경 쓸 항목만(교체 필요·곧 교체).
 * 전체 목록·기록 없는 항목은 소모품 탭으로 보낸다.
 */
@Composable
fun HomeScreen(graph: AppGraph, data: AppData, mode: Powertrain, onOpenItems: () -> Unit) {
    val vehicle by graph.vehicle.state.collectAsStateWithLifecycle()
    val navi by graph.navi.state.collectAsStateWithLifecycle()
    val status by graph.detector.status.collectAsStateWithLifecycle()
    val odo = graph.detector.odometerKm()
    val due = dueStatuses(graph, data, mode)
    val attention = due.filter { it.level == DueLevel.OVERDUE || it.level == DueLevel.SOON }
    val noRecord = due.count { it.level == DueLevel.NO_RECORD }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(saved) { if (saved != null) { listState.animateScrollToItem(0); delay(6_000); saved = null } }

    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        LazyColumn(Modifier.weight(0.46f).fillMaxHeight(), state = listState, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            saved?.let { item { AlertBanner(it, Palette.success, Icons.Filled.CheckCircle) } }
            items(data.prompts.sortedByDescending { it.createdAt }, key = { it.id }) { p ->
                PromptCard(p, mode, due, onSave = { items ->
                    graph.repo.addRecords(items, p.odometerKm, p.createdAt, p.shopName)
                    if (p.source == ShopSource.DESTINATION || p.source == ShopSource.MAP_SEARCH) {
                        graph.repo.addPlace(p.shopName, p.at, Place.Kind.MY_SHOP) // 다음부터는 바로 알아본다
                    }
                    graph.repo.dismissPrompt(p.id)
                    graph.notifier.cancelVisit(p.id)
                    saved = "${items.joinToString(", ") { it.label }} 기록했어요 · ${Format.km(p.odometerKm)}"
                }, onSkip = {
                    graph.repo.dismissPrompt(p.id)
                    graph.notifier.cancelVisit(p.id)
                }, onNotShop = {
                    graph.repo.addPlace(p.shopName, p.at, Place.Kind.IGNORED)
                    graph.repo.dismissPrompt(p.id)
                    graph.notifier.cancelVisit(p.id)
                    saved = "${p.shopName}은(는) 정비소가 아닌 곳으로 기억할게요"
                })
            }
            item {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Caption("${mode.label} · 총 주행거리")
                            BigNumber(NumberFormat.getIntegerInstance(Locale.KOREA).format(odo), "km")
                        }
                        GearIndicator(vehicle.gear)
                    }
                    Spacer(Modifier.height(12.dp))
                    val parked = vehicle.gear == Gear.PARK
                    val place = navi.address ?: "위치 확인 중"
                    AlertBanner(
                        when {
                            vehicle.gear == Gear.UNKNOWN -> "차량 정보를 불러오는 중이에요"
                            !parked -> "주행 중이에요"
                            status.shop != null -> "${status.shop!!.name}에 정차 중 · 출발하면 정비 내용을 여쭤볼게요"
                            status.resolving -> "$place · 정비소인지 확인하고 있어요"
                            else -> "$place · ${status.parkedSince?.let { Format.time(it) + "부터 " } ?: ""}정차 중"
                        },
                        tone = if (status.shop != null && parked) Palette.info else null,
                        icon = if (status.shop != null && parked) Icons.Filled.Build else if (parked) Icons.Filled.LocationOn else Icons.Filled.Info,
                    )
                }
            }
        }
        Panel(Modifier.weight(0.54f).fillMaxHeight(), padding = 0.dp) {
            Row(Modifier.padding(horizontal = 28.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("지금 신경 쓸 항목", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenItems) { Text("전체 ${due.size}개 보기", style = MaterialTheme.typography.bodyLarge, color = Palette.info.base) }
            }
            HorizontalDivider(color = Palette.outline)
            Column(Modifier.padding(horizontal = 28.dp)) {
                if (attention.isEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    AlertBanner("교체가 급한 소모품이 없어요", Palette.success, Icons.Filled.CheckCircle)
                    due.firstOrNull { it.level == DueLevel.OK }?.let {
                        Spacer(Modifier.height(8.dp))
                        Caption("가장 먼저 돌아오는 항목")
                        DueRow(it) { open = it.item.name }
                    }
                } else {
                    val shown = attention.take(5)
                    shown.forEachIndexed { i, s ->
                        DueRow(s) { open = s.item.name }
                        if (i < shown.lastIndex) HorizontalDivider(color = Palette.outline)
                    }
                }
                if (noRecord > 0) {
                    HorizontalDivider(color = Palette.outline)
                    ListRow(null, "기록 없는 항목 ${noRecord}개", "입력하면 알려드려요", valueColor = Palette.info.base, onClick = onOpenItems)
                }
            }
        }
    }

    open?.let { key ->
        due.firstOrNull { it.item.name == key }?.let { s ->
            ItemSheet(graph, data, mode, s, onDone = { saved = it; open = null }, onClose = { open = null })
        }
    }
}

/** 정비 기록 질문. 교체 예정이던 항목을 앞에 두어 바로 고르게 한다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptCard(
    p: VisitPrompt,
    mode: Powertrain,
    due: List<DueStatus>,
    onSave: (Set<MaintenanceItem>) -> Unit,
    onSkip: () -> Unit,
    onNotShop: () -> Unit,
) {
    // 주간/야간 전환 때 화면이 다시 만들어져도 고른 항목을 유지한다
    var selectedKeys by rememberSaveable(p.id) { mutableStateOf(emptyList<String>()) }
    val selected = selectedKeys.mapNotNull(MaintenanceItem::fromKey).toSet()
    val likely = due.filter { it.level == DueLevel.OVERDUE || it.level == DueLevel.SOON }.map { it.item }
    val others = MaintenanceItem.forMode(mode).filterNot { it in likely }
    fun toggle(item: MaintenanceItem) { selectedKeys = if (item.name in selectedKeys) selectedKeys - item.name else selectedKeys + item.name }

    HeaderPanel(header = {
        Text("${Format.time(p.createdAt)} · ${Format.km(p.odometerKm)} 자동 기록", style = MaterialTheme.typography.bodyMedium, color = Palette.onHeader.copy(alpha = 0.75f))
        Spacer(Modifier.height(4.dp))
        Text("${p.shopName}에서 출발하시네요", style = MaterialTheme.typography.titleLarge, color = Palette.onHeader)
    }) {
        Text("어떤 정비를 받으셨나요?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        if (likely.isNotEmpty()) {
            Caption("교체 예정이던 항목")
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                likely.forEach { item -> SelectChip(item.label, item in selected) { toggle(item) } }
            }
            Spacer(Modifier.height(16.dp))
            Caption("그 밖의 항목")
            Spacer(Modifier.height(8.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            others.forEach { item -> SelectChip(item.label, item in selected) { toggle(item) } }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            SecondaryButton("정비 안 받음", Modifier.weight(1f), onSkip)
            PrimaryButton(if (selected.isEmpty()) "받은 정비를 골라 주세요" else "${selected.size}개 기록하기", Modifier.weight(1.4f), enabled = selected.isNotEmpty()) { onSave(selected) }
        }
        if (p.source != ShopSource.MY_SHOP && p.source != ShopSource.DEMO) {
            TextButton(onClick = onNotShop, modifier = Modifier.padding(top = 8.dp)) {
                Text("여기는 정비소가 아니에요", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
            }
        }
    }
}
