package com.zigae.hephaestus.data

import com.zigae.hephaestus.domain.DueLevel
import com.zigae.hephaestus.domain.Interval
import com.zigae.hephaestus.domain.LatLon
import com.zigae.hephaestus.domain.MaintenanceItem
import com.zigae.hephaestus.domain.MaintenanceRecord
import com.zigae.hephaestus.domain.Place
import com.zigae.hephaestus.domain.Powertrain
import com.zigae.hephaestus.domain.ShopSource
import com.zigae.hephaestus.domain.VehicleSignal
import com.zigae.hephaestus.domain.VisitPrompt
import org.junit.Assert.assertEquals
import org.junit.Test

class AppDataCodecTest {
    @Test
    fun roundTrip() {
        val at = LatLon(37.41272, 127.09468)
        val d = AppData(
            mode = Powertrain.EV,
            detectedMode = Powertrain.EV,
            intervalOverrides = mapOf(MaintenanceItem.TIRE to Interval(35_000, 36)),
            records = listOf(MaintenanceRecord("r1", MaintenanceItem.TIRE, 52_340, 1_793_500_000_000, "시흥 카센터")),
            places = listOf(Place("p1", "단골 카센터", at, Place.Kind.MY_SHOP)),
            prompts = listOf(VisitPrompt("v1", "시흥 카센터", at, ShopSource.DESTINATION, 1L, 2L, 52_340, listOf(VehicleSignal.BRAKE_FLUID_LOW))),
            notified = mapOf(MaintenanceItem.BRAKE_FLUID to DueLevel.SOON),
            minDwellMinutes = 0,
            demoOdometerOffsetKm = 52_000,
            lastPrompt = VisitPrompt("v0", "단골 카센터", at, ShopSource.MY_SHOP, 0L, 1L, 50_000),
        )
        assertEquals(d, AppDataCodec.decode(AppDataCodec.encode(d)))
    }

    @Test
    fun emptyAndUnknownValuesAreTolerated() {
        assertEquals(AppData(), AppDataCodec.decode("{}"))
        val d = AppDataCodec.decode("""{"mode":"HYBRID","records":[{"id":"x","item":"GONE","km":1,"time":1}]}""")
        assertEquals(null, d.mode)
        assertEquals(0, d.records.size)
    }
}
