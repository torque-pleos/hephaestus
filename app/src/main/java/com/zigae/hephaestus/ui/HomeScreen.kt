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
 * 홈 = "지금 할 일". 왼쪽: 정비 기록 질문(있으면 맨 위) + 차량 상태. 오른쪽: 신경 쓸 항목만(차량 경고·교체 필요·곧 교체).
 * 색은 숫자에만 쓰고, 상태는 문장이 아니라 라벨로 적는다.
 */
@Composable
fun HomeScreen(graph: AppGraph, data: AppData, mode: Powertrain, onOpenItems: () -> Unit) {
    val vehicle by graph.vehicle.state.collectAsStateWithLifecycle()
    val navi by graph.navi.state.collectAsStateWithLifecycle()
    val status by graph.detector.status.collectAsStateWithLifecycle()
    val odo = graph.detector.odometerKm()
    val due = dueStatuses(graph, data, mode)
    val attention = due.filter { it.warning || it.level == DueLevel.OVERDUE || it.level == DueLevel.SOON }
    val noRecord = due.count { it.level == DueLevel.NO_RECORD }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(saved) { if (saved != null) { listState.animateScrollToItem(0); delay(6_000); saved = null } }

    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        LazyColumn(Modifier.weight(0.46f).fillMaxHeight(), state = listState, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            saved?.let { item { SavedNote(it) } }
            items(data.prompts.sortedByDescending { it.createdAt }, key = { it.id }) { p ->
                PromptCard(p, mode, due, onSave = { items ->
                    // 신호 질문의 shopName은 "차량 신호" → 이력에서 정비소 자리에 출처로 남는다
                    graph.repo.addRecords(items, p.odometerKm, p.createdAt, p.shopName)
                    if (p.source == ShopSource.DESTINATION || p.source == ShopSource.MAP_SEARCH) {
                        graph.repo.addPlace(p.shopName, p.at, Place.Kind.MY_SHOP) // 다음부터는 바로 알아본다
                    }
                    graph.repo.dismissPrompt(p.id)
                    graph.notifier.cancelVisit(p.id)
                    saved = "${items.joinToString(", ") { it.label }} 기록됨 · ${Format.km(p.odometerKm)}"
                }, onSkip = {
                    graph.repo.dismissPrompt(p.id)
                    graph.notifier.cancelVisit(p.id)
                }, onNotShop = {
                    graph.repo.addPlace(p.shopName, p.at, Place.Kind.IGNORED)
                    graph.repo.dismissPrompt(p.id)
                    graph.notifier.cancelVisit(p.id)
                    saved = "${p.shopName} · 정비소 아닌 곳으로 저장"
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
                    Spacer(Modifier.height(16.dp))
                    Rule()
                    Spacer(Modifier.height(16.dp))
                    val parked = vehicle.gear == Gear.PARK
                    val place = navi.address ?: "위치 확인 중"
                    val since = status.parkedSince?.let { " · ${Format.time(it)}부터" } ?: ""
                    when {
                        vehicle.gear == Gear.UNKNOWN -> StatusLine("차량 정보 확인 중")
                        !parked -> StatusLine("주행 중")
                        status.shop != null -> StatusLine("${status.shop!!.name}$since · 출발할 때 정비 내용을 물어요", Palette.primaryTone, Icons.Filled.Build)
                        status.resolving -> StatusLine("정차 중 · $place · 정비소 확인 중")
                        else -> StatusLine("정차 중 · $place$since")
                    }
                }
            }
        }
        Panel(Modifier.weight(0.54f).fillMaxHeight(), padding = 0.dp) {
            GroupHeader("지금 신경 쓸 항목", trailing = "전체 ${due.size}", onClick = onOpenItems)
            Rule()
            Column(Modifier.padding(horizontal = 28.dp)) {
                if (attention.isEmpty()) {
                    Text("급한 항목 없음", style = MaterialTheme.typography.bodyLarge, color = Palette.sub, modifier = Modifier.padding(vertical = 22.dp))
                    due.firstOrNull { it.level == DueLevel.OK }?.let {
                        Rule()
                        DueRow(it) { open = it.item.name }
                    }
                } else {
                    val shown = attention.take(5)
                    shown.forEachIndexed { i, s ->
                        DueRow(s) { open = s.item.name }
                        if (i < shown.lastIndex) Rule()
                    }
                }
                if (noRecord > 0) {
                    Rule()
                    ListRow(null, "기록 없음", "$noRecord", onClick = onOpenItems)
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

/** 정비 기록 질문. 화면에서 유일하게 색 면을 쓰는 블록. 교체 예정이던 항목을 앞에 두어 바로 고르게 한다. */
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
    // 차량 신호(레벨·경고등 꺼짐)로 정비된 것으로 보이는 항목은 미리 골라 둔다
    val fromSignals = p.signals.map { it.item }.distinct()
    // 주간/야간 전환 때 화면이 다시 만들어져도 고른 항목을 유지한다
    var selectedKeys by rememberSaveable(p.id, p.signals.size) { mutableStateOf(fromSignals.map { it.name }) }
    val selected = selectedKeys.mapNotNull(MaintenanceItem::fromKey).toSet()
    val likely = due.filter { it.level == DueLevel.OVERDUE || it.level == DueLevel.SOON }.map { it.item }.filterNot { it in fromSignals }
    val others = MaintenanceItem.forMode(mode).filterNot { it in likely || it in fromSignals }
    fun toggle(item: MaintenanceItem) { selectedKeys = if (item.name in selectedKeys) selectedKeys - item.name else selectedKeys + item.name }

    @Composable
    fun chips(items: List<MaintenanceItem>) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { item -> SelectChip(item.label, item in selected) { toggle(item) } }
        }
    }

    HeaderPanel(header = {
        Text("${Format.time(p.createdAt)} · ${Format.km(p.odometerKm)} 자동 기록", style = MaterialTheme.typography.bodyMedium, color = Palette.onHeader.copy(alpha = 0.75f))
        Spacer(Modifier.height(4.dp))
        Text(
            if (p.isSignalOnly) p.signals.joinToString(" · ") { it.clearedTitle } else "${p.shopName}에서 출발하시네요",
            style = MaterialTheme.typography.titleLarge, color = Palette.onHeader,
        )
    }) {
        Text(if (p.isSignalOnly) "교체하셨나요? 보충만 했다면 기록하지 않아요." else "어떤 정비를 받으셨나요?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        if (fromSignals.isNotEmpty()) {
            Caption("차량 신호로 확인됨")
            Spacer(Modifier.height(8.dp))
            chips(fromSignals)
            Spacer(Modifier.height(16.dp))
        }
        if (likely.isNotEmpty()) {
            Caption("교체 예정이던 항목")
            Spacer(Modifier.height(8.dp))
            chips(likely)
            Spacer(Modifier.height(16.dp))
        }
        if (fromSignals.isNotEmpty() || likely.isNotEmpty()) {
            Caption("그 밖의 항목")
            Spacer(Modifier.height(8.dp))
        }
        chips(others)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            SecondaryButton(if (p.isSignalOnly) "보충만 했어요" else "정비 안 받음", Modifier.weight(1f), onSkip)
            PrimaryButton(if (selected.isEmpty()) "받은 정비를 골라 주세요" else "${selected.size}개 기록하기", Modifier.weight(1.4f), enabled = selected.isNotEmpty()) { onSave(selected) }
        }
        if (p.source != ShopSource.MY_SHOP && p.source != ShopSource.DEMO && !p.isSignalOnly) {
            TextButton(onClick = onNotShop, modifier = Modifier.padding(top = 8.dp)) {
                Text("여기는 정비소가 아니에요", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
            }
        }
    }
}
