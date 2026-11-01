package com.zigae.hephaestus.ui

import com.zigae.hephaestus.domain.DueLevel
import com.zigae.hephaestus.domain.DueStatus
import com.zigae.hephaestus.domain.Interval
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {
    private val number = NumberFormat.getIntegerInstance(Locale.KOREA)

    fun km(v: Int) = "${number.format(v)}km"

    fun date(millis: Long): String = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(millis))

    fun time(millis: Long): String = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(millis))

    fun interval(iv: Interval): String = listOfNotNull(
        iv.km.takeIf { it > 0 }?.let { km(it) },
        iv.months.takeIf { it > 0 }?.let { if (it % 12 == 0) "${it / 12}년" else "${it}개월" },
    ).joinToString(" / ")

    /** "800km 남음", "1,200km 지남", "19일 남음" 중 더 급한 쪽. */
    fun remaining(s: DueStatus): String {
        if (s.level == DueLevel.NO_RECORD) return "기록 없음 · 교체 주기 ${interval(s.interval)}"
        val kmText = s.kmLeft?.let { if (it >= 0) "${km(it)} 남음" else "${km(-it)} 지남" }
        val dayText = s.daysLeft?.let { if (it >= 0) "${it}일 남음" else "${-it}일 지남" }
        val kmUrgent = s.kmLeft?.let { it.toLong() } ?: Long.MAX_VALUE
        val dayUrgent = s.daysLeft?.let { it.toLong() * 40 } ?: Long.MAX_VALUE
        return if (kmUrgent <= dayUrgent) kmText ?: dayText.orEmpty() else dayText ?: kmText.orEmpty()
    }
}
