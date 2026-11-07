package com.zigae.hephaestus.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.telltale.Telltales
import ai.pleos.playground.vehicle.constant.control.OilLevelState
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.telltale.TelltaleState
import ai.pleos.playground.vehicle.constant.telltale.TelltaleType
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.EvChargePortOpenListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.OilLevelStateListener
import ai.pleos.playground.vehicle.listener.TelltaleInfoListener
import android.content.Context
import android.util.Log
import com.zigae.hephaestus.domain.Gear
import com.zigae.hephaestus.domain.Powertrain
import com.zigae.hephaestus.domain.VehicleSignal
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
    val brakeFluid: OilLevelState? = null,
    val transmissionOil: OilLevelState? = null,
    /** 켜진 경고등(ON/BLINK)의 종류 이름. */
    val warnings: Set<String> = emptySet(),
    /** Capability API로 확인한 지원 여부. 조회 전이면 비어 있다. */
    val support: Map<String, Boolean> = emptyMap(),
    val detectedMode: Powertrain? = null,
    val lastError: String? = null,
) {
    /** 레벨·경고등에서 파생한 소모품 신호. */
    val signals: Set<VehicleSignal>
        get() = buildSet {
            if (engineOil.isLow()) add(VehicleSignal.ENGINE_OIL_LOW)
            if (brakeFluid.isLow()) add(VehicleSignal.BRAKE_FLUID_LOW)
            if (transmissionOil.isLow()) add(VehicleSignal.TRANSMISSION_OIL_LOW)
            if (TelltaleType.LOW_VOLT_BATTERY.name in warnings) add(VehicleSignal.BATTERY_12V_WARNING)
        }

    private fun OilLevelState?.isLow() = this == OilLevelState.LOW || this == OilLevelState.CRITICALLY_LOW
}

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
    /** 소모품 신호 집합이 바뀔 때마다 호출. */
    var onSignals: ((Set<VehicleSignal>) -> Unit)? = null

    private var job: Job? = null
    private val registered = mutableSetOf<String>()

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
    private val engineOilListener = levelListener("engine oil") { s, v -> s.copy(engineOil = v) }
    private val brakeFluidListener = levelListener("brake fluid") { s, v -> s.copy(brakeFluid = v) }
    private val transmissionOilListener = levelListener("transmission oil") { s, v -> s.copy(transmissionOil = v) }
    private val telltaleListener = object : TelltaleInfoListener {
        override fun onTelltaleInfoUpdated(t: Telltales?) = publishTelltales(t)
        override fun onFailed(e: Exception) = report("telltale listener", e)
    }

    private fun levelListener(what: String, apply: (VehicleSnapshot, OilLevelState?) -> VehicleSnapshot) = object : OilLevelStateListener {
        override fun onOilLevelUpdated(level: OilLevelState?) {
            Log.i(TAG, "$what level: $level")
            updateSignals { apply(it, level) }
        }
        override fun onFailed(e: Exception) = report("$what listener", e)
    }

    fun start() {
        vehicle.initialize()
        job = scope.launch {
            delay(1_500)
            detectPowertrain()
            checkSupport()
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
            if (GEAR in registered) vehicle.getGear().unregisterGearSelection(gearListener)
            if (ODO in registered) vehicle.getOdometer().unregisterDistanceDriven(odoListener)
            if (PORT in registered) vehicle.getEvBattery().unregisterEvChargePortOpen(portListener)
            if (ENGINE_OIL in registered) vehicle.getSafety().unregisterEngineOilLevelState(engineOilListener)
            if (BRAKE_FLUID in registered) vehicle.getSafety().unregisterBrakeFluidLevelState(brakeFluidListener)
            if (TRANSMISSION_OIL in registered) vehicle.getSafety().unregisterTransmissionOilLevelState(transmissionOilListener)
            if (TELLTALES in registered) vehicle.getSafety().unregisterTelltales(telltaleListener)
        }
        vehicle.release()
    }

    private fun poll() {
        @Suppress("DEPRECATION")
        vehicle.getGear().getCurrentGearSelectionState(::publishGear) { report("gear", it) }
        vehicle.getOdometer().getCurrentDistanceDriven(::publishOdometer) { report("odometer", it) }
        val safety = vehicle.getSafety()
        if (supported(ENGINE_OIL)) safety.getEngineOilLevelState({ v -> updateSignals { it.copy(engineOil = v) } }) { report("engine oil", it) }
        if (supported(BRAKE_FLUID)) safety.getBrakeFluidLevelState({ v -> updateSignals { it.copy(brakeFluid = v) } }) { report("brake fluid", it) }
        if (supported(TRANSMISSION_OIL)) safety.getTransmissionOilLevelState({ v -> updateSignals { it.copy(transmissionOil = v) } }) { report("transmission oil", it) }
        if (supported(TELLTALES)) safety.getTelltalesInfo(::publishTelltales) { report("telltales", it) }
    }

    private fun supported(key: String) = _state.value.support[key] != false

    /** Capability API로 차종별 지원 여부를 한 번 확인한다. 실패한 조회는 미지원으로 둔다. */
    private fun checkSupport() {
        val safety = vehicle.getSafety()
        safety.checkEngineOilLevelStateCapabilities({ c, _ -> mark(ENGINE_OIL, c.readable()) }) { mark(ENGINE_OIL, false, it) }
        safety.checkBrakeFluidLevelStateCapabilities({ c, _ -> mark(BRAKE_FLUID, c.readable()) }) { mark(BRAKE_FLUID, false, it) }
        safety.checkTransmissionOilLevelStateCapabilities({ c, _ -> mark(TRANSMISSION_OIL, c.readable()) }) { mark(TRANSMISSION_OIL, false, it) }
        safety.checkTelltalesCapabilities({ c -> mark(TELLTALES, c.readable()) }) { mark(TELLTALES, false, it) }
    }

    private fun ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig.readable() =
        accessType == AccessType.READ || accessType == AccessType.READ_WRITE

    private fun mark(key: String, ok: Boolean, e: Throwable? = null) {
        Log.i(TAG, "capability $key: ${if (ok) "supported" else "unsupported"}${e?.let { " (${it.message})" } ?: ""}")
        _state.update { it.copy(support = it.support + (key to ok)) }
    }

    private fun registerMissing() {
        val safety = vehicle.getSafety()
        register(GEAR) { vehicle.getGear().registerGearSelection(gearListener) }
        register(ODO) { vehicle.getOdometer().registerDistanceDriven(odoListener) }
        if (_state.value.detectedMode == Powertrain.EV) register(PORT) { vehicle.getEvBattery().registerEvChargePortOpen(portListener) }
        if (supported(ENGINE_OIL)) register(ENGINE_OIL) { safety.registerEngineOilLevelState(engineOilListener) }
        if (supported(BRAKE_FLUID)) register(BRAKE_FLUID) { safety.registerBrakeFluidLevelState(brakeFluidListener) }
        if (supported(TRANSMISSION_OIL)) register(TRANSMISSION_OIL) { safety.registerTransmissionOilLevelState(transmissionOilListener) }
        if (supported(TELLTALES)) register(TELLTALES) { safety.registerTelltales(telltaleListener) }
    }

    private fun register(key: String, block: () -> Unit) {
        if (key in registered) return
        if (runCatching(block).isSuccess) registered += key
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

    private fun publishTelltales(t: Telltales?) {
        val on = t?.telltales.orEmpty()
            .filter { it.state == TelltaleState.ON || it.state == TelltaleState.BLINK }
            .map { it.type.name }.toSet()
        if (on == _state.value.warnings) return
        Log.i(TAG, "telltales on: $on")
        updateSignals { it.copy(warnings = on) }
    }

    private fun updateSignals(transform: (VehicleSnapshot) -> VehicleSnapshot) {
        val before = _state.value.signals
        _state.update(transform)
        val after = _state.value.signals
        if (after != before) {
            Log.i(TAG, "signals: $before -> $after")
            onSignals?.invoke(after)
        }
    }

    private fun report(what: String, e: Throwable) {
        // VehicleException은 배포 AAR에서 난독화되어 있어 메시지만 쓴다.
        val msg = "$what: ${e.message ?: e.javaClass.simpleName}"
        if (msg != _state.value.lastError) Log.w(TAG, msg)
        _state.update { it.copy(lastError = msg) }
    }

    companion object {
        private const val TAG = "Heph/Vehicle"
        private const val POLL_MS = 5_000L
        private const val GEAR = "gear"
        private const val ODO = "odometer"
        private const val PORT = "chargePort"
        const val ENGINE_OIL = "engineOil"
        const val BRAKE_FLUID = "brakeFluid"
        const val TRANSMISSION_OIL = "transmissionOil"
        const val TELLTALES = "telltales"
    }
}
