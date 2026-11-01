package com.zigae.hephaestus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopsTest {
    @Test
    fun matchesShopNames() {
        listOf("시흥 카센터", "블루핸즈 성남점", "기아 오토큐 분당", "스피드메이트 수정점", "타이어뱅크 성남", "OO자동차공업사", "현대자동차 서비스센터")
            .forEach { assertTrue(it, ShopMatcher.isShopName(it)) }
    }

    @Test
    fun rejectsLookalikes() {
        listOf("S-OIL 시흥주유소", "GS칼텍스 주유소", "이피트 충전소", "손세차장", "부산신항", "", null)
            .forEach { assertFalse(it.toString(), ShopMatcher.isShopName(it)) }
    }

    private val here = LatLon(37.41272, 127.09468)
    private val near = LatLon(37.41300, 127.09468) // 약 31m
    private val far = LatLon(37.42272, 127.09468) // 약 1.1km

    @Test
    fun myShopWins() {
        val r = ShopRules.resolve(here, listOf(Place("p", "단골 카센터", near, Place.Kind.MY_SHOP)), "부산신항", far)
        assertEquals(ShopSource.MY_SHOP, r.hit!!.source)
    }

    @Test
    fun ignoredPlaceStopsSearch() {
        val r = ShopRules.resolve(here, listOf(Place("p", "회사", near, Place.Kind.IGNORED)), "시흥 카센터", near)
        assertNull(r.hit)
        assertTrue(r.ignored)
    }

    @Test
    fun destinationNearbyWithShopName() {
        assertEquals(ShopSource.DESTINATION, ShopRules.resolve(here, emptyList(), "시흥 카센터", near).hit!!.source)
        assertNull(ShopRules.resolve(here, emptyList(), "시흥 카센터", far).hit)
        assertNull(ShopRules.resolve(here, emptyList(), "부산신항", near).hit)
    }

    @Test
    fun distance() {
        assertEquals(1112.0, here.distanceTo(far), 5.0)
    }
}
