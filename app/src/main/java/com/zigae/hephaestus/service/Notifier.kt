package com.zigae.hephaestus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.zigae.hephaestus.MainActivity
import com.zigae.hephaestus.R
import com.zigae.hephaestus.domain.DueLevel
import com.zigae.hephaestus.domain.DueStatus
import com.zigae.hephaestus.domain.VisitPrompt
import com.zigae.hephaestus.ui.Format

/**
 * 차량 화면 알림. Pleos 에뮬레이터는 헤즈업 배너를 띄우지 않으므로(2026-11-01 확인)
 * 알림 센터에 남기고, 같은 내용을 앱 홈 카드로도 보여준다.
 */
class Notifier(private val context: Context) {
    private val nm = context.getSystemService(NotificationManager::class.java)

    init {
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_VISIT, "정비 기록 질문", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CH_DUE, "교체 시기 알림", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CH_SERVICE, "방문 감지", NotificationManager.IMPORTANCE_MIN),
            ),
        )
    }

    fun serviceNotification(): Notification = Notification.Builder(context, CH_SERVICE)
        .setSmallIcon(R.drawable.ic_wrench)
        .setContentTitle("정비소 방문 감지 중")
        .setContentIntent(openApp())
        .setOngoing(true)
        .build()

    fun visit(p: VisitPrompt) {
        val n = Notification.Builder(context, CH_VISIT)
            .setSmallIcon(R.drawable.ic_wrench)
            .setContentTitle("${p.shopName}에서 출발하시네요")
            .setContentText("어떤 정비를 받으셨나요? · ${Format.km(p.odometerKm)} 자동 기록")
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()
        nm.notify(p.id.hashCode(), n)
    }

    fun due(s: DueStatus) {
        val title = when (s.level) {
            DueLevel.OVERDUE -> "${s.item.label} 시기가 지났어요"
            else -> "${s.item.label} 시기가 다가와요"
        }
        val n = Notification.Builder(context, CH_DUE)
            .setSmallIcon(R.drawable.ic_wrench)
            .setContentTitle(title)
            .setContentText(Format.remaining(s))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()
        nm.notify(DUE_BASE + s.item.ordinal, n)
    }

    fun cancelVisit(id: String) = nm.cancel(id.hashCode())

    private fun openApp() = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val SERVICE_ID = 1
        private const val DUE_BASE = 1000
        private const val CH_VISIT = "visit"
        private const val CH_DUE = "due"
        private const val CH_SERVICE = "service"
    }
}
