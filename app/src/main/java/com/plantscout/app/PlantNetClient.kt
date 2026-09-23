package com.plantscout.app

import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

class PlantNetException(message: String) : Exception(message)

/** Minimal client for the Pl@ntNet identification API (https://my.plantnet.org). */
object PlantNetClient {

    fun identify(apiKey: String, jpeg: ByteArray, organ: String): List<Candidate> {
        val boundary = "----PlantScout" + System.currentTimeMillis()
        val url = URL(
            "https://my-api.plantnet.org/v2/identify/all?include-related-images=false&lang=en&api-key=" +
                URLEncoder.encode(apiKey, "UTF-8")
        )
        val conn = url.openConnection() as HttpsURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            conn.outputStream.use { out ->
                fun w(s: String) = out.write(s.toByteArray(Charsets.UTF_8))
                w("--$boundary\r\n")
                w("Content-Disposition: form-data; name=\"organs\"\r\n\r\n")
                w("$organ\r\n")
                w("--$boundary\r\n")
                w("Content-Disposition: form-data; name=\"images\"; filename=\"plant.jpg\"\r\n")
                w("Content-Type: image/jpeg\r\n\r\n")
                out.write(jpeg)
                w("\r\n--$boundary--\r\n")
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""

            when {
                code == 404 -> throw PlantNetException(
                    "No plant recognised. Try a sharper close-up of a single leaf or flower."
                )
                code == 401 || code == 403 -> throw PlantNetException(
                    "Pl@ntNet rejected the API key. Check it under menu → API key settings."
                )
                code == 429 -> throw PlantNetException(
                    "Daily Pl@ntNet limit reached. Try again tomorrow."
                )
                code !in 200..299 -> throw PlantNetException(
                    "Pl@ntNet error $code: ${body.take(160)}"
                )
            }
            return parse(body)
        } finally {
            conn.disconnect()
        }
    }

    fun parse(body: String): List<Candidate> {
        val results = JSONObject(body).optJSONArray("results")
            ?: throw PlantNetException("No plant recognised in that photo.")
        val out = mutableListOf<Candidate>()
        for (i in 0 until minOf(results.length(), 5)) {
            val r = results.getJSONObject(i)
            val sp = r.optJSONObject("species") ?: continue
            val sci = sp.optString("scientificNameWithoutAuthor").trim()
            if (sci.isEmpty()) continue
            val commons = sp.optJSONArray("commonNames")
            val common = if (commons != null && commons.length() > 0) commons.optString(0) else ""
            val genus = sp.optJSONObject("genus")?.optString("scientificNameWithoutAuthor")
                ?.takeIf { it.isNotBlank() } ?: sci.substringBefore(" ")
            val family = sp.optJSONObject("family")?.optString("scientificNameWithoutAuthor") ?: ""
            out += Candidate(sci, common, genus, family, r.optDouble("score", 0.0))
        }
        if (out.isEmpty()) throw PlantNetException("No plant recognised in that photo.")
        return out
    }
}
