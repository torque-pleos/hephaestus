package com.zigae.hephaestus.domain

/**
 * 차량이 직접 알려주는 소모품 신호. Vehicle SDK에는 교체 주기·잔여 수명이 없고
 * 레벨 상태(엔진오일·브레이크액·변속기 오일)와 경고등(12V 배터리)만 있다.
 *
 * - 켜짐: "지금 신경 쓸 항목"에 차량 경고로 올린다 (교체 시기와 별개의 실제 신호).
 * - 꺼짐: 누군가 손을 봤다는 증거 → 해당 항목을 미리 고른 기록 질문을 띄운다.
 *   켜짐 자체는 보충·누유 신호라 기록을 묻지 않는다.
 */
enum class VehicleSignal(val label: String, val item: MaintenanceItem, val clearedTitle: String) {
    ENGINE_OIL_LOW("엔진오일 부족", MaintenanceItem.ENGINE_OIL, "엔진오일 레벨이 정상으로 돌아왔어요"),
    BRAKE_FLUID_LOW("브레이크액 부족", MaintenanceItem.BRAKE_FLUID, "브레이크액 레벨이 정상으로 돌아왔어요"),
    TRANSMISSION_OIL_LOW("변속기 오일 부족", MaintenanceItem.TRANSMISSION_OIL, "변속기 오일 레벨이 정상으로 돌아왔어요"),
    BATTERY_12V_WARNING("12V 배터리 경고등", MaintenanceItem.BATTERY_12V, "12V 배터리 경고등이 꺼졌어요"),
    ;

    companion object {
        fun fromKey(key: String): VehicleSignal? = entries.firstOrNull { it.name == key }
    }
}

/**
 * 신호 켜짐/꺼짐 전이를 추적한다 (안드로이드 의존 없음).
 *
 * 레벨 센서는 경사·온도로 LOW와 NORMAL을 오가므로, 켜진 신호는
 * **시동 ON을 한 번 넘기거나** [minActiveMs] 이상 유지된 뒤 꺼질 때만 "정비됨"으로 인정한다.
 */
class SignalTracker(var minActiveMs: Long = DEFAULT_MIN_ACTIVE_MS) {
    data class Active(val since: Long, val confirmed: Boolean = false)

    private val active = linkedMapOf<VehicleSignal, Active>()

    /** 지금 켜져 있는 신호. */
    val current: Set<VehicleSignal> get() = active.keys.toSet()

    /** 현재 켜진 신호 집합을 넣으면, 정비된 것으로 볼 수 있는 꺼진 신호를 돌려준다. */
    fun onSignals(now: Long, signals: Set<VehicleSignal>): List<VehicleSignal> {
        val cleared = mutableListOf<VehicleSignal>()
        active.keys.toList().forEach { s ->
            if (s !in signals) {
                val a = active.remove(s)!!
                if (a.confirmed || now - a.since >= minActiveMs) cleared += s
            }
        }
        signals.forEach { s -> if (s !in active) active[s] = Active(now) }
        return cleared
    }

    /** 시동 ON: 지금 켜져 있는 신호는 이전 시동 주기부터 켜져 있던 것으로 본다. */
    fun onIgnitionOn() {
        active.replaceAll { _, a -> a.copy(confirmed = true) }
    }

    companion object {
        const val DEFAULT_MIN_ACTIVE_MS = 10L * 60 * 1000
    }
}
