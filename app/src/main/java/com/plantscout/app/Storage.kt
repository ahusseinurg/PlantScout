package com.plantscout.app

import android.content.Context
import org.json.JSONArray
import java.io.File

object Prefs {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    fun apiKey(ctx: Context): String = prefs(ctx).getString("plantnet_key", "") ?: ""
    fun setApiKey(ctx: Context, key: String) = prefs(ctx).edit().putString("plantnet_key", key).apply()
}

object PlantStore {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("plants", Context.MODE_PRIVATE)

    fun imageDir(ctx: Context): File = File(ctx.filesDir, "plants").apply { mkdirs() }

    fun load(ctx: Context): MutableList<PlantRecord> {
        val raw = prefs(ctx).getString("list", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length())
                .map { PlantRecord.fromJson(arr.getJSONObject(it)) }
                .filter { it.candidates.isNotEmpty() && (it.imagePath.isBlank() || File(it.imagePath).exists()) }
                .toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun save(ctx: Context, list: List<PlantRecord>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs(ctx).edit().putString("list", arr.toString()).apply()
    }
}
