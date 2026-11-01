package com.zigae.hephaestus.service

import android.util.Log
import com.zigae.hephaestus.data.Repository
import com.zigae.hephaestus.domain.DueLevel
import com.zigae.hephaestus.domain.Gear
import com.zigae.hephaestus.domain.LatLon
import com.zigae.hephaestus.domain.PromptCooldown
import com.zigae.hephaestus.domain.Reminders
import com.zigae.hephaestus.domain.ShopHit
import com.zigae.hephaestus.domain.ShopRules
import com.zigae.hephaestus.domain.ShopSource
import com.zigae.hephaestus.domain.VisitPrompt
import com.zigae.hephaestus.domain.VisitTracker
import com.zigae.hephaestus.navi.NaviMonitor
import com.zigae.hephaestus.vehicle.VehicleMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** 홈 화면에 보여줄 감지 상태. */
data class DetectionStatus(
    val parkedSince: Long? = null,
    val resolving: Boolean = false,
    val shop: ShopHit? = null,
    val ignoredPlace: Boolean = false,
)

/**
 * 차량·내비 이벤트를 [VisitTracker]에 넣고, 질문이 나오면 저장·알림한다.
 * 모든 상태 변경은 메인 스레드에서 처리한다(SDK 콜백은 바인더 스레드에서 온다).
 */
class Detector(
    private val repo: Repository,
    private val vehicle: VehicleMonitor,
    private val navi: NaviMonitor,
    private val notifier: Notifier,
    private val scope: CoroutineScope,
    private val shopSearch: ShopSearch = ShopSearch(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val tracker = VisitTracker()
    private val _status = MutableStateFlow(DetectionStatus())
    val status: StateFlow<DetectionStatus> = _status.asStateFlow()

    fun start() {
        vehicle.onGear = { g -> onMain { handleGear(g) } }
        vehicle.onChargePort = { open -> onMain { tracker.onChargePortOpen(open) } }
        navi.onLocation = { at -> onMain { tracker.onLocation(at)?.let { resolve(it) } } }
        navi.onArrived = { name -> Log.i(TAG, "navi arrived at $name") }
        scope.launch {
            vehicle.state.collect { s -> s.detectedMode?.let { if (repo.data.value.detectedMode != it) repo.setDetectedMode(it) } }
        }
    }

    /** 실제 주행거리 + 데모 보정. */
    fun odometerKm(): Int = (vehicle.state.value.odometerKm ?: 0f).toInt() + repo.data.value.demoOdometerOffsetKm

    fun onScreenOn() = onMain {
        syncDwell()
        tracker.onScreenOn(now(), odometerKm()).prompt?.let(::emit)
        if (vehicle.state.value.gear == Gear.PARK) checkReminders()
    }

    /** 데모: 지금 위치를 정비소로 보고 바로 질문을 만든다. */
    fun simulateVisit(name: String) = onMain {
        val at = navi.state.value.location ?: LatLon(37.41272, 127.09468)
        emit(VisitPrompt(UUID.randomUUID().toString(), name, at, ShopSource.DEMO, now(), now(), odometerKm()))
    }

    /** 정차 중일 때만 교체 시기 알림 (주행 중 알림은 운전을 방해). */
    fun checkRemindersIfParked() = onMain { if (vehicle.state.value.gear == Gear.PARK) checkReminders() }

    private fun checkReminders() {
        val d = repo.data.value
        val mode = d.mode ?: return
        val statuses = Reminders.all(mode, { d.interval(it, mode)!! }, d.records, odometerKm(), now())
        val toNotify = statuses.filter {
            (it.level == DueLevel.SOON || it.level == DueLevel.OVERDUE) && d.notified[it.item] != it.level
        }
        if (toNotify.isEmpty()) return
        toNotify.forEach(notifier::due)
        repo.update { it.copy(notified = it.notified + toNotify.associate { s -> s.item to s.level }) }
        Log.i(TAG, "due notified: ${toNotify.map { "${it.item}=${it.level}" }}")
    }

    private fun handleGear(g: Gear) {
        syncDwell()
        val out = tracker.onGear(g, now(), odometerKm())
        out.started?.let {
            _status.value = DetectionStatus(parkedSince = it.since)
            resolve(it)
        }
        out.prompt?.let(::emit)
        if (g == Gear.PARK) checkReminders() else if (tracker.session == null) _status.value = DetectionStatus()
    }

    private fun resolve(s: VisitTracker.Session) {
        val at = s.at ?: return // 위치가 들어오면 onLocation이 다시 부른다
        _status.value = _status.value.copy(resolving = true)
        scope.launch {
            val d = repo.data.value
            val dest = navi.state.value.destination
            val rule = ShopRules.resolve(at, d.places, dest?.name, dest?.at)
            val hit = rule.hit ?: if (rule.ignored) null else shopSearch.find(at)
            Log.i(TAG, "parked at $at → shop=${hit?.name} (${hit?.source}) ignored=${rule.ignored}")
            withContext(Dispatchers.Main) {
                tracker.onShopResolved(s.id, hit)
                if (tracker.session?.id == s.id) {
                    _status.value = DetectionStatus(parkedSince = s.since, shop = hit, ignoredPlace = rule.ignored)
                }
            }
        }
    }

    private fun emit(p: VisitPrompt) {
        if (PromptCooldown.blocks(p, repo.data.value.lastPrompt)) {
            Log.i(TAG, "prompt skipped (asked here recently): ${p.shopName}")
            return
        }
        Log.i(TAG, "prompt: ${p.shopName} at ${p.odometerKm}km (${p.source})")
        repo.addPrompt(p)
        notifier.visit(p)
    }

    private fun syncDwell() {
        tracker.minDwellMs = repo.data.value.minDwellMinutes * 60_000L
    }

    private fun onMain(block: () -> Unit) {
        scope.launch(Dispatchers.Main.immediate) { block() }
    }

    private companion object {
        const val TAG = "Heph/Detector"
    }
}
