package com.plantscout.app

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Checks the GitHub repository's latest release for a newer APK. */
object UpdateChecker {

    /** Your GitHub "owner/repository". Change this if you rename or move the repo. */
    const val REPO = "ahusseinurg/PlantScout"

    data class Release(
        val versionCode: Long,
        val name: String,
        val apkUrl: String,
        val pageUrl: String
    )

    fun currentVersionCode(ctx: Context): Long =
        PackageInfoCompat.getLongVersionCode(ctx.packageManager.getPackageInfo(ctx.packageName, 0))

    fun currentVersionName(ctx: Context): String =
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"

    /** Returns the latest release, or null if there isn't one with an APK. Runs network I/O: call off the main thread. */
    fun fetchLatest(): Release? {
        val conn = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpsURLConnection
        try {
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "PlantScout-Android")
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            val code = conn.responseCode
            if (code == 404) return null
            if (code !in 200..299) throw IOException("GitHub returned $code")
            val body = conn.inputStream.bufferedReader().use { it.readText() }

            val json = JSONObject(body)
            val tag = json.optString("tag_name")
            // Tags look like "v1.7" (or "build-7" from older builds): the trailing number is the build number.
            val number = Regex("(\\d+)$").find(tag)?.groupValues?.get(1)?.toLongOrNull() ?: return null

            val assets = json.optJSONArray("assets") ?: return null
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = a.optString("browser_download_url")
                    break
                }
            }
            if (apkUrl.isNullOrBlank()) return null

            return Release(
                versionCode = number,
                name = json.optString("name").ifBlank { tag },
                apkUrl = apkUrl,
                pageUrl = json.optString("html_url")
            )
        } finally {
            conn.disconnect()
        }
    }

    /** Downloads the APK to [dest], reporting progress 0–100 (or -1 if the size is unknown). */
    fun download(url: String, dest: File, onProgress: (Int) -> Unit) {
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, dest.name + ".part")
        val conn = URL(url).openConnection() as HttpsURLConnection
        try {
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "PlantScout-Android")
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("Download failed ($code)")
            val total = conn.contentLengthLong
            var read = 0L
            var lastPct = -2
            conn.inputStream.use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        val pct = if (total > 0) ((read * 100) / total).toInt() else -1
                        if (pct != lastPct) {
                            lastPct = pct
                            onProgress(pct)
                        }
                    }
                }
            }
            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) throw IOException("Couldn't save the update")
        } finally {
            conn.disconnect()
            if (tmp.exists()) tmp.delete()
        }
    }
}
