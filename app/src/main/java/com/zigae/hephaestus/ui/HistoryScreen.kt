package com.zigae.hephaestus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zigae.hephaestus.AppGraph
import com.zigae.hephaestus.data.AppData
import com.zigae.hephaestus.domain.MaintenanceRecord

/** 정비 이력: 같은 날·같은 장소의 기록을 한 행으로 묶고, 주행거리가 큰(최근) 순. 항목을 누르면 확인 뒤 삭제. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(graph: AppGraph, data: AppData) {
    val groups = data.records
        .sortedWith(compareByDescending<MaintenanceRecord> { it.odometerKm }.thenByDescending { it.timeMillis })
        .groupBy { Triple(Format.date(it.timeMillis), it.odometerKm, it.shopName) }
        .toList()
    var deleting by remember { mutableStateOf<MaintenanceRecord?>(null) }

    Column {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 16.dp)) {
            Text("정비 이력", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("${data.records.size}건", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
        }
        Panel(padding = 0.dp) {
            if (groups.isEmpty()) {
                Column(Modifier.padding(28.dp)) {
                    Text("기록 없음", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("정비소에서 출발할 때 홈에서 묻고, 소모품 탭에서 직접 입력할 수도 있어요.", color = Palette.sub, style = MaterialTheme.typography.bodyLarge)
                }
            }
            LazyColumn {
                items(groups, key = { it.second.first().id }) { (key, recs) ->
                    val (date, km, shop) = key
                    Column(Modifier.padding(horizontal = 28.dp, vertical = 22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(shop?.let { if (it == com.zigae.hephaestus.domain.VisitTracker.SIGNAL_SHOP_NAME) "차량 신호로 확인" else it } ?: "직접 입력", style = MaterialTheme.typography.titleMedium)
                                Text(date, style = MaterialTheme.typography.bodyMedium, color = Palette.sub)
                            }
                            Text(Format.km(km), style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(14.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            recs.forEach { r ->
                                Text(
                                    r.item.label, style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.clip(ButtonShape).background(Palette.cardHigh).clickable { deleting = r }
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                )
                            }
                        }
                    }
                    if (recs.first().id != groups.last().second.first().id) Rule(Modifier.padding(horizontal = 28.dp))
                }
            }
        }
    }

    deleting?.let { r ->
        Sheet(onDismiss = { deleting = null }, width = 760.dp) {
            Text("기록 삭제", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            Text("${r.item.label} · ${Format.date(r.timeMillis)} · ${Format.km(r.odometerKm)}", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SecondaryButton("취소", Modifier.weight(1f)) { deleting = null }
                PrimaryButton("지우기", Modifier.weight(1f)) { graph.repo.deleteRecord(r.id); deleting = null }
            }
        }
    }
}
