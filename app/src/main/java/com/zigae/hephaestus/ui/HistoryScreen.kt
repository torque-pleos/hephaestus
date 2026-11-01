package com.zigae.hephaestus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zigae.hephaestus.AppGraph
import com.zigae.hephaestus.data.AppData

/** 정비 이력: 같은 날·같은 장소의 기록을 한 카드로 묶는다. */
@Composable
fun HistoryScreen(graph: AppGraph, data: AppData) {
    val groups = data.records
        .sortedByDescending { it.timeMillis }
        .groupBy { Triple(Format.date(it.timeMillis), it.odometerKm, it.shopName) }
        .toList()

    Column {
        SectionTitle("정비 이력", "${data.records.size}건 · 기록마다 그때 주행거리가 함께 저장됩니다")
        Spacer(Modifier.height(12.dp))
        if (groups.isEmpty()) {
            Text("아직 기록이 없습니다. 정비소에 다녀오면 홈 화면에서 물어봅니다.", color = Palette.sub, style = MaterialTheme.typography.bodyLarge)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(groups, key = { it.second.first().id }) { (key, recs) ->
                val (date, km, shop) = key
                Card(colors = CardDefaults.cardColors(containerColor = Palette.card), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("$date · ${Format.km(km)}" + (shop?.let { " · $it" } ?: ""), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        recs.forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("· ${r.item.label}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                TextButton(onClick = { graph.repo.deleteRecord(r.id) }) { Text("삭제", color = Palette.sub) }
                            }
                        }
                    }
                }
            }
        }
    }
}
