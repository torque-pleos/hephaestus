package com.zigae.hephaestus.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import com.zigae.hephaestus.domain.LatLon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class Destination(val name: String, val address: String?, val at: LatLon?)

data class NaviSnapshot(
    val connected: Boolean = false,
    val location: LatLon? = null,
    val address: String? = null,
    val destination: Destination? = null,
    val lastError: String? = null,
)

/** NaviHelper 래퍼: 현재 위치·주소, 목적지 이름·좌표를 주기 조회한다. */
class NaviMonitor(context: Context, private val scope: CoroutineScope) {
    private val navi = NaviHelper(context.applicationContext)
    private val _state = MutableStateFlow(NaviSnapshot())
    val state: StateFlow<NaviSnapshot> = _state.asStateFlow()
    private var job: Job? = null

    var onLocation: ((LatLon) -> Unit)? = null
    var onArrived: ((name: String?) -> Unit)? = null

    private val listener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            val at = LatLon(currentLocationInfo.latitude, currentLocationInfo.longitude)
            if (at.lat == 0.0 && at.lon == 0.0) return
            _state.update { it.copy(location = at, address = currentLocationInfo.address) }
            onLocation?.invoke(at)
        }

        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            val name = destinationInfo.name
            val dest = if (name.isNullOrBlank()) null else Destination(
                name, destinationInfo.address,
                LatLon(destinationInfo.latitude, destinationInfo.longitude).takeIf { it.lat != 0.0 || it.lon != 0.0 },
            )
            if (dest != _state.value.destination) Log.i(TAG, "destination: $dest")
            _state.update { it.copy(destination = dest) }
        }

        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            Log.i(TAG, "arrived: ${destinationArrivedInfo.name}")
            onArrived?.invoke(destinationArrivedInfo.name)
        }

        override fun onRouteCancelled() {
            _state.update { it.copy(destination = null) }
        }

        override fun onRouteEnded() {
            _state.update { it.copy(destination = null) }
        }

        override fun onError(errorCode: NaviErrorCode) {
            Log.w(TAG, "navi error: $errorCode")
            _state.update { it.copy(lastError = errorCode.name) }
        }
    }

    fun start() {
        navi.addListener(listener)
        navi.initialize(
            onConnected = {
                Log.i(TAG, "connected")
                _state.update { it.copy(connected = true, lastError = null) }
                startPolling()
            },
            onFailed = { t ->
                Log.w(TAG, "connect failed", t)
                _state.update { it.copy(connected = false, lastError = "connect: ${t.message}") }
            },
        )
    }

    fun stop() {
        job?.cancel()
        navi.removeListener(listener)
        navi.release()
    }

    /** 데모: 지금 위치에 있는 정비소를 목적지로 설정한다. */
    fun requestDestination(name: String, at: LatLon, address: String?) = runCatching {
        navi.requestRoute(
            RouteInfo(
                latitude = at.lat, longitude = at.lon, poiName = name, poiId = "", address = address ?: "", poiSubId = "",
            ),
        )
    }.onFailure { Log.w(TAG, "requestRoute failed", it) }.isSuccess

    private fun startPolling() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                runCatching {
                    navi.getCurrentLocationInfo()
                    navi.getDestinationInfo()
                }.onFailure { Log.w(TAG, "poll failed", it) }
                delay(POLL_MS)
            }
        }
    }

    private companion object {
        const val TAG = "Heph/Navi"
        const val POLL_MS = 5_000L
    }
}
