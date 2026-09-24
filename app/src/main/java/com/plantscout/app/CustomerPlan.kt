package com.plantscout.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

data class PlanSection(var title: String, var body: String)

/** An editable, customer-facing eradication plan. */
data class CustomerPlan(
    val id: String,
    var title: String,
    var planNumber: String,
    val createdAt: Long,
    var updatedAt: Long,
    var customer: CrmCustomer,
    val sections: MutableList<PlanSection>,
    /** "eradication" or "followup" (after the job is done). */
    var mode: String = MODE_ERADICATION,
    /** History of sends, e.g. "23 Sep 2026 14:05 — maria@example.com". */
    val sentLog: MutableList<String> = mutableListOf()
) {
    fun toJson(): JSONObject {
        val arr = JSONArray()
        sections.forEach { arr.put(JSONObject().put("title", it.title).put("body", it.body)) }
        return JSONObject()
            .put("id", id).put("title", title).put("plan_number", planNumber)
            .put("created_at", createdAt).put("updated_at", updatedAt)
            .put("customer", customer.toJson()).put("sections", arr)
            .put("mode", mode).put("sent_log", JSONArray(sentLog))
    }

    companion object {
        const val MODE_ERADICATION = "eradication"
        const val MODE_FOLLOWUP = "followup"

        fun fromJson(o: JSONObject): CustomerPlan {
            val arr = o.optJSONArray("sections") ?: JSONArray()
            val sections = (0 until arr.length()).map {
                val s = arr.getJSONObject(it)
                PlanSection(s.optString("title"), s.optString("body"))
            }.toMutableList()
            return CustomerPlan(
                id = o.getString("id"),
                title = o.optString("title", "Weed Eradication Plan"),
                planNumber = o.optString("plan_number"),
                createdAt = o.optLong("created_at"),
                updatedAt = o.optLong("updated_at"),
                customer = CrmCustomer.fromJson(o.optJSONObject("customer") ?: JSONObject()),
                sections = sections,
                mode = o.optString("mode", MODE_ERADICATION),
                sentLog = o.optJSONArray("sent_log")?.let { a -> (0 until a.length()).map { a.optString(it) }.toMutableList() }
                    ?: mutableListOf()
            )
        }
    }
}

object CustomerPlanStore {
    private fun dir(ctx: Context) = File(ctx.filesDir, "customer_plans").apply { mkdirs() }

    fun list(ctx: Context): List<CustomerPlan> =
        (dir(ctx).listFiles() ?: emptyArray())
            .filter { it.name.endsWith(".json") }
            .mapNotNull { f -> try { CustomerPlan.fromJson(JSONObject(f.readText())) } catch (e: Exception) { null } }
            .sortedByDescending { it.updatedAt }

    fun load(ctx: Context, id: String): CustomerPlan? = try {
        CustomerPlan.fromJson(JSONObject(File(dir(ctx), "$id.json").readText()))
    } catch (e: Exception) {
        null
    }

    fun save(ctx: Context, plan: CustomerPlan) {
        plan.updatedAt = System.currentTimeMillis()
        val tmp = File(dir(ctx), "${plan.id}.json.tmp")
        tmp.writeText(plan.toJson().toString())
        val dest = File(dir(ctx), "${plan.id}.json")
        if (dest.exists()) dest.delete()
        tmp.renameTo(dest)
    }

    fun delete(ctx: Context, id: String) {
        File(dir(ctx), "$id.json").delete()
    }
}

/** One weed chosen for the plan, plus the AI research for it (if it worked). */
class WeedEntry(val label: String, val candidate: Candidate?) {
    var ai: JSONObject? = null      // response from /recommendation
    var error: String? = null
}

object CustomerPlanBuilder {

    private val money: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)

    fun newPlanNumber(): String =
        "WEP-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())

    private fun goatLabel(key: String) = when (key) {
        "suitable" -> "Suitable for goat grazing"
        "suitable_with_caution" -> "Suitable for goat grazing, with caution"
        "not_suitable" -> "NOT suitable for goat grazing — needs a different removal method"
        else -> "Goat grazing suitability uncertain — confirm before grazing"
    }

    private fun StringBuilder.sub(title: String, text: String?) {
        val t = text?.trim().orEmpty()
        if (t.isNotEmpty()) append(title).append(":\n").append(t).append("\n\n")
    }

    /** Text for one weed's section, from AI research or (fallback) the built-in knowledge base. */
    private fun weedBody(w: WeedEntry, followUp: Boolean): String {
        val f = w.ai?.optJSONObject("fields")
        if (f != null && followUp) {
            return buildString {
                sub("Why it comes back", f.optString("life_cycle").trim() + "\n\n" + f.optString("spread_mechanism").trim())
                sub("Preventing regrowth", f.optString("regrowth_prevention"))
                sub("What you can do", f.optString("customer_actions"))
                sub("What to expect", f.optString("timeline"))
                sub("Safety", f.optString("toxicity_notes"))
            }.trim()
        }
        if (f != null) {
            return buildString {
                sub("Classification", f.optString("classification"))
                sub("Life cycle", f.optString("life_cycle"))
                sub("How it spreads", f.optString("spread_mechanism"))
                sub("Why it's a problem", f.optString("why_problem"))
                sub(
                    "Toxicity and goat safety",
                    f.optString("toxicity_notes").trim() + "\n\n" + goatLabel(f.optString("goat_grazing_suitability"))
                )
                sub("What we will do", f.optString("our_approach"))
                sub("What you can do", f.optString("customer_actions"))
                sub("Timeline", f.optString("timeline"))
                sub("Preventing regrowth", f.optString("regrowth_prevention"))
            }.trim()
        }
        // Fallback: built-in guidance so the plan is still useful.
        val c = w.candidate
        val info = if (c != null) KnowledgeBase.lookup(c).info else PlantInfo(Strategy.GENERIC)
        val pb = Playbooks.forStrategy(info.strategy)
        val goat = if (c != null) GoatGuide.forPlant(c, info) else null
        return buildString {
            append("(Detailed AI research wasn't available for this plant")
            w.error?.let { append(": ").append(it) }
            append(". General guidance is shown — review before sending.)\n\n")
            if (info.hazards.isNotEmpty()) sub("Safety", info.hazards.joinToString("\n") { "${it.label}: ${it.advice}" })
            if (info.note.isNotBlank()) sub("Notes", info.note)
            sub("When to act", pb.timing)
            sub("How to remove it", pb.steps.joinToString("\n") { "• $it" })
            if (goat != null) sub("Goat grazing (${goat.fit.label})", goat.text)
            sub("Stop it coming back", pb.prevent.joinToString("\n") { "• $it" })
            sub("Follow-up", pb.followUp.joinToString("\n") { "• $it" })
        }.trim()
    }

    fun weedTitle(w: WeedEntry): String {
        val f = w.ai?.optJSONObject("fields")
        val common = f?.optString("common_name")?.takeIf { it.isNotBlank() }
        val sci = f?.optString("scientific_name")?.takeIf { it.isNotBlank() }
        return when {
            common != null && sci != null -> "Weed: $common ($sci)"
            common != null -> "Weed: $common"
            else -> "Weed: ${w.label}"
        }
    }

    /** Short facts per weed that are sent to the AI for the whole-site plan. */
    fun overviewInput(weeds: List<WeedEntry>): JSONArray {
        val arr = JSONArray()
        for (w in weeds) {
            val f = w.ai?.optJSONObject("fields")
            val summary = if (f != null) {
                (f.optString("why_problem") + " " + f.optString("toxicity_notes") + " " + f.optString("life_cycle")).take(600)
            } else {
                val c = w.candidate
                if (c != null) {
                    val info = KnowledgeBase.lookup(c).info
                    info.strategy.label + ". " + info.hazards.joinToString { it.label } + " " + info.note
                } else ""
            }
            val goat = f?.optString("goat_grazing_suitability")?.takeIf { it.isNotBlank() }
                ?: w.candidate?.let { c ->
                    when (GoatGuide.forPlant(c, KnowledgeBase.lookup(c).info).fit) {
                        GoatFit.EXCELLENT, GoatFit.GOOD -> "suitable"
                        GoatFit.PARTIAL, GoatFit.CAUTION -> "suitable_with_caution"
                        GoatFit.UNSAFE -> "not_suitable"
                    }
                } ?: "uncertain"
            arr.put(JSONObject().put("name", weedTitle(w).removePrefix("Weed: ")).put("goat_suitability", goat).put("summary", summary))
        }
        return arr
    }

    fun build(
        customer: CrmCustomer,
        weeds: List<WeedEntry>,
        overview: JSONObject?,
        overviewError: String?,
        scans: List<PlantRecord>,
        mode: String = CustomerPlan.MODE_ERADICATION
    ): CustomerPlan {
        val followUp = mode == CustomerPlan.MODE_FOLLOWUP
        val s = mutableListOf<PlanSection>()
        val o = overview?.optJSONObject("sections")
        fun ov(key: String) = o?.optString(key)?.trim().orEmpty()

        s += PlanSection(
            if (followUp) "Your service summary" else "Summary",
            ov("executive_summary").ifBlank {
                (if (followUp) "Thank you for choosing us. Your targeted-grazing service is complete. This plan explains what to expect next and how to keep the weeds from coming back."
                else "This plan covers the weeds identified on your property and how we recommend removing them and keeping them from coming back.") +
                    (overviewError?.let { "\n\n(The AI overview couldn't be generated: $it. Edit this summary before sending.)" } ?: "")
            }
        )

        // What was identified on site (from the phone scans).
        val scanLines = scans.mapNotNull { it.selected }
            .groupBy { it.scientificName.lowercase() }
            .map { (_, cs) ->
                val best = cs.maxByOrNull { it.score } ?: cs.first()
                val times = if (cs.size > 1) " — found ${cs.size} times" else ""
                val how = if (best.isManual) "added manually" else "${(best.score * 100).roundToInt()}% identification match"
                "• ${PlantCatalog.displayLabel(best)} — $how$times"
            }
        val weedLines = weeds.map { "• ${weedTitle(it).removePrefix("Weed: ")}" }
        s += PlanSection(
            "Plants identified on site",
            (if (scanLines.isNotEmpty()) "Plants recorded on site:\n" + scanLines.joinToString("\n") + "\n\n" else "") +
                (if (followUp) "Weeds covered in this follow-up plan:\n" else "Included in this plan:\n") + weedLines.joinToString("\n")
        )

        if (followUp) {
            if (ov("what_to_expect").isNotBlank()) s += PlanSection("What to expect next", ov("what_to_expect"))
            if (ov("regrowth_prevention_plan").isNotBlank()) s += PlanSection("Preventing regrowth", ov("regrowth_prevention_plan"))
        }
        if (ov("site_assessment").isNotBlank()) s += PlanSection("Site assessment", ov("site_assessment"))

        weeds.forEach { s += PlanSection(weedTitle(it), weedBody(it, followUp)) }

        if (ov("treatment_sequence").isNotBlank()) s += PlanSection("Treatment sequence and schedule", ov("treatment_sequence"))
        if (ov("goat_grazing_plan").isNotBlank()) s += PlanSection("Goat grazing plan", ov("goat_grazing_plan"))
        if (ov("safety_precautions").isNotBlank()) s += PlanSection("Safety precautions", ov("safety_precautions"))
        if (ov("customer_guide").isNotBlank()) {
            s += PlanSection(if (followUp) "Your 12-month maintenance calendar" else "Your guide: what you can do", ov("customer_guide"))
        }
        if (ov("monitoring_followup").isNotBlank()) {
            s += PlanSection(if (followUp) "Checking your property and booking your next visit" else "Monitoring and follow-up", ov("monitoring_followup"))
        }

        // Price comes from the business's own rate card, never from the AI.
        val price = overview?.optJSONObject("price")
            ?: weeds.firstNotNullOfOrNull { it.ai?.optJSONObject("price") }
        if (price != null) {
            val low = price.optDouble("low", 0.0)
            val high = price.optDouble("high", 0.0)
            val range = if (high > low) "${money.format(low)} – ${money.format(high)}" else money.format(low)
            s += PlanSection(
                if (followUp) "Estimated cost of a follow-up visit" else "Estimated investment",
                "Estimated range: $range\n\n" + price.optString("basis") +
                    "\n\nThis is a preliminary estimate. A formal quote will confirm the final price."
            )
        }

        // Caveats and sources from the AI research.
        val notes = StringBuilder()
        weeds.forEach { w ->
            val f = w.ai?.optJSONObject("fields") ?: return@forEach
            val c = f.optString("confidence_notes").trim()
            if (c.isNotEmpty()) notes.append(weedTitle(w).removePrefix("Weed: ")).append(":\n").append(c).append("\n\n")
        }
        val sources = LinkedHashMap<String, String>()
        weeds.forEach { w ->
            val arr = w.ai?.optJSONArray("sources") ?: return@forEach
            for (i in 0 until arr.length()) {
                val src = arr.optJSONObject(i) ?: continue
                val url = src.optString("url")
                if (url.isNotBlank()) sources[url] = src.optString("title").ifBlank { url }
            }
        }
        if (sources.isNotEmpty()) {
            notes.append("Sources:\n")
            sources.entries.take(15).forEach { (url, title) -> notes.append("• ").append(title).append(" — ").append(url).append('\n') }
        }
        notes.append("\nThis plan is guidance based on the information available. Local regulations, weather and site conditions can change what's appropriate.")
        s += PlanSection("Notes and limitations", notes.toString().trim())

        val now = System.currentTimeMillis()
        return CustomerPlan(
            id = UUID.randomUUID().toString(),
            title = if (followUp) "Follow-Up & Regrowth Prevention Plan" else "Weed Eradication Plan",
            planNumber = newPlanNumber(),
            createdAt = now,
            updatedAt = now,
            customer = customer,
            sections = s,
            mode = mode
        )
    }
}
