package com.zigae.hephaestus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VisitTrackerTest {
    private val shopAt = LatLon(37.41272, 127.09468)
    private val shop = ShopHit("시흥 카센터", shopAt, ShopSource.DESTINATION)
    private val min = 60 * 1000L
    private var ids = 0
    private fun tracker() = VisitTracker(minDwellMs = 20 * min, newId = { "id${ids++}" })

    private fun parkedAtShop(t: VisitTracker, at: Long = 0L): VisitTracker.Session {
        t.onLocation(shopAt)
        val s = t.onGear(Gear.PARK, at, 52_000).started!!
        t.onShopResolved(s.id, shop)
        return s
    }

    @Test
    fun promptsOnDepartureAfterDwell() {
        val t = tracker()
        parkedAtShop(t)
        val out = t.onGear(Gear.DRIVE, 60 * min, 52_340)
        val p = assertNotNullAndGet(out.prompt)
        assertEquals("시흥 카센터", p.shopName)
        assertEquals(52_340, p.odometerKm)
        assertNull(t.session)
    }

    @Test
    fun noPromptForShortStop() {
        val t = tracker()
        parkedAtShop(t)
        assertNull(t.onGear(Gear.DRIVE, 5 * min, 52_000).prompt)
    }

    @Test
    fun promptsOnceOnScreenOnThenNotAgainOnDeparture() {
        val t = tracker()
        parkedAtShop(t)
        assertNotNull(t.onScreenOn(45 * min, 52_000).prompt)
        assertNull(t.onScreenOn(46 * min, 52_000).prompt)
        assertNull(t.onGear(Gear.DRIVE, 47 * min, 52_000).prompt)
    }

    @Test
    fun noPromptWhenNotShop() {
        val t = tracker()
        t.onLocation(shopAt)
        val s = t.onGear(Gear.PARK, 0, 1)!!.started!!
        t.onShopResolved(s.id, null)
        assertNull(t.onGear(Gear.DRIVE, 90 * min, 1).prompt)
    }

    @Test
    fun chargingStopIsSkippedUnlessMyShop() {
        val t = tracker()
        parkedAtShop(t)
        t.onChargePortOpen(true)
        assertNull(t.onGear(Gear.DRIVE, 90 * min, 1).prompt)

        val t2 = tracker()
        t2.onLocation(shopAt)
        val s = t2.onGear(Gear.PARK, 0, 1).started!!
        t2.onShopResolved(s.id, shop.copy(source = ShopSource.MY_SHOP))
        t2.onChargePortOpen(true)
        assertNotNull(t2.onGear(Gear.DRIVE, 90 * min, 1).prompt)
    }

    @Test
    fun staleShopResultIsIgnored() {
        val t = tracker()
        val first = parkedAtShop(t)
        t.onGear(Gear.DRIVE, 1 * min, 1)
        val second = t.onGear(Gear.PARK, 2 * min, 1).started!!
        t.onShopResolved(first.id, shop)
        assertNull(t.session!!.shop)
        assertEquals(second.id, t.session!!.id)
    }

    @Test
    fun sessionWithoutLocationReportsWhenLocationArrives() {
        val t = tracker()
        val s = t.onGear(Gear.PARK, 0, 1).started!!
        assertNull(s.at)
        val updated = t.onLocation(shopAt)
        assertEquals(shopAt, updated!!.at)
        assertNull(t.onLocation(shopAt))
    }

    @Test
    fun zeroDwellPromptsImmediately() {
        val t = tracker().apply { minDwellMs = 0 }
        parkedAtShop(t)
        assertNotNull(t.onGear(Gear.REVERSE, 1, 1).prompt)
    }

    private fun <T> assertNotNullAndGet(v: T?): T {
        assertNotNull(v)
        return v!!
    }
}

class PromptCooldownTest {
    private val at = LatLon(37.41272, 127.09468)
    private fun p(t: Long, where: LatLon = at, src: ShopSource = ShopSource.DESTINATION) = VisitPrompt("x$t", "카센터", where, src, t, t, 1)
    private val h = 60 * 60 * 1000L

    @Test
    fun samePlaceWithinWindowIsBlocked() {
        assertEquals(true, PromptCooldown.blocks(p(2 * h), p(0)))
        assertEquals(false, PromptCooldown.blocks(p(7 * h), p(0)))
        assertEquals(false, PromptCooldown.blocks(p(1 * h, LatLon(37.42272, 127.09468)), p(0)))
        assertEquals(false, PromptCooldown.blocks(p(1 * h), null))
        assertEquals(false, PromptCooldown.blocks(p(1 * h, src = ShopSource.DEMO), p(0)))
    }
}
