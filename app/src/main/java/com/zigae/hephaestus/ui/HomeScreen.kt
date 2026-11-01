package com.zigae.hephaestus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

fun levelColor(level: DueLevel): Color = when (level) {
    DueLevel.OVERDUE -> Palette.overdue
    DueLevel.SOON -> Palette.soon
    DueLevel.OK -> Palette.ok
    DueLevel.NO_RECORD -> Palette.none
}

@Composable
fun HomeScreen(graph: AppGraph, data: AppData, mode: Powertrain) {
    val vehicle by graph.vehicle.state.collectAsStateWithLifecycle()
    val navi by graph.navi.state.collectAsStateWithLifecycle()
    val status by graph.detector.status.collectAsStateWithLifecycle()
    val odo = (vehicle.odometerKm ?: 0f).toInt() + data.demoOdometerOffsetKm
    val due = Reminders.all(mode, { data.interval(it, mode)!! }, data.records, odo, System.currentTimeMillis())
    var editing by remember { mutableStateOf<DueStatus?>(null) }

    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        LazyColumn(Modifier.weight(0.46f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Caption("${mode.label} · 총 주행거리", modifier = Modifier.weight(1f))
                        GearIndicator(vehicle.gear)
                    }
                    BigNumber(NumberFormat.getIntegerInstance(Locale.KOREA).format(odo), "km")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = Palette.sub, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            listOfNotNull(
                                navi.address,
                                when (vehicle.gear) {
                                    Gear.PARK -> status.parkedSince?.let { "${Format.time(it)}부터 정차" } ?: "정차 중"
                                    Gear.UNKNOWN -> "차량 연결 중…"
                                    else -> "주행 중"
                                },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodyLarge, color = Palette.sub,
                        )
                    }
                    if (vehicle.gear == Gear.PARK) {
                        Spacer(Modifier.height(16.dp))
                        when {
                            status.shop != null -> Pill("정비소 감지 · ${status.shop!!.name}", icon = Icons.Filled.Build)
                            status.resolving -> Pill("정비소인지 확인 중…", color = Palette.sub)
                            status.ignoredPlace -> Pill("정비소 아님으로 등록한 곳", color = Palette.sub)
                            else -> Pill("정비소 아님", color = Palette.sub)
                        }
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
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatTile(due.count { it.level == DueLevel.OVERDUE }.toString(), "교체 지남", Palette.overdue, Modifier.weight(1f))
                    StatTile(due.count { it.level == DueLevel.SOON }.toString(), "곧 교체", Palette.soon, Modifier.weight(1f))
                    StatTile(data.records.size.toString(), "정비 기록", Palette.text, Modifier.weight(1f))
                }
            }
        }
        Column(Modifier.weight(0.54f).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("교체 시기", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Caption("급한 순")
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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

@Composable
private fun Caption(text: String, modifier: Modifier) {
    Box(modifier) { Caption(text) }
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
    Panel(glow = true) {
        Caption("정비 기록 · ${Format.time(p.createdAt)}", color = Palette.accent)
        Spacer(Modifier.height(6.dp))
        Text("${p.shopName}에서 출발하시네요", style = MaterialTheme.typography.titleLarge)
        Text(
            "어떤 정비를 받으셨나요?  ${Format.km(p.odometerKm)} 자동 기록",
            color = Palette.sub, style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(20.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MaintenanceItem.forMode(mode).forEach { item ->
                val on = item in selected
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(if (on) Palette.accent else Palette.card)
                        .border(1.5.dp, if (on) Palette.accent else Palette.outline, CircleShape)
                        .clickable { selected = if (on) selected - item else selected + item }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (on) {
                        Icon(Icons.Filled.Check, null, tint = Palette.onAccent, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(item.label, style = MaterialTheme.typography.bodyLarge, color = if (on) Palette.onAccent else Palette.text)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { onSave(selected) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.height(72.dp),
                colors = ButtonDefaults.buttonColors(disabledContainerColor = Palette.card, disabledContentColor = Palette.sub),
            ) {
                Text(if (selected.isEmpty()) "항목을 고르세요" else "${selected.size}개 기록", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp))
            }
            OutlinedButton(onClick = onSkip, modifier = Modifier.height(72.dp)) {
                Text("정비 안 받음", style = MaterialTheme.typography.labelLarge, color = Palette.text)
            }
            if (p.source != ShopSource.MY_SHOP && p.source != ShopSource.DEMO) {
                TextButton(onClick = onNotShop, modifier = Modifier.height(72.dp)) {
                    Text("정비소 아님", style = MaterialTheme.typography.labelLarge, color = Palette.sub)
                }
            }
        }
    }
}

/** Android Auto 목록 행 + 칸 게이지. */
@Composable
private fun DueRow(s: DueStatus, onClick: () -> Unit) {
    val color = levelColor(s.level)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.card)
            .border(1.dp, if (s.level == DueLevel.OVERDUE) Palette.overdue.copy(alpha = 0.6f) else Palette.outline, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.item.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    if (s.level == DueLevel.NO_RECORD) "기록 없음" else Format.remaining(s),
                    color = color, style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (s.progress != null) {
                SegmentBar(s.progress!!, color)
                Spacer(Modifier.height(10.dp))
            }
            Text(
                s.last?.let { "마지막 ${Format.date(it.timeMillis)} · ${Format.km(it.odometerKm)}" + (it.shopName?.let { n -> " · $n" } ?: "") }
                    ?: "주기 ${Format.interval(s.interval)} · 눌러서 마지막 교체 입력",
                color = Palette.sub, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.none, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun RecordDialog(s: DueStatus, currentKm: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf(currentKm.toString()) }
    val km = text.filter { it.isDigit() }.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.cardHigh,
        title = { Text(if (s.last == null) "${s.item.label} 마지막 교체" else "${s.item.label} 교체 기록") },
        text = {
            Column {
                Text("교체한 시점의 주행거리 (현재 ${Format.km(currentKm)})", color = Palette.sub)
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
        confirmButton = { Button(onClick = { km?.let(onSave) }, enabled = km != null) { Text("기록") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = Palette.sub) } },
    )
}

/** 설정·기록 화면에서 쓰는 패널 (예전 HCard 호환). */
@Composable
fun HCard(highlight: Boolean = false, content: @Composable () -> Unit) = Panel(glow = highlight, content = content)
