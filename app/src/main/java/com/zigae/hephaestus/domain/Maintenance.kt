package com.zigae.hephaestus.domain

/** 차량 동력원. 모드별로 관리 항목과 기본 주기가 다르다. */
enum class Powertrain(val label: String) {
    ICE("내연기관"),
    EV("전기차"),
}

/** 교체 주기. 0이면 해당 기준을 쓰지 않는다. */
data class Interval(val km: Int, val months: Int) {
    init {
        require(km >= 0 && months >= 0) { "interval must be non-negative" }
        require(km > 0 || months > 0) { "interval needs km or months" }
    }
}

/**
 * 관리 항목과 모드별 기본 주기.
 * 값은 국산 승용차 정비 안내서의 일반적인 범위를 단순화한 기본값이며, 사용자가 설정에서 바꿀 수 있다.
 */
enum class MaintenanceItem(val label: String, private val ice: Interval?, private val ev: Interval?) {
    ENGINE_OIL("엔진오일·필터", Interval(7_000, 12), null),
    AIR_CLEANER("에어클리너", Interval(20_000, 24), null),
    SPARK_PLUG("점화플러그", Interval(100_000, 0), null),
    TRANSMISSION_OIL("변속기 오일", Interval(100_000, 0), null),
    REDUCER_OIL("감속기 오일", null, Interval(60_000, 0)),
    BATTERY_COOLANT("배터리 냉각수", null, Interval(200_000, 120)),

    // 전기차는 무거워서 타이어 마모가 빠르고, 회생제동 덕분에 브레이크 패드는 오래 간다.
    TIRE_ROTATION("타이어 위치 교환", Interval(10_000, 12), Interval(8_000, 12)),
    TIRE("타이어 교체", Interval(50_000, 48), Interval(40_000, 48)),
    BRAKE_PAD("브레이크 패드", Interval(40_000, 0), Interval(60_000, 0)),
    BRAKE_FLUID("브레이크액", Interval(40_000, 24), Interval(40_000, 24)),
    CABIN_FILTER("에어컨 필터", Interval(15_000, 12), Interval(15_000, 12)),
    WIPER("와이퍼", Interval(0, 12), Interval(0, 12)),
    BATTERY_12V("12V 배터리", Interval(0, 48), Interval(0, 48)),
    ;

    fun defaultInterval(mode: Powertrain): Interval? = when (mode) {
        Powertrain.ICE -> ice
        Powertrain.EV -> ev
    }

    fun appliesTo(mode: Powertrain) = defaultInterval(mode) != null

    companion object {
        fun forMode(mode: Powertrain) = entries.filter { it.appliesTo(mode) }
        fun fromKey(key: String) = entries.firstOrNull { it.name == key }
    }
}

/** 정비 기록 한 건. 주행거리는 기록 시점 차량 값이 자동으로 들어간다. */
data class MaintenanceRecord(
    val id: String,
    val item: MaintenanceItem,
    val odometerKm: Int,
    val timeMillis: Long,
    val shopName: String? = null,
)
