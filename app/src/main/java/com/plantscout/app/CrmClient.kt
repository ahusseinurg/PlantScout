package com.plantscout.app

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class CrmException(message: String) : Exception(message)

/** Company letterhead details, fetched from the CRM and cached on the phone. */
data class Branding(
    val company: String,
    val address: String,
    val phone: String,
    val email: String,
    val website: String,
    val tagline: String,
    val primaryColor: String,
    val logoPath: String?
) {
    fun toJson(): JSONObject = JSONObject()
        .put("company", company).put("address", address).put("phone", phone)
        .put("email", email).put("website", website).put("tagline", tagline)
        .put("primary_color", primaryColor).put("logo_path", logoPath ?: "")

    companion object {
        fun fromJson(o: JSONObject) = Branding(
            o.optString("company"), o.optString("address"), o.optString("phone"),
            o.optString("email"), o.optString("website"), o.optString("tagline"),
            o.optString("primary_color", "#28734b"),
            o.optString("logo_path").ifBlank { null }
        )
    }
}

/** A weed-control customer/job from the CRM (or entered by hand). */
data class CrmCustomer(
    var jobId: Long = 0,
    var jobNumber: String = "",
    var status: String = "",
    var name: String = "",
    var company: String = "",
    var email: String = "",
    var phone: String = "",
    var address: String = "",
    var acres: Double = 0.0,
    var goatsNeeded: Int = 0,
    var brushDensity: String = "",
    var terrain: String = "",
    var water: String = "",
    var fence: String = "",
    var siteNotes: String = "",
    var customerNotes: String = "",
    var completed: Boolean = false,
    var completedAt: String = "",
    var previousWeeds: List<String> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("job_id", jobId).put("job_number", jobNumber).put("status", status)
        .put("customer_name", name).put("company", company).put("email", email)
        .put("phone", phone).put("service_address", address).put("acres", acres)
        .put("goats_needed", goatsNeeded).put("brush_density", brushDensity)
        .put("terrain_level", terrain).put("water_access", water).put("fence_status", fence)
        .put("site_notes", siteNotes).put("customer_notes", customerNotes)
        .put("completed", completed).put("completed_at", completedAt)
        .put("previous_weeds", JSONArray(previousWeeds))

    companion object {
        fun fromJson(o: JSONObject) = CrmCustomer(
            jobId = o.optLong("job_id"),
            jobNumber = o.optString("job_number"),
            status = o.optString("status"),
            name = o.optString("customer_name"),
            company = o.optString("company"),
            email = o.optString("email"),
            phone = o.optString("phone"),
            address = o.optString("service_address"),
            acres = o.optDouble("acres", 0.0),
            goatsNeeded = o.optInt("goats_needed"),
            brushDensity = o.optString("brush_density"),
            terrain = o.optString("terrain_level"),
            water = o.optString("water_access"),
            fence = o.optString("fence_status"),
            siteNotes = o.optString("site_notes"),
            customerNotes = o.optString("customer_notes"),
            completed = o.optBoolean("completed"),
            completedAt = o.optString("completed_at"),
            previousWeeds = o.optJSONArray("previous_weeds")?.let { a ->
                (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() }
            } ?: emptyList()
        )
    }
}

object CrmPrefs {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("crm", Context.MODE_PRIVATE)

    fun url(ctx: Context): String = prefs(ctx).getString("url", "") ?: ""
    fun token(ctx: Context): String = prefs(ctx).getString("token", "") ?: ""
    fun isConfigured(ctx: Context) = url(ctx).isNotBlank() && token(ctx).isNotBlank()

    fun save(ctx: Context, url: String, token: String) {
        prefs(ctx).edit().putString("url", url).putString("token", token).apply()
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().clear().apply()
        File(ctx.filesDir, "branding_logo").delete()
    }

    fun branding(ctx: Context): Branding? {
        val raw = prefs(ctx).getString("branding", null) ?: return null
        return try {
            Branding.fromJson(JSONObject(raw))
        } catch (e: Exception) {
            null
        }
    }

    fun saveBranding(ctx: Context, b: Branding) {
        prefs(ctx).edit().putString("branding", b.toJson().toString()).apply()
    }
}

/** Talks to the "PlantScout Connector for URG" WordPress plugin. */
object CrmClient {

    /**
     * Turns whatever was typed or pasted into the site's base address:
     * removes spaces (phone keyboards add them after dots), adds https://,
     * and drops anything after the address such as /wp-admin or ?rest_route=...
     */
    fun normalizeUrl(input: String): String {
        var u = input.replace(Regex("\\s+"), "").trim()
        if (u.isEmpty()) return u
        if (!u.startsWith("http://", true) && !u.startsWith("https://", true)) u = "https://$u"
        u = u.substringBefore('?').substringBefore('#')
        for (marker in listOf("/wp-admin", "/wp-login", "/wp-json", "/dashboard")) {
            val i = u.indexOf(marker, ignoreCase = true)
            if (i > 0) u = u.substring(0, i)
        }
        return u.trimEnd('/')
    }

    private fun endpoint(base: String, route: String, query: Map<String, String> = emptyMap()): URL {
        // ?rest_route= works on every WordPress site, with or without pretty permalinks.
        val sb = StringBuilder(normalizeUrl(base))
            .append("/?rest_route=")
            .append(URLEncoder.encode("/plantscout/v1$route", "UTF-8"))
        for ((k, v) in query) {
            sb.append('&').append(k).append('=').append(URLEncoder.encode(v, "UTF-8"))
        }
        return URL(sb.toString())
    }

    private fun request(
        base: String,
        token: String,
        route: String,
        method: String = "GET",
        body: JSONObject? = null,
        query: Map<String, String> = emptyMap(),
        readTimeoutMs: Int = 30_000
    ): JSONObject {
        if (base.isBlank() || token.isBlank()) throw CrmException("Connect to your CRM first (menu → Company connection).")
        val conn = endpoint(base, route, query).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 20_000
            conn.readTimeout = readTimeoutMs
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("X-PlantScout-Token", token)
            conn.setRequestProperty("User-Agent", "PlantScout-Android")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = try {
                JSONObject(text.trim().let { t -> t.substring(maxOf(0, t.indexOf('{'))) })
            } catch (e: Exception) {
                null
            }
            if (code in 200..299 && json != null) return json

            val serverMsg = json?.optString("message").orEmpty()
            val serverCode = json?.optString("code").orEmpty()
            throw CrmException(
                when {
                    serverCode == "rest_no_route" ->
                        "The PlantScout Connector plugin isn't active on that website. Install and activate it in WordPress."
                    code == 401 || code == 403 ->
                        serverMsg.ifBlank { "The CRM rejected the app token. Create a new one in WordPress → Settings → PlantScout App." }
                    serverMsg.isNotBlank() -> serverMsg
                    json == null && code in 200..299 ->
                        "That website didn't return app data. Check the website address."
                    else -> "CRM error $code. Check the website address and try again."
                }
            )
        } catch (e: CrmException) {
            throw e
        } catch (e: java.net.UnknownHostException) {
            val host = try { URL(normalizeUrl(base)).host } catch (x: Exception) { base }
            throw CrmException("Can't find the website \"$host\". Check the spelling (no spaces) and that the phone has internet.")
        } catch (e: javax.net.ssl.SSLException) {
            throw CrmException("Secure connection to ${normalizeUrl(base)} failed. If your site has no https certificate, ask your web host to add one.")
        } catch (e: java.net.SocketTimeoutException) {
            throw CrmException("The CRM took too long to respond. Try again.")
        } catch (e: IOException) {
            throw CrmException("Connection problem: ${e.message}")
        } finally {
            conn.disconnect()
        }
    }

    fun ping(base: String, token: String): JSONObject = request(base, token, "/ping")

    /** Downloads the letterhead and logo and caches them. */
    fun refreshBranding(ctx: Context): Branding {
        val o = request(CrmPrefs.url(ctx), CrmPrefs.token(ctx), "/branding")
        var logoPath: String? = null
        val logo = o.optJSONObject("logo")
        if (logo != null) {
            val b64 = logo.optString("base64")
            if (b64.isNotBlank()) {
                val f = File(ctx.filesDir, "branding_logo")
                f.writeBytes(Base64.decode(b64, Base64.DEFAULT))
                logoPath = f.absolutePath
            }
        }
        val b = Branding(
            company = o.optString("company"),
            address = o.optString("address"),
            phone = o.optString("phone"),
            email = o.optString("email"),
            website = o.optString("website"),
            tagline = o.optString("tagline"),
            primaryColor = o.optString("primary_color", "#28734b"),
            logoPath = logoPath
        )
        CrmPrefs.saveBranding(ctx, b)
        return b
    }

    /** [filter] is "all", "completed" (job done) or "active". */
    fun customers(ctx: Context, search: String, filter: String = "all"): List<CrmCustomer> {
        val o = request(CrmPrefs.url(ctx), CrmPrefs.token(ctx), "/customers", query = mapOf("search" to search, "limit" to "60", "filter" to filter))
        val arr = o.optJSONArray("customers") ?: JSONArray()
        return (0 until arr.length()).map { CrmCustomer.fromJson(arr.getJSONObject(it)) }
    }

    /** AI research + recommendation for one weed (a few minutes at most). */
    fun recommendation(ctx: Context, jobId: Long, weedName: String, serviceAddress: String = ""): JSONObject =
        request(
            CrmPrefs.url(ctx), CrmPrefs.token(ctx), "/recommendation", "POST",
            JSONObject().put("job_id", jobId).put("weed_name", weedName).put("service_address", serviceAddress),
            readTimeoutMs = 240_000
        )

    /** AI whole-site plan tying all weeds together. */
    fun overview(ctx: Context, jobId: Long, weeds: JSONArray, staffNotes: String, mode: String, serviceAddress: String = ""): JSONObject =
        request(
            CrmPrefs.url(ctx), CrmPrefs.token(ctx), "/overview", "POST",
            JSONObject().put("job_id", jobId).put("weeds", weeds).put("staff_notes", staffNotes).put("mode", mode)
                .put("service_address", serviceAddress),
            readTimeoutMs = 240_000
        )

    /** Emails the PDF from the business through the CRM, and logs it on the job. Returns who it went to. */
    fun sendPlan(
        ctx: Context,
        jobId: Long,
        to: List<String>,
        copyBusiness: Boolean,
        subject: String,
        message: String,
        pdf: File
    ): List<String> {
        val body = JSONObject()
            .put("job_id", jobId)
            .put("to", JSONArray(to))
            .put("copy_business", copyBusiness)
            .put("subject", subject)
            .put("message", message)
            .put("filename", pdf.name)
            .put("pdf_base64", Base64.encodeToString(pdf.readBytes(), Base64.NO_WRAP))
        val o = request(CrmPrefs.url(ctx), CrmPrefs.token(ctx), "/send-plan", "POST", body, readTimeoutMs = 90_000)
        val arr = o.optJSONArray("to") ?: JSONArray()
        return (0 until arr.length()).map { arr.optString(it) }
    }
}
