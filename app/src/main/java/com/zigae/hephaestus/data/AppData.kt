package com.zigae.hephaestus.data

import com.zigae.hephaestus.domain.DueLevel
import com.zigae.hephaestus.domain.Interval
import com.zigae.hephaestus.domain.LatLon
import com.zigae.hephaestus.domain.MaintenanceItem
import com.zigae.hephaestus.domain.MaintenanceRecord
import com.zigae.hephaestus.domain.Place
import com.zigae.hephaestus.domain.Powertrain
import com.zigae.hephaestus.domain.ShopSource
import com.zigae.hephaestus.domain.VisitPrompt
import org.json.JSONArray
import org.json.JSONObject

/** 앱 저장 상태 전체. 한 파일(JSON)로 저장한다. */
data class AppData(
    val mode: Powertrain? = null,
    /** 차량에서 추정한 모드 (사용자 확인 전 기본값으로 제안). */
    val detectedMode: Powertrain? = null,
    val intervalOverrides: Map<MaintenanceItem, Interval> = emptyMap(),
    val records: List<MaintenanceRecord> = emptyList(),
    val places: List<Place> = emptyList(),
    val prompts: List<VisitPrompt> = emptyList(),
    /** 항목별로 마지막으로 알린 단계. 같은 단계는 다시 알리지 않는다(새 기록이 생기면 초기화). */
    val notified: Map<MaintenanceItem, DueLevel> = emptyMap(),
    val minDwellMinutes: Int = 20,
    /** 데모용 주행거리 보정(km). 에뮬레이터 주행거리는 0에서 움직이지 않는다. */
    val demoOdometerOffsetKm: Int = 0,
) {
    fun interval(item: MaintenanceItem, mode: Powertrain): Interval? =
        intervalOverrides[item] ?: item.defaultInterval(mode)
}

object AppDataCodec {
    fun encode(d: AppData): String = JSONObject().apply {
        put("version", 1)
        d.mode?.let { put("mode", it.name) }
        d.detectedMode?.let { put("detectedMode", it.name) }
        put("intervals", JSONObject().apply {
            d.intervalOverrides.forEach { (item, iv) -> put(item.name, JSONObject().put("km", iv.km).put("months", iv.months)) }
        })
        put("records", JSONArray().apply {
            d.records.forEach {
                put(JSONObject().put("id", it.id).put("item", it.item.name).put("km", it.odometerKm).put("time", it.timeMillis)
                    .apply { it.shopName?.let { s -> put("shop", s) } })
            }
        })
        put("places", JSONArray().apply {
            d.places.forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("lat", it.at.lat).put("lon", it.at.lon).put("kind", it.kind.name))
            }
        })
        put("prompts", JSONArray().apply {
            d.prompts.forEach {
                put(JSONObject().put("id", it.id).put("shop", it.shopName).put("lat", it.at.lat).put("lon", it.at.lon)
                    .put("source", it.source.name).put("arrivedAt", it.arrivedAt).put("createdAt", it.createdAt).put("km", it.odometerKm))
            }
        })
        put("notified", JSONObject().apply { d.notified.forEach { (k, v) -> put(k.name, v.name) } })
        put("minDwellMinutes", d.minDwellMinutes)
        put("demoOdometerOffsetKm", d.demoOdometerOffsetKm)
    }.toString()

    fun decode(text: String): AppData {
        val o = JSONObject(text)
        return AppData(
            mode = o.optString("mode").toEnum<Powertrain>(),
            detectedMode = o.optString("detectedMode").toEnum<Powertrain>(),
            intervalOverrides = o.optJSONObject("intervals")?.let { iv ->
                iv.keys().asSequence().mapNotNull { key ->
                    val item = MaintenanceItem.fromKey(key) ?: return@mapNotNull null
                    val v = iv.getJSONObject(key)
                    runCatching { item to Interval(v.getInt("km"), v.getInt("months")) }.getOrNull()
                }.toMap()
            } ?: emptyMap(),
            records = o.optJSONArray("records").objects().mapNotNull {
                val item = MaintenanceItem.fromKey(it.getString("item")) ?: return@mapNotNull null
                MaintenanceRecord(it.getString("id"), item, it.getInt("km"), it.getLong("time"), it.optString("shop").ifBlank { null })
            },
            places = o.optJSONArray("places").objects().mapNotNull {
                val kind = it.getString("kind").toEnum<Place.Kind>() ?: return@mapNotNull null
                Place(it.getString("id"), it.getString("name"), LatLon(it.getDouble("lat"), it.getDouble("lon")), kind)
            },
            prompts = o.optJSONArray("prompts").objects().map {
                VisitPrompt(
                    it.getString("id"), it.getString("shop"), LatLon(it.getDouble("lat"), it.getDouble("lon")),
                    it.getString("source").toEnum<ShopSource>() ?: ShopSource.DEMO,
                    it.getLong("arrivedAt"), it.getLong("createdAt"), it.getInt("km"),
                )
            },
            notified = o.optJSONObject("notified")?.let { n ->
                n.keys().asSequence().mapNotNull { key ->
                    val item = MaintenanceItem.fromKey(key) ?: return@mapNotNull null
                    val level = n.getString(key).toEnum<DueLevel>() ?: return@mapNotNull null
                    item to level
                }.toMap()
            } ?: emptyMap(),
            minDwellMinutes = o.optInt("minDwellMinutes", 20),
            demoOdometerOffsetKm = o.optInt("demoOdometerOffsetKm", 0),
        )
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(): E? =
        if (this.isNullOrBlank()) null else enumValues<E>().firstOrNull { it.name == this }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }
}
