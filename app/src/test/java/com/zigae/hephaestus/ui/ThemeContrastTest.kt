package com.zigae.hephaestus.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/** Pleos UI 가이드: 아이콘·텍스트 대비율 4.5:1 이상. 주간·야간 토큰의 글자/배경 조합을 모두 확인한다. */
class ThemeContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return max(la, lb).toDouble() / min(la, lb)
    }

    private fun pairs(c: AppColors): Map<String, Pair<Color, Color>> = buildMap {
        put("text/bg", c.text to c.bg)
        put("text/card", c.text to c.card)
        put("sub/card", c.sub to c.card)
        put("sub/bg", c.sub to c.bg)
        put("text/cardHigh", c.text to c.cardHigh)
        listOf("primary" to c.primary, "info" to c.info, "success" to c.success, "warning" to c.warning, "danger" to c.danger).forEach { (n, t) ->
            put("$n.on/base", t.on to t.base)
            put("$n.onContainer/container", t.onContainer to t.container)
        }
        put("danger.base/card", c.danger.base to c.card) // 지난 거리 숫자
        put("warning.onContainer/card", c.warning.onContainer to c.card) // 곧 교체 숫자
        put("primary/card", c.primary.base to c.card) // 기록 화면 주행거리
    }

    @Test
    fun dayAndNightMeetContrast() {
        val failures = listOf("day" to DayColors, "night" to NightColors).flatMap { (theme, c) ->
            pairs(c).mapNotNull { (name, p) ->
                val r = ratio(p.first, p.second)
                if (r < 4.5) "$theme $name = ${"%.2f".format(r)}" else null
            }
        }
        assertTrue("대비 부족: $failures", failures.isEmpty())
    }
}

class NightHeaderContrastTest {
    @Test
    fun nightHeaderUsesReadableContainer() {
        val c = NightColors
        val la = c.primary.onContainer.luminance() + 0.05
        val lb = c.primary.container.luminance() + 0.05
        assertTrue(maxOf(la, lb) / minOf(la, lb) >= 4.5f)
    }
}
