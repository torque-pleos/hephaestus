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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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

/** 정비 이력: 같은 날·같은 장소의 기록을 한 카드로 묶고, 주행거리가 큰(최근) 순. 삭제는 확인 뒤에. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(graph: AppGraph, data: AppData) {
    val groups = data.records
        .sortedWith(compareByDescending<MaintenanceRecord> { it.odometerKm }.thenByDescending { it.timeMillis })
        .groupBy { Triple(Format.date(it.timeMillis), it.odometerKm, it.shopName) }
        .toList()
    var deleting by remember { mutableStateOf<MaintenanceRecord?>(null) }

    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("정비 이력", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("총 ${data.records.size}건", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
        }
        Spacer(Modifier.height(20.dp))
        if (groups.isEmpty()) {
            Panel {
                Text("아직 정비 기록이 없어요", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("정비소에 다녀오면 홈 화면에서 먼저 물어볼게요. 소모품 탭에서 직접 입력할 수도 있어요.", color = Palette.sub, style = MaterialTheme.typography.bodyLarge)
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(groups, key = { it.second.first().id }) { (key, recs) ->
                val (date, km, shop) = key
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(shop ?: "직접 입력", style = MaterialTheme.typography.titleMedium)
                            Text(date, style = MaterialTheme.typography.bodyMedium, color = Palette.sub)
                        }
                        Text(Format.km(km), style = MaterialTheme.typography.titleLarge, color = Palette.primary)
                    }
                    Spacer(Modifier.height(16.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        recs.forEach { r ->
                            Row(
                                Modifier.clip(ButtonShape).background(Palette.cardHigh).clickable { deleting = r }
                                    .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(r.item.label, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.width(10.dp))
                                Icon(Icons.Filled.Close, "삭제", tint = Palette.sub, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    deleting?.let { r ->
        Sheet(onDismiss = { deleting = null }, width = 760.dp) {
            Text("이 기록을 지울까요?", style = MaterialTheme.typography.headlineSmall)
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
