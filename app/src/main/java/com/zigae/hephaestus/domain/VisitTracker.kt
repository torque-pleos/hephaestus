package com.zigae.hephaestus.domain

/** 기록 유도 질문 하나. 정비소에서 출차(또는 시동 ON)할 때 만들어진다. */
data class VisitPrompt(
    val id: String,
    val shopName: String,
    val at: LatLon,
    val source: ShopSource,
    val arrivedAt: Long,
    val createdAt: Long,
    val odometerKm: Int,
    /** 차량 신호가 꺼져서 정비된 것으로 보이는 항목(미리 고른 상태로 보여준다). */
    val signals: List<VehicleSignal> = emptyList(),
) {
    val isSignalOnly: Boolean get() = source == ShopSource.SIGNAL
}

enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }

/**
 * 정비소 방문 감지 상태 기계 (안드로이드 의존 없음).
 *
 * - 기어가 P가 되면 정차 세션을 시작하고, 호출자가 그 위치가 정비소인지 확인해 [onShopResolved]로 알려준다.
 * - 정비소에 [minDwellMs] 이상 머문 뒤 **시동 ON(화면 켜짐)** 또는 **출차(P → 다른 기어)** 가 오면 질문을 한 번 만든다.
 *   정비소에 맡기면 시동을 끄므로 머무는 동안에는 앱이 동작하지 않는다 → 다시 켜지는 순간이 기록하기 가장 좋은 때.
 * - 충전구가 열린 적 있는 세션은 충전 정차로 보고 묻지 않는다(내 정비소로 등록한 곳은 예외).
 */
class VisitTracker(
    var minDwellMs: Long = DEFAULT_MIN_DWELL_MS,
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
) {
    data class Session(
        val id: String,
        val since: Long,
        val at: LatLon?,
        val shop: ShopHit? = null,
        val resolved: Boolean = false,
        val charging: Boolean = false,
        val prompted: Boolean = false,
        /** 정차 중에 꺼진 차량 신호. 출차 질문에 미리 고른 항목으로 들어간다. */
        val signals: List<VehicleSignal> = emptyList(),
    )

    var session: Session? = null
        private set

    private var gear = Gear.UNKNOWN
    private var location: LatLon? = null

    /** 정차 세션이 새로 시작되면 그 세션을 돌려준다(호출자가 정비소 판별을 시작). */
    fun onGear(newGear: Gear, now: Long, odometerKm: Int): Outcome {
        val previous = gear
        gear = newGear
        if (newGear == Gear.PARK) {
            if (session == null) {
                val s = Session(newId(), now, location)
                session = s
                return Outcome(started = s)
            }
            return Outcome()
        }
        if (newGear == Gear.UNKNOWN) return Outcome()
        // 출차: P에서 다른 기어로
        val s = session
        session = null
        if (previous != Gear.PARK || s == null) return Outcome()
        return Outcome(prompt = promptFor(s, now, odometerKm) ?: signalPromptFor(s, now, odometerKm))
    }

    /**
     * 차량 신호가 꺼졌다(정비된 것으로 보인다). 정비소에 정차 중이면 그 세션에 붙여 출차 때 한 번에 묻고,
     * 아니면 바로 물을 질문을 돌려준다(정차 중이 아닐 때는 호출자가 다음 정차까지 미룬다).
     */
    fun onSignalsCleared(signals: List<VehicleSignal>, now: Long, odometerKm: Int, at: LatLon?): VisitPrompt? {
        if (signals.isEmpty()) return null
        val s = session
        if (s != null && s.shop != null && !s.prompted) {
            session = s.copy(signals = (s.signals + signals).distinct())
            return null
        }
        return VisitPrompt(newId(), SIGNAL_SHOP_NAME, at ?: location ?: LatLon(0.0, 0.0), ShopSource.SIGNAL, now, now, odometerKm, signals.distinct())
    }

    /** 위치 갱신. 위치 없이 시작한 세션이 이제 위치를 얻었으면 그 세션을 돌려준다(판별 시작 신호). */
    fun onLocation(at: LatLon): Session? {
        location = at
        val s = session ?: return null
        if (s.at != null) return null
        return s.copy(at = at).also { session = it }
    }

    /** 정비소 판별 결과. 세션이 바뀌었으면 무시한다. */
    fun onShopResolved(sessionId: String, hit: ShopHit?) {
        val s = session ?: return
        if (s.id != sessionId) return
        session = s.copy(shop = hit, resolved = true)
    }

    fun onChargePortOpen(open: Boolean) {
        val s = session ?: return
        if (open) session = s.copy(charging = true)
    }

    /** 시동 ON에 해당하는 화면 켜짐. 아직 정차 중이면 그 자리에서 묻는다. */
    fun onScreenOn(now: Long, odometerKm: Int): Outcome {
        val s = session ?: return Outcome()
        val prompt = promptFor(s, now, odometerKm) ?: signalPromptFor(s, now, odometerKm) ?: return Outcome()
        return Outcome(prompt = prompt)
    }

    /** 정비소 질문 조건(머문 시간 등)에 못 미쳐도, 꺼진 차량 신호가 있으면 그 항목만이라도 묻는다. */
    private fun signalPromptFor(s: Session, now: Long, odometerKm: Int): VisitPrompt? {
        if (s.prompted || s.signals.isEmpty()) return null
        if (session?.id == s.id) session = s.copy(prompted = true)
        val at = s.shop?.at ?: s.at ?: location ?: LatLon(0.0, 0.0)
        return VisitPrompt(newId(), s.shop?.name ?: SIGNAL_SHOP_NAME, at, s.shop?.source ?: ShopSource.SIGNAL, s.since, now, odometerKm, s.signals)
    }

    private fun promptFor(s: Session, now: Long, odometerKm: Int): VisitPrompt? {
        if (s.prompted) return null
        val shop = s.shop ?: return null
        if (now - s.since < minDwellMs) return null
        if (s.charging && shop.source != ShopSource.MY_SHOP) return null
        if (session?.id == s.id) session = s.copy(prompted = true)
        return VisitPrompt(newId(), shop.name, shop.at, shop.source, s.since, now, odometerKm, s.signals)
    }

    data class Outcome(val started: Session? = null, val prompt: VisitPrompt? = null)

    companion object {
        const val DEFAULT_MIN_DWELL_MS = 20L * 60 * 1000
        const val SIGNAL_SHOP_NAME = "차량 신호"
    }
}

/** 같은 정비소에서 정차·출차를 반복할 때(정비 중 이동, 재주차) 같은 질문을 다시 하지 않는다. */
object PromptCooldown {
    const val WINDOW_MS = 6L * 60 * 60 * 1000
    const val RADIUS_M = 300.0

    fun blocks(next: VisitPrompt, last: VisitPrompt?): Boolean {
        if (last == null || next.source == ShopSource.DEMO || next.source == ShopSource.SIGNAL) return false
        return next.createdAt - last.createdAt in 0 until WINDOW_MS && next.at.distanceTo(last.at) <= RADIUS_M
    }
}
