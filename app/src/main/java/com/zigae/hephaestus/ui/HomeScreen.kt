package com.zigae.hephaestus.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
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

@Composable
fun HomeScreen(graph: AppGraph, data: AppData, mode: Powertrain) {
    val vehicle by graph.vehicle.state.collectAsStateWithLifecycle()
    val navi by graph.navi.state.collectAsStateWithLifecycle()
    val status by graph.detector.status.collectAsStateWithLifecycle()
    val odo = (vehicle.odometerKm ?: 0f).toInt() + data.demoOdometerOffsetKm
    val due = Reminders.all(mode, { data.interval(it, mode)!! }, data.records, odo, System.currentTimeMillis())
    var editing by remember { mutableStateOf<DueStatus?>(null) }

    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        LazyColumn(Modifier.weight(0.45f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                HCard {
                    Text(mode.label, color = Palette.sub, style = MaterialTheme.typography.bodyMedium)
                    Text(Format.km(odo), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    val gearText = when (vehicle.gear) {
                        Gear.PARK -> status.parkedSince?.let { "정차 중 · ${Format.time(it)}부터" } ?: "정차 중"
                        Gear.UNKNOWN -> "차량 연결 중…"
                        else -> "주행 중"
                    }
                    Text(gearText, style = MaterialTheme.typography.bodyLarge)
                    navi.address?.let { Text(it, color = Palette.sub, style = MaterialTheme.typography.bodyMedium) }
                    if (vehicle.gear == Gear.PARK) {
                        val shopText = when {
                            status.resolving && status.shop == null -> "정비소인지 확인 중…"
                            status.shop != null -> "정비소 감지: ${status.shop!!.name} (${status.shop!!.source.label})"
                            status.ignoredPlace -> "정비소 아님으로 등록한 장소"
                            else -> "정비소 아님"
                        }
                        Text(shopText, color = if (status.shop != null) Palette.accent else Palette.sub, style = MaterialTheme.typography.bodyMedium)
                    }
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
        }
        Column(Modifier.weight(0.55f).fillMaxHeight()) {
            Text("교체 시기", style = MaterialTheme.typography.titleLarge)
            Text("주행거리 기준으로 가장 급한 순", color = Palette.sub, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(due, key = { it.item.name }) { s -> DueRow(s) { editing = s } }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptCard(
    p: VisitPrompt,
    mode: Powertrain,
    onSave: (Set<MaintenanceItem>) -> Unit,
    onSkip: () -> Unit,
    onNotShop: () -> Unit,
) {
    var selected by remember(p.id) { mutableStateOf(emptySet<MaintenanceItem>()) }
    HCard(highlight = true) {
        Text("${p.shopName}에서 출발하시네요", style = MaterialTheme.typography.titleMedium)
        Text(
            "어떤 정비를 받으셨나요? · ${Format.km(p.odometerKm)} 자동 기록 · ${p.source.label}",
            color = Palette.sub, style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MaintenanceItem.forMode(mode).forEach { item ->
                val on = item in selected
                FilterChip(
                    selected = on,
                    onClick = { selected = if (on) selected - item else selected + item },
                    label = { Text(item.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 10.dp)) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onSave(selected) }, enabled = selected.isNotEmpty(), modifier = Modifier.height(64.dp)) {
                Text(if (selected.isEmpty()) "항목을 고르세요" else "${selected.size}개 기록")
            }
            OutlinedButton(onClick = onSkip, modifier = Modifier.height(64.dp)) { Text("정비 안 받음") }
            if (p.source != ShopSource.MY_SHOP && p.source != ShopSource.DEMO) {
                TextButton(onClick = onNotShop, modifier = Modifier.height(64.dp)) { Text("정비소 아님") }
            }
        }
    }
}

@Composable
private fun DueRow(s: DueStatus, onClick: () -> Unit) {
    val color = when (s.level) {
        DueLevel.OVERDUE -> Palette.overdue
        DueLevel.SOON -> Palette.soon
        DueLevel.OK -> Palette.ok
        DueLevel.NO_RECORD -> Palette.none
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Palette.card),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row {
                Text(s.item.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(Format.remaining(s), color = color, style = MaterialTheme.typography.titleMedium)
            }
            s.progress?.let {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { it.coerceIn(0f, 1f) },
                    color = color,
                    trackColor = Palette.cardHigh,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    drawStopIndicator = {},
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                s.last?.let { "마지막 ${Format.date(it.timeMillis)} · ${Format.km(it.odometerKm)}" + (it.shopName?.let { n -> " · $n" } ?: "") }
                    ?: "눌러서 마지막 교체를 입력하세요",
                color = Palette.sub, style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun RecordDialog(s: DueStatus, currentKm: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf(currentKm.toString()) }
    val km = text.filter { it.isDigit() }.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (s.last == null) "${s.item.label} 마지막 교체" else "${s.item.label} 교체 기록") },
        text = {
            Column {
                Text("교체한 시점의 주행거리 (현재 ${Format.km(currentKm)})", color = Palette.sub)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(7) },
                    suffix = { Text("km") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = { Button(onClick = { km?.let(onSave) }, enabled = km != null) { Text("기록") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
fun HCard(highlight: Boolean = false, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (highlight) Palette.cardHigh else Palette.card),
        shape = RoundedCornerShape(20.dp),
        border = if (highlight) androidx.compose.foundation.BorderStroke(2.dp, Palette.accent) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp)) { content() }
    }
}

@Composable
fun SectionTitle(text: String, sub: String? = null, color: Color = Palette.text) {
    Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = color)
        sub?.let { Text(it, color = Palette.sub, style = MaterialTheme.typography.bodyMedium) }
    }
}
