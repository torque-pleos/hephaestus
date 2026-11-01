package com.zigae.hephaestus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigae.hephaestus.AppGraph
import com.zigae.hephaestus.data.AppData
import com.zigae.hephaestus.domain.Interval
import com.zigae.hephaestus.domain.MaintenanceItem
import com.zigae.hephaestus.domain.Place
import com.zigae.hephaestus.domain.Powertrain

@Composable
fun SettingsScreen(graph: AppGraph, data: AppData, mode: Powertrain) {
    val vehicle by graph.vehicle.state.collectAsStateWithLifecycle()
    val navi by graph.navi.state.collectAsStateWithLifecycle()
    val status by graph.detector.status.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<MaintenanceItem?>(null) }
    var naming by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            HCard {
                SectionTitle("차종", data.detectedMode?.let { "차량 정보로 추정: ${it.label}" } ?: "차량 정보로 추정하지 못함")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Powertrain.entries.forEach { p ->
                        SelectChip(p.label, p == mode) { graph.repo.setMode(p) }
                    }
                }
            }
        }
        item {
            HCard {
                SectionTitle("내 정비소", "등록한 곳에 정차하면 바로 정비소로 알아봅니다. 기록을 남긴 정비소는 자동 등록됩니다.")
                data.places.filter { it.kind == Place.Kind.MY_SHOP }.forEach { PlaceRow(it) { graph.repo.deletePlace(it.id) } }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(shape = ButtonShape, onClick = { naming = true }, enabled = navi.location != null, modifier = Modifier.height(60.dp)) {
                    Text(if (navi.location != null) "현재 위치를 내 정비소로 등록" else "현재 위치 확인 중…")
                }
                val ignored = data.places.filter { it.kind == Place.Kind.IGNORED }
                if (ignored.isNotEmpty()) {
                    SectionTitle("정비소 아님", "이 장소들에서는 묻지 않습니다")
                    ignored.forEach { PlaceRow(it) { graph.repo.deletePlace(it.id) } }
                }
            }
        }
        item {
            HCard {
                SectionTitle("교체 주기", "항목을 눌러 바꿉니다")
                MaintenanceItem.forMode(mode).forEach { item ->
                    val iv = data.interval(item, mode)!!
                    ListRow(
                        null, item.label,
                        Format.interval(iv) + if (item in data.intervalOverrides) " (변경)" else "",
                        valueColor = if (item in data.intervalOverrides) Palette.info.base else Palette.sub,
                    ) { editing = item }
                }
            }
        }
        item {
            HCard {
                SectionTitle("감지", "정비소에 이 시간 이상 머물러야 묻습니다 (잠깐 들른 경우 제외)")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(0, 5, 20, 30).forEach { m ->
                        SelectChip(if (m == 0) "바로" else "${m}분", data.minDwellMinutes == m) {
                            graph.repo.update { it.copy(minDwellMinutes = m) }
                        }
                    }
                }
            }
        }
        item {
            HCard {
                SectionTitle("데모", "에뮬레이터는 주행거리가 0에서 움직이지 않아 보정값을 더합니다")
                Text("주행거리 보정 +${Format.km(data.demoOdometerOffsetKm)}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(100, 1_000, 5_000, 50_000).forEach { add ->
                        OutlinedButton(shape = ButtonShape, onClick = {
                            graph.repo.update { it.copy(demoOdometerOffsetKm = it.demoOdometerOffsetKm + add) }
                            graph.detector.checkRemindersIfParked()
                        }, modifier = Modifier.height(56.dp)) { Text("+${Format.km(add)}") }
                    }
                    TextButton(onClick = { graph.repo.update { it.copy(demoOdometerOffsetKm = 0) } }, modifier = Modifier.height(56.dp)) { Text("초기화") }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(shape = ButtonShape, onClick = {
                        val at = navi.location
                        message = if (at != null && graph.navi.requestDestination(DEMO_SHOP, at, navi.address)) {
                            "내비 목적지를 \"$DEMO_SHOP\"(현재 위치)로 설정했습니다. 기어 P → 출차하면 질문이 나옵니다."
                        } else "목적지 설정 실패 (내비 연결·위치 확인)"
                    }, modifier = Modifier.height(60.dp)) { Text("현재 위치 카센터를 목적지로") }
                    OutlinedButton(shape = ButtonShape, onClick = {
                        graph.detector.simulateVisit(DEMO_SHOP)
                        message = "방문 질문을 만들었습니다. 홈과 알림 센터를 확인하세요."
                    }, modifier = Modifier.height(60.dp)) { Text("방문 질문 바로 만들기") }
                }
                message?.let { Text(it, color = Palette.primary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        item {
            HCard {
                SectionTitle("진단")
                Diag("기어", vehicle.gear.name)
                Diag("차량 주행거리", vehicle.odometerKm?.let { "$it km" } ?: "-")
                Diag("충전구", vehicle.chargePortOpen?.let { if (it) "열림" else "닫힘" } ?: "-")
                Diag("엔진오일", vehicle.engineOil?.name ?: "-")
                Diag("내비 연결", if (navi.connected) "됨" else "안 됨")
                Diag("현재 위치", navi.location?.let { "%.5f, %.5f".format(it.lat, it.lon) } ?: "-")
                Diag("목적지", navi.destination?.name ?: "-")
                Diag("정차 세션", status.parkedSince?.let { Format.time(it) } ?: "-")
                Diag("정비소 판별", status.shop?.let { "${it.name} (${it.source.label})" } ?: if (status.resolving) "확인 중" else "-")
                Diag("최근 오류", listOfNotNull(vehicle.lastError, navi.lastError).joinToString(" / ").ifBlank { "없음" })
            }
        }
    }

    editing?.let { item ->
        IntervalDialog(item, data.interval(item, mode)!!, item.defaultInterval(mode)!!, onDismiss = { editing = null }) { iv ->
            graph.repo.update { d ->
                d.copy(intervalOverrides = if (iv == item.defaultInterval(mode)) d.intervalOverrides - item else d.intervalOverrides + (item to iv))
            }
            editing = null
        }
    }
    if (naming) {
        val at = navi.location
        NameDialog(initial = navi.address?.let { "$it 정비소" } ?: "내 정비소", onDismiss = { naming = false }) { name ->
            if (at != null) graph.repo.addPlace(name, at, Place.Kind.MY_SHOP)
            naming = false
        }
    }
}

private const val DEMO_SHOP = "시흥 오토 카센터"

@Composable
private fun PlaceRow(p: Place, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(p.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onDelete) { Text("삭제", color = Palette.sub) }
    }
}

@Composable
private fun Diag(k: String, v: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(k, color = Palette.sub, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(200.dp))
        Text(v, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun IntervalDialog(item: MaintenanceItem, current: Interval, default: Interval, onDismiss: () -> Unit, onSave: (Interval) -> Unit) {
    var km by remember { mutableStateOf(current.km.toString()) }
    var months by remember { mutableStateOf(current.months.toString()) }
    val parsed = runCatching { Interval(km.toIntOrNull() ?: 0, months.toIntOrNull() ?: 0) }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${item.label} 교체 주기") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("기본값 ${Format.interval(default)} · 0은 사용 안 함", color = Palette.sub)
                OutlinedTextField(km, { km = it.filter(Char::isDigit).take(6) }, suffix = { Text("km") }, singleLine = true, textStyle = MaterialTheme.typography.titleMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(months, { months = it.filter(Char::isDigit).take(3) }, suffix = { Text("개월") }, singleLine = true, textStyle = MaterialTheme.typography.titleMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { Button(onClick = { parsed?.let(onSave) }, enabled = parsed != null) { Text("저장") } },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave(default) }) { Text("기본값") }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        },
    )
}

@Composable
private fun NameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("내 정비소 등록") },
        text = { OutlinedTextField(name, { name = it.take(30) }, singleLine = true, textStyle = MaterialTheme.typography.titleMedium, label = { Text("이름") }) },
        confirmButton = { Button(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text("등록") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
