package com.zigae.hephaestus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalTrackerTest {
    private val min = 60 * 1000L
    private val oil = VehicleSignal.ENGINE_OIL_LOW
    private val battery = VehicleSignal.BATTERY_12V_WARNING

    @Test
    fun flickerIsNotCleared() {
        val t = SignalTracker(minActiveMs = 10 * min)
        assertTrue(t.onSignals(0L, setOf(oil)).isEmpty())
        assertTrue(t.onSignals(2 * min, emptySet()).isEmpty()) // 10분 안에 꺼짐 → 센서 흔들림
        assertTrue(t.current.isEmpty())
    }

    @Test
    fun clearedAfterMinActive() {
        val t = SignalTracker(minActiveMs = 10 * min)
        t.onSignals(0L, setOf(oil))
        assertEquals(listOf(oil), t.onSignals(11 * min, emptySet()))
    }

    @Test
    fun clearedAfterIgnitionCycle() {
        val t = SignalTracker(minActiveMs = 10 * min)
        t.onSignals(0L, setOf(oil, battery))
        t.onIgnitionOn()
        assertEquals(listOf(oil), t.onSignals(min, setOf(battery)))
        assertEquals(setOf(battery), t.current)
    }

    @Test
    fun newSignalAfterIgnitionIsNotConfirmed() {
        val t = SignalTracker(minActiveMs = 10 * min)
        t.onIgnitionOn()
        t.onSignals(0L, setOf(oil))
        assertTrue(t.onSignals(min, emptySet()).isEmpty())
    }
}

class VisitTrackerSignalTest {
    private val shopAt = LatLon(37.41272, 127.09468)
    private val shop = ShopHit("시흥 카센터", shopAt, ShopSource.DESTINATION)
    private val min = 60 * 1000L
    private var ids = 0
    private fun tracker() = VisitTracker(minDwellMs = 20 * min, newId = { "id${ids++}" })

    @Test
    fun signalAtShopJoinsDeparturePrompt() {
        val t = tracker()
        t.onLocation(shopAt)
        val s = t.onGear(Gear.PARK, 0L, 52_000).started!!
        t.onShopResolved(s.id, shop)
        assertNull(t.onSignalsCleared(listOf(VehicleSignal.BRAKE_FLUID_LOW), 30 * min, 52_000, shopAt))
        val p = t.onGear(Gear.DRIVE, 40 * min, 52_000).prompt!!
        assertEquals("시흥 카센터", p.shopName)
        assertEquals(listOf(VehicleSignal.BRAKE_FLUID_LOW), p.signals)
    }

    @Test
    fun shortShopStopStillAsksAboutSignal() {
        val t = tracker()
        t.onLocation(shopAt)
        val s = t.onGear(Gear.PARK, 0L, 52_000).started!!
        t.onShopResolved(s.id, shop)
        t.onSignalsCleared(listOf(VehicleSignal.BATTERY_12V_WARNING), 3 * min, 52_000, shopAt)
        val p = t.onGear(Gear.DRIVE, 5 * min, 52_000).prompt
        assertNotNull(p)
        assertEquals(listOf(VehicleSignal.BATTERY_12V_WARNING), p!!.signals)
        assertEquals("시흥 카센터", p.shopName)
    }

    @Test
    fun signalAwayFromShopPromptsImmediately() {
        val t = tracker()
        t.onLocation(LatLon(37.5, 127.0))
        val s = t.onGear(Gear.PARK, 0L, 52_000).started!!
        t.onShopResolved(s.id, null)
        val p = t.onSignalsCleared(listOf(VehicleSignal.ENGINE_OIL_LOW), min, 52_000, null)
        assertNotNull(p)
        assertTrue(p!!.isSignalOnly)
        assertEquals(ShopSource.SIGNAL, p.source)
        assertEquals(LatLon(37.5, 127.0), p.at)
        // 출차 때 같은 세션으로 또 묻지 않는다
        assertNull(t.onGear(Gear.DRIVE, 2 * min, 52_000).prompt)
    }

    @Test
    fun signalPromptIgnoresCooldown() {
        val last = VisitPrompt("a", "시흥 카센터", shopAt, ShopSource.DESTINATION, 0L, 0L, 52_000)
        val next = VisitPrompt("b", VisitTracker.SIGNAL_SHOP_NAME, shopAt, ShopSource.SIGNAL, min, min, 52_000, listOf(VehicleSignal.ENGINE_OIL_LOW))
        assertTrue(!PromptCooldown.blocks(next, last))
    }
}

class RemindersWarningTest {
    @Test
    fun warningItemSortsFirst() {
        val all = Reminders.all(Powertrain.ICE, { it.defaultInterval(Powertrain.ICE)!! }, emptyList(), 50_000, 0L, warnings = setOf(MaintenanceItem.BRAKE_FLUID))
        assertEquals(MaintenanceItem.BRAKE_FLUID, all.first().item)
        assertTrue(all.first().warning)
        assertTrue(all.drop(1).none { it.warning })
    }
}
