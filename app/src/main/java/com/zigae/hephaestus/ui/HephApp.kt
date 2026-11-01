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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
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
    HISTORY("기록", Icons.AutoMirrored.Filled.List),
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
                    Rail(tab) { tab = it }
                    Box(Modifier.weight(1f).fillMaxHeight().padding(28.dp)) {
                        when (tab) {
                            Tab.HOME -> HomeScreen(graph, data, mode)
                            Tab.HISTORY -> HistoryScreen(graph, data)
                            Tab.SETTINGS -> SettingsScreen(graph, data, mode)
                        }
                    }
                }
            }
        }
    }
}

/** Android Auto 스타일 왼쪽 레일: 원형 아이콘, 선택 항목만 라임. */
@Composable
private fun Rail(selected: Tab, onSelect: (Tab) -> Unit) {
    Column(
        Modifier.fillMaxHeight().width(132.dp).background(Palette.rail).padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Icon(Icons.Filled.Build, null, tint = Palette.accent, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Tab.entries.forEach { t ->
            Column(
                Modifier.clip(RoundedCornerShape(20.dp)).clickable { onSelect(t) }.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconCircle(t.icon, selected = t == selected)
                Spacer(Modifier.height(6.dp))
                Text(t.label, style = MaterialTheme.typography.bodyMedium, color = if (t == selected) Palette.text else Palette.sub)
            }
        }
    }
}

/** 첫 실행: 차종 선택. 차량에서 추정한 값을 라임 테두리로 먼저 제안한다. */
@Composable
private fun ModeSetup(detected: Powertrain?, onPick: (Powertrain) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Build, null, tint = Palette.accent, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text("HEPHAESTUS", style = MaterialTheme.typography.headlineMedium, color = Palette.text)
        Spacer(Modifier.height(8.dp))
        Text("정비소에 다녀오면 먼저 묻고, 주행거리를 붙여 기록합니다", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
        Spacer(Modifier.height(56.dp))
        Caption(detected?.let { "차량 정보로 추정 · ${it.label}" } ?: "차량 정보 확인 중…", color = Palette.accent)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Powertrain.entries.forEach { p ->
                val suggested = p == detected
                Column(
                    Modifier
                        .size(width = 480.dp, height = 220.dp)
                        .clip(PanelShape)
                        .background(if (suggested) Palette.cardHigh else Palette.card)
                        .border(if (suggested) 2.dp else 1.dp, if (suggested) Palette.accent else Palette.outline, PanelShape)
                        .clickable { onPick(p) }
                        .padding(32.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (suggested) Pill("추천", filled = true) else Spacer(Modifier.height(44.dp))
                    Column {
                        Text(p.label, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            if (p == Powertrain.ICE) "엔진오일 · 변속기 오일 · 점화플러그" else "감속기 오일 · 배터리 냉각수 · 타이어",
                            style = MaterialTheme.typography.bodyMedium, color = Palette.sub,
                        )
                    }
                }
            }
        }
    }
}
