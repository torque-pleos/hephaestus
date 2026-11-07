package com.zigae.hephaestus.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLon(val lat: Double, val lon: Double) {
    fun distanceTo(other: LatLon): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(other.lat - lat)
        val dLon = Math.toRadians(other.lon - lon)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}

/** 사용자가 등록한 장소. 정비소(MY_SHOP) 또는 "정비소 아님"(IGNORED). */
data class Place(val id: String, val name: String, val at: LatLon, val kind: Kind) {
    enum class Kind { MY_SHOP, IGNORED }
}

/** 정비소로 판단한 근거. */
enum class ShopSource(val label: String) {
    MY_SHOP("내 정비소"),
    DESTINATION("내비 목적지"),
    MAP_SEARCH("지도 검색"),
    DEMO("데모"),
    /** 정비소 판별 없이 차량 신호(레벨·경고등 꺼짐)만으로 만든 질문. */
    SIGNAL("차량 신호"),
}

data class ShopHit(val name: String, val at: LatLon, val source: ShopSource)

/** 장소 이름으로 정비소 여부를 판단한다. */
object ShopMatcher {
    private val positive = listOf(
        "카센터", "정비", "공업사", "블루핸즈", "오토큐", "스피드메이트", "오토오아시스", "오토서비스",
        "자동차서비스", "자동차 서비스", "서비스센터", "타이어", "티스테이션", "T스테이션", "카닥", "모터스",
        "오일교환", "오일 교환", "경정비", "판금", "덴트",
    )

    // 이름에 "오일"·"타이어"가 들어가도 정비소가 아닌 곳
    private val negative = listOf("주유소", "S-OIL", "에쓰오일", "충전소", "충전기", "세차", "주차장", "매매단지", "중고차")

    fun isShopName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val n = name.replace(" ", "")
        if (negative.any { n.contains(it.replace(" ", ""), ignoreCase = true) }) return false
        return positive.any { n.contains(it.replace(" ", ""), ignoreCase = true) }
    }
}

/**
 * 오프라인 판별: 등록 장소 → 내비 목적지 순서. 지도 검색은 이 결과가 null일 때 호출자가 따로 한다.
 * @return Pair(판별 결과, 무시 장소 여부). 무시 장소면 지도 검색도 하지 않는다.
 */
object ShopRules {
    const val PLACE_RADIUS_M = 150.0
    const val DESTINATION_RADIUS_M = 300.0

    data class Result(val hit: ShopHit?, val ignored: Boolean)

    fun resolve(here: LatLon, places: List<Place>, destinationName: String?, destinationAt: LatLon?): Result {
        val nearest = places.minByOrNull { it.at.distanceTo(here) }
        if (nearest != null && nearest.at.distanceTo(here) <= PLACE_RADIUS_M) {
            return when (nearest.kind) {
                Place.Kind.MY_SHOP -> Result(ShopHit(nearest.name, nearest.at, ShopSource.MY_SHOP), false)
                Place.Kind.IGNORED -> Result(null, true)
            }
        }
        if (destinationAt != null && destinationAt.distanceTo(here) <= DESTINATION_RADIUS_M && ShopMatcher.isShopName(destinationName)) {
            return Result(ShopHit(destinationName!!, destinationAt, ShopSource.DESTINATION), false)
        }
        return Result(null, false)
    }
}
