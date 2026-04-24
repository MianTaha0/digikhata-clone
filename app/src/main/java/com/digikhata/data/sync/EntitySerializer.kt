package com.digikhata.data.sync

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Phase 3b.2: turns entity data classes into JSON strings for the sync queue,
 * and back into Map<String, Any?> payloads when pushing to Firestore.
 *
 * Gson handles plain Kotlin data classes with primitive + nullable fields
 * (which all of our Room entities happen to be) without extra config.
 */
object EntitySerializer {
    private val gson: Gson = Gson()
    private val mapType = object : TypeToken<Map<String, Any?>>() {}.type

    fun toJson(payload: Any): String = gson.toJson(payload)

    fun toMap(json: String): Map<String, Any?> {
        if (json.isBlank()) return emptyMap()
        return gson.fromJson<Map<String, Any?>>(json, mapType) ?: emptyMap()
    }

    fun toMap(payload: Any): Map<String, Any?> = toMap(toJson(payload))
}
