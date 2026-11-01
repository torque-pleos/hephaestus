package com.zigae.hephaestus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class RemindersTest {
    private val zone = TimeZone.getTimeZone("Asia/Seoul")
    private fun date(y: Int, m: Int, d: Int) = Calendar.getInstance(zone).apply { clear(); set(y, m - 1, d) }.timeInMillis
    private fun rec(item: MaintenanceItem, km: Int, t: Long) = MaintenanceRecord("r$km", item, km, t)

    @Test
    fun noRecord() {
        val s = Reminders.status(MaintenanceItem.ENGINE_OIL, Interval(7000, 12), emptyList(), 50_000, date(2026, 11, 1), zone)
        assertEquals(DueLevel.NO_RECORD, s.level)
        assertNull(s.kmLeft)
    }

    @Test
    fun okSoonOverdueByKm() {
        val now = date(2026, 11, 1)
        val recs = listOf(rec(MaintenanceItem.ENGINE_OIL, 52_340, date(2026, 10, 1)))
        val iv = Interval(7000, 12)
        assertEquals(DueLevel.OK, Reminders.status(MaintenanceItem.ENGINE_OIL, iv, recs, 55_000, now, zone).level)
        val soon = Reminders.status(MaintenanceItem.ENGINE_OIL, iv, recs, 58_540, now, zone)
        assertEquals(DueLevel.SOON, soon.level)
        assertEquals(800, soon.kmLeft)
        assertEquals(DueLevel.OVERDUE, Reminders.status(MaintenanceItem.ENGINE_OIL, iv, recs, 59_500, now, zone).level)
    }

    @Test
    fun dueByMonths() {
        val recs = listOf(rec(MaintenanceItem.WIPER, 10_000, date(2025, 11, 20)))
        val s = Reminders.status(MaintenanceItem.WIPER, Interval(0, 12), recs, 10_100, date(2026, 11, 1), zone)
        assertNull(s.kmLeft)
        assertEquals(19, s.daysLeft)
        assertEquals(DueLevel.SOON, s.level)
    }

    @Test
    fun latestRecordWins() {
        val recs = listOf(
            rec(MaintenanceItem.ENGINE_OIL, 40_000, date(2025, 1, 1)),
            rec(MaintenanceItem.ENGINE_OIL, 47_000, date(2026, 6, 1)),
        )
        val s = Reminders.status(MaintenanceItem.ENGINE_OIL, Interval(7000, 0), recs, 50_000, date(2026, 11, 1), zone)
        assertEquals(4_000, s.kmLeft)
    }

    @Test
    fun modesHaveDifferentItems() {
        val ice = MaintenanceItem.forMode(Powertrain.ICE)
        val ev = MaintenanceItem.forMode(Powertrain.EV)
        assertTrue(MaintenanceItem.ENGINE_OIL in ice)
        assertFalse(MaintenanceItem.ENGINE_OIL in ev)
        assertTrue(MaintenanceItem.REDUCER_OIL in ev)
        assertTrue(MaintenanceItem.TIRE in ice && MaintenanceItem.TIRE in ev)
        assertTrue(
            MaintenanceItem.TIRE.defaultInterval(Powertrain.EV)!!.km < MaintenanceItem.TIRE.defaultInterval(Powertrain.ICE)!!.km,
        )
    }

    @Test
    fun allSortsOverdueFirst() {
        val now = date(2026, 11, 1)
        val recs = listOf(
            rec(MaintenanceItem.TIRE, 10_000, now),
            rec(MaintenanceItem.BRAKE_PAD, 0, now),
        )
        val list = Reminders.all(Powertrain.EV, { it.defaultInterval(Powertrain.EV)!! }, recs, 61_000, now)
        // 타이어 11,000km 초과 → 브레이크 패드 1,000km 초과 순
        assertEquals(MaintenanceItem.TIRE, list[0].item)
        assertEquals(MaintenanceItem.BRAKE_PAD, list[1].item)
        assertEquals(DueLevel.OVERDUE, list[1].level)
        assertEquals(DueLevel.NO_RECORD, list.last().level)
    }
}
