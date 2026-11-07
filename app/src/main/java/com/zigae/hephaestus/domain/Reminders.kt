package com.zigae.hephaestus.domain

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.min

enum class DueLevel { NO_RECORD, OK, SOON, OVERDUE }

/**
 * 항목 하나의 교체 시기 상태.
 * [kmLeft]/[daysLeft]는 해당 기준이 없거나 기록이 없으면 null. 음수면 지났다는 뜻.
 */
data class DueStatus(
    val item: MaintenanceItem,
    val interval: Interval,
    val last: MaintenanceRecord?,
    val kmLeft: Int?,
    val daysLeft: Int?,
    val level: DueLevel,
    /** 차량이 지금 부족·경고를 알리고 있는 항목. 교체 주기와 별개로 맨 위에 올린다. */
    val warning: Boolean = false,
) {
    /** 주행거리 기준 진행률(0..1+). 거리 기준이 없으면 기간 기준. */
    val progress: Float?
        get() {
            if (last == null) return null
            if (kmLeft != null && interval.km > 0) return (interval.km - kmLeft).toFloat() / interval.km
            if (daysLeft != null && interval.months > 0) {
                val total = interval.months * 30f
                return (total - daysLeft) / total
            }
            return null
        }
}

object Reminders {
    const val SOON_KM = 1_000
    const val SOON_DAYS = 30
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun status(
        item: MaintenanceItem,
        interval: Interval,
        records: List<MaintenanceRecord>,
        odometerKm: Int,
        nowMillis: Long,
        zone: TimeZone = TimeZone.getDefault(),
    ): DueStatus {
        val last = records.filter { it.item == item }.maxWithOrNull(compareBy({ it.odometerKm }, { it.timeMillis }))
            ?: return DueStatus(item, interval, null, null, null, DueLevel.NO_RECORD)

        val kmLeft = if (interval.km > 0) last.odometerKm + interval.km - odometerKm else null
        val daysLeft = if (interval.months > 0) {
            val due = Calendar.getInstance(zone).apply {
                timeInMillis = last.timeMillis
                add(Calendar.MONTH, interval.months)
            }.timeInMillis
            Math.floorDiv(due - nowMillis, DAY_MS).toInt()
        } else null

        val level = when {
            (kmLeft != null && kmLeft < 0) || (daysLeft != null && daysLeft < 0) -> DueLevel.OVERDUE
            (kmLeft != null && kmLeft <= SOON_KM) || (daysLeft != null && daysLeft <= SOON_DAYS) -> DueLevel.SOON
            else -> DueLevel.OK
        }
        return DueStatus(item, interval, last, kmLeft, daysLeft, level)
    }

    fun all(
        mode: Powertrain,
        intervals: (MaintenanceItem) -> Interval,
        records: List<MaintenanceRecord>,
        odometerKm: Int,
        nowMillis: Long,
        warnings: Set<MaintenanceItem> = emptySet(),
    ): List<DueStatus> = MaintenanceItem.forMode(mode)
        .map { status(it, intervals(it), records, odometerKm, nowMillis).copy(warning = it in warnings) }
        .sortedWith(compareBy({ !it.warning }, { order(it.level) }, { urgency(it) }))

    private fun order(level: DueLevel) = when (level) {
        DueLevel.OVERDUE -> 0
        DueLevel.SOON -> 1
        DueLevel.OK -> 2
        DueLevel.NO_RECORD -> 3
    }

    /** 같은 단계 안에서는 남은 거리(일 단위를 대략 km로 환산)가 적은 순. */
    private fun urgency(s: DueStatus): Int {
        val byKm = s.kmLeft ?: Int.MAX_VALUE
        val byDays = s.daysLeft?.let { it * 40 } ?: Int.MAX_VALUE
        return min(byKm, byDays)
    }
}
