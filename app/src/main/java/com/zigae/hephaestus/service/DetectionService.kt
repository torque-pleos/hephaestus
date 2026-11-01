package com.zigae.hephaestus.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import com.zigae.hephaestus.HephApp

/**
 * 정비소 방문 감지 상주 서비스.
 * 시동 OFF는 Suspend-to-RAM이라 프로세스가 유지되고, 시동 ON(복귀) 때 SCREEN_ON이 온다 → 질문 시점.
 */
class DetectionService : Service() {
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i(TAG, "screen: ${intent.action}")
            if (intent.action == Intent.ACTION_SCREEN_ON) HephApp.graph(this@DetectionService).detector.onScreenOn()
        }
    }
    private var registered = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val graph = HephApp.graph(this)
        runCatching {
            startForeground(Notifier.SERVICE_ID, graph.notifier.serviceNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        }.onFailure { Log.w(TAG, "startForeground failed", it) }
        if (!registered) {
            registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
            registered = true
        }
        graph.ensureStarted()
        Log.i(TAG, "started (${intent?.getStringExtra(EXTRA_FROM)})")
        return START_STICKY
    }

    override fun onDestroy() {
        if (registered) unregisterReceiver(screenReceiver)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Heph/Service"
        const val EXTRA_FROM = "from"

        fun start(context: Context, from: String) {
            runCatching {
                context.startForegroundService(Intent(context, DetectionService::class.java).putExtra(EXTRA_FROM, from))
            }.onFailure { Log.w(TAG, "start failed from $from", it) }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) DetectionService.start(context, "boot")
    }
}
