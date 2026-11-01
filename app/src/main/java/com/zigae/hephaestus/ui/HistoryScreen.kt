package com.zigae.hephaestus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zigae.hephaestus.AppGraph
import com.zigae.hephaestus.data.AppData
import java.text.NumberFormat
import java.util.Locale

/** 정비 이력: 같은 날·같은 장소의 기록을 한 카드로 묶은 타임라인. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(graph: AppGraph, data: AppData) {
    val groups = data.records
        .sortedWith(compareByDescending<com.zigae.hephaestus.domain.MaintenanceRecord> { it.odometerKm }.thenByDescending { it.timeMillis })
        .groupBy { Triple(Format.date(it.timeMillis), it.odometerKm, it.shopName) }
        .toList()

    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("정비 이력", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Caption("${data.records.size}건 · 기록마다 그때 주행거리 저장")
        }
        Spacer(Modifier.height(20.dp))
        if (groups.isEmpty()) {
            Panel {
                Text("아직 기록이 없습니다", style = MaterialTheme.typography.titleMedium)
                Text("정비소에 다녀오면 홈 화면에서 먼저 물어봅니다.", color = Palette.sub, style = MaterialTheme.typography.bodyLarge)
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            items(groups, key = { it.second.first().id }) { (key, recs) ->
                val (date, km, shop) = key
                Row(Modifier.height(IntrinsicSize.Min)) {
                    // 타임라인 선과 점
                    Column(Modifier.width(48.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(36.dp))
                        Box(Modifier.size(18.dp).clip(CircleShape).background(Palette.accent))
                        Box(Modifier.width(2.dp).weight(1f).background(Palette.outline))
                    }
                    Box(Modifier.weight(1f).padding(bottom = 20.dp)) {
                        Panel {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Caption(date + (shop?.let { " · $it" } ?: ""))
                                    BigNumber(NumberFormat.getIntegerInstance(Locale.KOREA).format(km), "km", small = true)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                recs.forEach { r ->
                                    Row(
                                        Modifier.clip(CircleShape).border(1.5.dp, Palette.outline, CircleShape).padding(start = 22.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(r.item.label, style = MaterialTheme.typography.bodyLarge)
                                        IconButton(onClick = { graph.repo.deleteRecord(r.id) }) {
                                            Icon(Icons.Filled.Close, "삭제", tint = Palette.sub)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
