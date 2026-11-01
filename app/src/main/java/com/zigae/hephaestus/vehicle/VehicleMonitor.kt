package com.zigae.hephaestus.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.OilLevelState
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.EvChargePortOpenListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.OilLevelStateListener
import android.content.Context
import android.util.Log
import com.zigae.hephaestus.domain.Gear
import com.zigae.hephaestus.domain.Powertrain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class VehicleSnapshot(
    val gear: Gear = Gear.UNKNOWN,
    /** 차량 총 주행거리(km). 조회 전이면 null. */
    val odometerKm: Float? = null,
    val chargePortOpen: Boolean? = null,
    val engineOil: OilLevelState? = null,
    val detectedMode: Powertrain? = null,
    val lastError: String? = null,
)

/**
 * Vehicle State 조회 래퍼. Vehicle SDK는 연결 완료 콜백이 없어서
 * 리스너 등록이 성공할 때까지 재시도하고, 주기 조회로 값을 보정한다.
 */
class VehicleMonitor(context: Context, private val scope: CoroutineScope) {
    private val vehicle = Vehicle(context.applicationContext)
    private val _state = MutableStateFlow(VehicleSnapshot())
    val state: StateFlow<VehicleSnapshot> = _state.asStateFlow()

    /** 기어가 바뀔 때마다 호출 (같은 값 반복은 거른다). */
    var onGear: ((Gear) -> Unit)? = null
    var onOdometer: ((Float) -> Unit)? = null
    var onChargePort: ((Boolean) -> Unit)? = null

    private var job: Job? = null
    private var gearRegistered = false
    private var odoRegistered = false
    private var portRegistered = false
    private var oilRegistered = false

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) = publishGear(gear)
        override fun onFailed(e: Exception) = report("gear listener", e)
    }
    private val odoListener = object : DistanceDrivenListener {
        override fun onDistanceDrivenUpdated(distance: Float?) = publishOdometer(distance)
        override fun onFailed(e: Exception) = report("odometer listener", e)
    }
    private val portListener = object : EvChargePortOpenListener {
        override fun onEvChargePortOpenUpdated(open: Boolean?) = publishPort(open)
        override fun onFailed(e: Exception) = report("charge port listener", e)
    }
    private val oilListener = object : OilLevelStateListener {
        override fun onOilLevelUpdated(level: OilLevelState?) {
            _state.update { it.copy(engineOil = level) }
        }
        override fun onFailed(e: Exception) = report("engine oil listener", e)
    }

    fun start() {
        vehicle.initialize()
        job = scope.launch {
            delay(1_500)
            detectPowertrain()
            while (isActive) {
                poll()
                registerMissing()
                delay(POLL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        runCatching {
            if (gearRegistered) vehicle.getGear().unregisterGearSelection(gearListener)
            if (odoRegistered) vehicle.getOdometer().unregisterDistanceDriven(odoListener)
            if (portRegistered) vehicle.getEvBattery().unregisterEvChargePortOpen(portListener)
            if (oilRegistered) vehicle.getSafety().unregisterEngineOilLevelState(oilListener)
        }
        vehicle.release()
    }

    private fun poll() {
        @Suppress("DEPRECATION")
        vehicle.getGear().getCurrentGearSelectionState(::publishGear) { report("gear", it) }
        vehicle.getOdometer().getCurrentDistanceDriven(::publishOdometer) { report("odometer", it) }
    }

    private fun registerMissing() {
        if (!gearRegistered) gearRegistered = runCatching { vehicle.getGear().registerGearSelection(gearListener) }.isSuccess
        if (!odoRegistered) odoRegistered = runCatching { vehicle.getOdometer().registerDistanceDriven(odoListener) }.isSuccess
        val mode = _state.value.detectedMode
        if (mode == Powertrain.EV && !portRegistered) {
            portRegistered = runCatching { vehicle.getEvBattery().registerEvChargePortOpen(portListener) }.isSuccess
        }
        if (mode == Powertrain.ICE && !oilRegistered) {
            oilRegistered = runCatching { vehicle.getSafety().registerEngineOilLevelState(oilListener) }.isSuccess
        }
    }

    /**
     * 연료 종류를 직접 주는 API가 없어 배터리 용량 → 연료 잔량 순으로 추정한다.
     * 배터리 용량이 0보다 크면 전기차, 아니면 연료 잔량이 읽히면 내연기관.
     */
    private fun detectPowertrain() {
        vehicle.getEvBattery().getEvBatteryCapacity({ capacity ->
            if (capacity != null && capacity > 0f) {
                Log.i(TAG, "powertrain: EV (battery capacity $capacity)")
                _state.update { it.copy(detectedMode = Powertrain.EV) }
            } else detectFuel()
        }) { e ->
            Log.i(TAG, "battery capacity unavailable: ${e.message}")
            detectFuel()
        }
    }

    private fun detectFuel() {
        vehicle.getFuel().getFuelPercent({ percent ->
            Log.i(TAG, "powertrain: ICE (fuel $percent%)")
            _state.update { it.copy(detectedMode = Powertrain.ICE) }
        }) { e -> Log.i(TAG, "fuel unavailable: ${e.message}") }
    }

    private fun publishGear(g: VehicleGear?) {
        val gear = when (g) {
            VehicleGear.GEAR_PARK -> Gear.PARK
            VehicleGear.GEAR_REVERSE -> Gear.REVERSE
            VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
            null, VehicleGear.GEAR_UNKNOWN -> Gear.UNKNOWN
            else -> Gear.DRIVE // D, 수동 1~9단
        }
        if (gear == _state.value.gear) return
        Log.i(TAG, "gear: ${_state.value.gear} -> $gear")
        _state.update { it.copy(gear = gear) }
        onGear?.invoke(gear)
    }

    private fun publishOdometer(km: Float?) {
        if (km == null || km == _state.value.odometerKm) return
        _state.update { it.copy(odometerKm = km) }
        onOdometer?.invoke(km)
    }

    private fun publishPort(open: Boolean?) {
        if (open == null || open == _state.value.chargePortOpen) return
        Log.i(TAG, "charge port open: $open")
        _state.update { it.copy(chargePortOpen = open) }
        onChargePort?.invoke(open)
    }

    private fun report(what: String, e: Throwable) {
        // VehicleException은 배포 AAR에서 난독화되어 있어 메시지만 쓴다.
        val msg = "$what: ${e.message ?: e.javaClass.simpleName}"
        if (msg != _state.value.lastError) Log.w(TAG, msg)
        _state.update { it.copy(lastError = msg) }
    }

    private companion object {
        const val TAG = "Heph/Vehicle"
        const val POLL_MS = 5_000L
    }
}
