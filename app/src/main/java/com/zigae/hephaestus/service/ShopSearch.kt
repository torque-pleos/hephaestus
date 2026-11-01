package com.zigae.hephaestus.service

import android.util.Log
import com.zigae.hephaestus.BuildConfig
import com.zigae.hephaestus.domain.LatLon
import com.zigae.hephaestus.domain.ShopHit
import com.zigae.hephaestus.domain.ShopMatcher
import com.zigae.hephaestus.domain.ShopSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 지도 검색으로 현재 위치가 정비소인지 확인한다.
 * 카카오 로컬 키가 있으면 카카오(국내 정비소 데이터가 풍부), 없으면 OSM Overpass(키 불필요, 국내 데이터는 적음).
 * 공모전 데모 범위라 클라이언트에서 직접 호출한다(상용 시 서버 프록시로 키 보호).
 */
class ShopSearch(private val kakaoKey: String = BuildConfig.KAKAO_REST_KEY) {

    suspend fun find(at: LatLon): ShopHit? = withContext(Dispatchers.IO) {
        runCatching { if (kakaoKey.isNotBlank()) kakao(at) else overpass(at) }
            .onFailure { Log.w(TAG, "map search failed: ${it.message}") }
            .getOrNull()
    }

    private fun kakao(at: LatLon): ShopHit? {
        for (query in listOf("자동차정비", "카센터", "타이어")) {
            val url = "https://dapi.kakao.com/v2/local/search/keyword.json?query=${enc(query)}" +
                "&x=${at.lon}&y=${at.lat}&radius=$RADIUS_M&sort=distance&size=5"
            val body = get(url, mapOf("Authorization" to "KakaoAK $kakaoKey")) ?: continue
            val docs = JSONObject(body).optJSONArray("documents") ?: continue
            for (i in 0 until docs.length()) {
                val d = docs.getJSONObject(i)
                val name = d.optString("place_name")
                val category = d.optString("category_name")
                if (category.contains("정비") || category.contains("타이어") || ShopMatcher.isShopName(name)) {
                    return ShopHit(name, LatLon(d.getString("y").toDouble(), d.getString("x").toDouble()), ShopSource.MAP_SEARCH)
                }
            }
        }
        return null
    }

    private fun overpass(at: LatLon): ShopHit? {
        val q = "[out:json][timeout:8];nwr(around:$RADIUS_M,${at.lat},${at.lon})[\"shop\"~\"^(car_repair|tyres)$\"];out center tags 3;"
        val body = get("https://overpass-api.de/api/interpreter?data=${enc(q)}", emptyMap()) ?: return null
        val els = JSONObject(body).optJSONArray("elements") ?: return null
        for (i in 0 until els.length()) {
            val e = els.getJSONObject(i)
            val name = e.optJSONObject("tags")?.optString("name").orEmpty().ifBlank { "정비소" }
            val lat = if (e.has("lat")) e.getDouble("lat") else e.getJSONObject("center").getDouble("lat")
            val lon = if (e.has("lon")) e.getDouble("lon") else e.getJSONObject("center").getDouble("lon")
            return ShopHit(name, LatLon(lat, lon), ShopSource.MAP_SEARCH)
        }
        return null
    }

    private fun get(url: String, headers: Map<String, String>): String? {
        val c = URL(url).openConnection() as HttpURLConnection
        return try {
            c.connectTimeout = 8_000
            c.readTimeout = 10_000
            c.setRequestProperty("User-Agent", "Hephaestus/0.1 (Pleos contest app)")
            headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
            if (c.responseCode !in 200..299) {
                Log.w(TAG, "HTTP ${c.responseCode} for ${url.substringBefore('?')}")
                null
            } else c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private companion object {
        const val TAG = "Heph/ShopSearch"
        const val RADIUS_M = 100
    }
}
