package com.zigae.hephaestus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigae.hephaestus.AppGraph
import com.zigae.hephaestus.domain.Powertrain

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("홈", Icons.Filled.Home),
    ITEMS("소모품", Icons.Filled.Build),
    HISTORY("이력", Icons.AutoMirrored.Filled.List),
    SETTINGS("설정", Icons.Filled.Settings),
}

@Composable
fun HephApp(graph: AppGraph) {
    HephTheme {
        val data by graph.repo.data.collectAsStateWithLifecycle()
        Surface(Modifier.fillMaxSize(), color = Palette.bg, contentColor = Palette.text) {
            val mode = data.mode
            if (mode == null) {
                ModeSetup(detected = data.detectedMode, onPick = graph.repo::setMode)
            } else {
                var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
                Row(Modifier.fillMaxSize()) {
                    Rail(tab, badge = data.prompts.size) { tab = it }
                    Box(Modifier.weight(1f).fillMaxHeight().padding(28.dp)) {
                        when (tab) {
                            Tab.HOME -> HomeScreen(graph, data, mode) { tab = Tab.ITEMS }
                            Tab.ITEMS -> ItemsScreen(graph, data, mode)
                            Tab.HISTORY -> HistoryScreen(graph, data)
                            Tab.SETTINGS -> SettingsScreen(graph, data, mode)
                        }
                    }
                }
            }
        }
    }
}

/** 세로 탭. 선택 탭은 강조색 + 왼쪽 막대. 홈에 답하지 않은 질문 수를 점으로 표시. */
@Composable
private fun Rail(selected: Tab, badge: Int, onSelect: (Tab) -> Unit) {
    Column(
        Modifier.fillMaxHeight().width(150.dp).background(Palette.rail).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Tab.entries.forEach { t ->
            val on = t == selected
            Row(Modifier.fillMaxWidth().height(118.dp).clickable { onSelect(t) }, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(6.dp).height(64.dp).background(if (on) Palette.primary else Palette.rail))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        Icon(t.icon, null, tint = if (on) Palette.primary else Palette.none, modifier = Modifier.size(44.dp))
                        if (t == Tab.HOME && badge > 0) {
                            Box(
                                Modifier.align(Alignment.TopEnd).size(18.dp).clip(RoundedCornerShape(9.dp)).background(Palette.danger.base),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(t.label, style = MaterialTheme.typography.bodyMedium, color = if (on) Palette.primary else Palette.sub)
                }
            }
        }
    }
}

/** 첫 실행: 차종 선택. 차량에서 추정한 값에 "추천" 표시. */
@Composable
private fun ModeSetup(detected: Powertrain?, onPick: (Powertrain) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("차종 선택", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text("정비소에서 출발할 때 묻고, 그때 주행거리를 붙여 기록합니다", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
        Spacer(Modifier.height(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Powertrain.entries.forEach { p ->
                val suggested = p == detected
                Column(
                    Modifier
                        .size(width = 480.dp, height = 230.dp)
                        .clip(PanelShape)
                        .background(Palette.card)
                        .border(if (suggested) 2.dp else 0.dp, if (suggested) Palette.primary else Palette.card, PanelShape)
                        .clickable { onPick(p) }
                        .padding(32.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(p.label, style = MaterialTheme.typography.headlineMedium)
                        if (suggested) Text("차량 정보로 추천", style = MaterialTheme.typography.bodyLarge, color = Palette.primary)
                    }
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            if (p == Powertrain.ICE) "엔진오일 · 변속기 오일 · 점화플러그" else "감속기 오일 · 배터리 냉각수 · 타이어",
                            style = MaterialTheme.typography.bodyLarge, color = Palette.sub,
                        )
                        Spacer(Modifier.height(12.dp))
                        Icon(Icons.Filled.Build, null, tint = Palette.primary, modifier = Modifier.size(36.dp).align(Alignment.End))
                    }
                }
            }
        }
    }
}
