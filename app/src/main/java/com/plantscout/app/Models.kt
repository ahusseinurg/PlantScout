package com.plantscout.app

import org.json.JSONArray
import org.json.JSONObject

data class Candidate(
    val scientificName: String,
    val commonName: String,
    val genus: String,
    val family: String,
    val score: Double
) {
    val displayName: String get() = commonName.ifBlank { scientificName }

    fun toJson(): JSONObject = JSONObject().apply {
        put("sci", scientificName)
        put("common", commonName)
        put("genus", genus)
        put("family", family)
        put("score", score)
    }

    companion object {
        fun fromJson(o: JSONObject) = Candidate(
            o.optString("sci"),
            o.optString("common"),
            o.optString("genus"),
            o.optString("family"),
            o.optDouble("score", 0.0)
        )
    }
}

data class PlantRecord(
    val id: String,
    val imagePath: String,
    val organ: String,
    val candidates: List<Candidate>,
    var selectedIndex: Int = 0
) {
    val selected: Candidate? get() = candidates.getOrNull(selectedIndex)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("image", imagePath)
        put("organ", organ)
        put("selected", selectedIndex)
        val arr = JSONArray()
        candidates.forEach { arr.put(it.toJson()) }
        put("candidates", arr)
    }

    companion object {
        fun fromJson(o: JSONObject): PlantRecord {
            val arr = o.optJSONArray("candidates") ?: JSONArray()
            val list = (0 until arr.length()).map { Candidate.fromJson(arr.getJSONObject(it)) }
            return PlantRecord(
                o.getString("id"),
                o.getString("image"),
                o.optString("organ", "auto"),
                list,
                o.optInt("selected", 0)
            )
        }
    }
}
