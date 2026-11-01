package com.zigae.hephaestus.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigae.hephaestus.AppGraph
import com.zigae.hephaestus.domain.Powertrain

private enum class Tab(val label: String, val glyph: String) {
    HOME("홈", "⌂"),
    HISTORY("기록", "☰"),
    SETTINGS("설정", "⚙"),
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
                    NavigationRail(containerColor = Palette.card, modifier = Modifier.fillMaxHeight().width(120.dp)) {
                        Spacer(Modifier.height(16.dp))
                        Tab.entries.forEach { t ->
                            NavigationRailItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = { Text(t.glyph, style = MaterialTheme.typography.titleLarge) },
                                label = { Text(t.label, style = MaterialTheme.typography.bodyMedium) },
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight().padding(24.dp)) {
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

/** 첫 실행: 차종 선택. 차량에서 추정한 값을 먼저 제안한다. */
@Composable
private fun ModeSetup(detected: Powertrain?, onPick: (Powertrain) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Hephaestus", style = MaterialTheme.typography.headlineMedium, color = Palette.accent)
        Spacer(Modifier.height(12.dp))
        Text("정비소에 다녀오면 먼저 묻고, 주행거리를 붙여 기록합니다", style = MaterialTheme.typography.bodyLarge, color = Palette.sub)
        Spacer(Modifier.height(48.dp))
        Text("차종을 선택하세요", style = MaterialTheme.typography.titleLarge)
        Text(
            detected?.let { "차량 정보로 추정: ${it.label}" } ?: "차량 정보 확인 중…",
            style = MaterialTheme.typography.bodyMedium, color = Palette.sub,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Powertrain.entries.forEach { p ->
                val suggested = p == detected
                Button(
                    onClick = { onPick(p) },
                    modifier = Modifier.size(width = 280.dp, height = 120.dp),
                    colors = if (suggested) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(containerColor = Palette.cardHigh, contentColor = Palette.text),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(p.label, style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (p == Powertrain.ICE) "엔진오일·미션오일 등" else "감속기 오일·배터리 냉각수 등",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
