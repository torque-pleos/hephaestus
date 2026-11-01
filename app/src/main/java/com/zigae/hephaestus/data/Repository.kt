package com.zigae.hephaestus.data

import android.content.Context
import android.util.Log
import com.zigae.hephaestus.domain.LatLon
import com.zigae.hephaestus.domain.MaintenanceItem
import com.zigae.hephaestus.domain.MaintenanceRecord
import com.zigae.hephaestus.domain.Place
import com.zigae.hephaestus.domain.Powertrain
import com.zigae.hephaestus.domain.VisitPrompt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

/** 앱 상태 저장소. 변경할 때마다 파일에 바로 쓴다(데이터가 작아 충분). */
class Repository(context: Context) {
    private val file = File(context.filesDir, "hephaestus.json")
    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private fun load(): AppData = runCatching {
        if (file.exists()) AppDataCodec.decode(file.readText()) else AppData()
    }.getOrElse {
        Log.w(TAG, "load failed, starting fresh", it)
        AppData()
    }

    @Synchronized
    fun update(transform: (AppData) -> AppData) {
        val next = transform(_data.value)
        if (next == _data.value) return
        _data.value = next
        runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(AppDataCodec.encode(next))
            tmp.renameTo(file)
        }.onFailure { Log.w(TAG, "save failed", it) }
    }

    fun setMode(mode: Powertrain) = update { it.copy(mode = mode) }

    fun setDetectedMode(mode: Powertrain) = update { it.copy(detectedMode = mode) }

    fun addRecords(items: Collection<MaintenanceItem>, odometerKm: Int, time: Long, shopName: String?) = update { d ->
        val added = items.map { MaintenanceRecord(UUID.randomUUID().toString(), it, odometerKm, time, shopName) }
        d.copy(records = d.records + added, notified = d.notified - items.toSet())
    }

    fun deleteRecord(id: String) = update { d -> d.copy(records = d.records.filterNot { it.id == id }) }

    /** 같은 곳(300m 안)의 답하지 않은 질문은 새 질문으로 바꾼다. */
    fun addPrompt(p: VisitPrompt) = update { d ->
        d.copy(prompts = d.prompts.filterNot { it.at.distanceTo(p.at) <= 300.0 } + p, lastPrompt = p)
    }

    fun dismissPrompt(id: String) = update { d -> d.copy(prompts = d.prompts.filterNot { it.id == id }) }

    fun addPlace(name: String, at: LatLon, kind: Place.Kind) = update { d ->
        // 같은 자리(반경 안)의 기존 등록은 새 등록으로 바꾼다
        val kept = d.places.filterNot { it.at.distanceTo(at) <= 150.0 }
        d.copy(places = kept + Place(UUID.randomUUID().toString(), name, at, kind))
    }

    fun deletePlace(id: String) = update { d -> d.copy(places = d.places.filterNot { it.id == id }) }

    private companion object {
        const val TAG = "Heph/Repo"
    }
}
