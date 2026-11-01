package com.zigae.hephaestus

import android.app.Application
import android.content.Context
import com.zigae.hephaestus.data.Repository
import com.zigae.hephaestus.navi.NaviMonitor
import com.zigae.hephaestus.service.Detector
import com.zigae.hephaestus.service.Notifier
import com.zigae.hephaestus.vehicle.VehicleMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** 서비스와 화면이 같은 인스턴스를 쓰도록 앱 단위로 묶는다. */
class AppGraph(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repo = Repository(context)
    val notifier = Notifier(context)
    val vehicle = VehicleMonitor(context, scope)
    val navi = NaviMonitor(context, scope)
    val detector = Detector(repo, vehicle, navi, notifier, scope)
    private var started = false

    @Synchronized
    fun ensureStarted() {
        if (started) return
        started = true
        detector.start()
        vehicle.start()
        navi.start()
    }
}

class HephApp : Application() {
    val graph by lazy { AppGraph(this) }

    companion object {
        fun graph(context: Context) = (context.applicationContext as HephApp).graph
    }
}
