package com.zigae.hephaestus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigae.hephaestus.AppGraph
import com.zigae.hephaestus.data.AppData
import com.zigae.hephaestus.domain.MaintenanceItem
import com.zigae.hephaestus.domain.Place
import com.zigae.hephaestus.domain.Powertrain
import com.zigae.hephaestus.domain.VehicleSignal

/** 설정: 차종 · 내 정비소 · 감지. 교체 주기는 소모품 탭의 항목 시트에서 바꾼다. 개발용 도구는 맨 아래 접어 둔다. */
@Composable
fun SettingsScreen(graph: AppGraph, data: AppData, mode: Powertrain) {
    val vehicle by graph.vehicle.state.collectAsStateWithLifecycle()
    val navi by graph.navi.state.collectAsStateWithLifecycle()
    val status by graph.detector.status.collectAsStateWithLifecycle()
    val signals by graph.detector.signals.collectAsStateWithLifecycle()
    var naming by remember { mutableStateOf(false) }
    var showDev by rememberSaveable { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Panel {
                SectionTitle("차종", data.detectedMode?.let { "차량 정보로 추정: ${it.label}" } ?: "차량 정보로 추정하지 못했어요")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Powertrain.entries.forEach { p -> SelectChip(p.label, p == mode) { graph.repo.setMode(p) } }
                }
                Spacer(Modifier.height(10.dp))
                Text("차종에 따라 관리 항목과 기본 교체 주기가 달라져요.", style = MaterialTheme.typography.bodyMedium, color = Palette.sub)
            }
        }
        item {
            Panel {
                SectionTitle("내 정비소", "등록한 곳에 정차하면 바로 정비소로 알아봐요. 기록을 남긴 정비소는 자동으로 등록돼요.")
                val mine = data.places.filter { it.kind == Place.Kind.MY_SHOP }
                mine.forEach { PlaceRow(it, Icons.Filled.Build) { graph.repo.deletePlace(it.id) } }
                if (mine.isNotEmpty()) Spacer(Modifier.height(8.dp))
                SecondaryButton(if (navi.location != null) "현재 위치를 내 정비소로 등록" else "현재 위치 확인 중…") { if (navi.location != null) naming = true }
                val ignored = data.places.filter { it.kind == Place.Kind.IGNORED }
                if (ignored.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    SectionTitle("정비소 아닌 곳", "이 장소에서는 묻지 않아요")
                    ignored.forEach { PlaceRow(it, Icons.Filled.LocationOn) { graph.repo.deletePlace(it.id) } }
                }
            }
        }
        item {
            Panel {
                SectionTitle("정비소에 머문 시간", "이 시간보다 짧게 들른 경우에는 묻지 않아요")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(0, 5, 20, 30).forEach { m ->
                        SelectChip(if (m == 0) "바로" else "${m}분 이상", data.minDwellMinutes == m) { graph.repo.update { it.copy(minDwellMinutes = m) } }
                    }
                }
            }
        }
        item {
            Panel {
                ListRow(null, "개발용 · 데모와 진단", if (showDev) "접기" else "펼치기") { showDev = !showDev }
                if (showDev) {
                    HorizontalDivider(color = Palette.outline)
                    Spacer(Modifier.height(16.dp))
                    SectionTitle("데모", "에뮬레이터는 주행거리가 0에서 움직이지 않아 보정값을 더해요")
                    Text("주행거리 보정 +${Format.km(data.demoOdometerOffsetKm)}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(100, 1_000, 5_000, 50_000).forEach { add ->
                            StepButton("+${Format.km(add)}") {
                                graph.repo.update { it.copy(demoOdometerOffsetKm = it.demoOdometerOffsetKm + add) }
                                graph.detector.checkRemindersIfParked()
                            }
                        }
                        StepButton("초기화") { graph.repo.update { it.copy(demoOdometerOffsetKm = 0) } }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryButton("현재 위치 카센터를 목적지로", Modifier.weight(1f)) {
                            val at = navi.location
                            message = if (at != null && graph.navi.requestDestination(DEMO_SHOP, at, navi.address)) {
                                "내비 목적지를 \"$DEMO_SHOP\"(현재 위치)로 설정했어요. 기어 P → 출차하면 질문이 나와요."
                            } else "목적지 설정 실패 (내비 연결·위치 확인)"
                        }
                        SecondaryButton("방문 질문 바로 만들기", Modifier.weight(1f)) {
                            graph.detector.simulateVisit(DEMO_SHOP)
                            message = "방문 질문을 만들었어요. 홈과 알림 센터를 확인하세요."
                        }
                    }
                    message?.let { Text(it, color = Palette.info.base, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp)) }
                    Spacer(Modifier.height(24.dp))
                    SectionTitle("차량 신호 흉내", "켜면 소모품 목록에 차량 경고로 뜨고, 끄면 정비된 것으로 보고 기록을 물어요. 12V 경고등은 Virtual Console로도 켤 수 있어요.")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VehicleSignal.entries.filter { it.item in MaintenanceItem.forMode(mode) }.forEach { s ->
                            val on = s in signals
                            SelectChip(s.label, on) { graph.detector.simulateSignal(s, !on) }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    SectionTitle("진단")
                    Diag("신호 지원", vehicle.support.entries.joinToString(" · ") { "${it.key}=${if (it.value) "O" else "X"}" }.ifBlank { "-" })
                    Diag("켜진 경고등", vehicle.warnings.joinToString(", ").ifBlank { "없음" })
                    Diag("소모품 신호", signals.joinToString(", ") { it.label }.ifBlank { "없음" })
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
    }

    if (naming) {
        val at = navi.location
        val suggestions = listOfNotNull(navi.address?.let { "$it 정비소" }, navi.destination?.name, "단골 카센터").distinct()
        Sheet(onDismiss = { naming = false }, width = 860.dp) {
            Text("내 정비소로 등록", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("이름을 고르세요. 현재 위치(${navi.address ?: "확인 중"})를 등록해요.", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
            Spacer(Modifier.height(20.dp))
            suggestions.forEach { name ->
                ListRow(Icons.Filled.Build, name) {
                    if (at != null) graph.repo.addPlace(name, at, Place.Kind.MY_SHOP)
                    naming = false
                }
                HorizontalDivider(color = Palette.outline)
            }
            Spacer(Modifier.height(20.dp))
            SecondaryButton("취소", Modifier.fillMaxWidth()) { naming = false }
        }
    }
}

private const val DEMO_SHOP = "시흥 오토 카센터"

@Composable
private fun PlaceRow(p: Place, icon: ImageVector, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Icon(icon, null, tint = Palette.sub, modifier = Modifier.width(28.dp))
        Spacer(Modifier.width(14.dp))
        Text(p.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, "삭제", tint = Palette.sub) }
    }
}

@Composable
private fun Diag(k: String, v: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(k, color = Palette.sub, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(200.dp))
        Text(v, style = MaterialTheme.typography.bodyMedium)
    }
}
