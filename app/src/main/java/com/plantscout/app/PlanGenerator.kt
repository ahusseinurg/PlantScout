package com.plantscout.app

import com.plantscout.app.Strategy.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Builds a combined eradication plan for all scanned plants.
 * Output is plain text; lines starting with "#", "##", "###" are headings.
 */
object PlanGenerator {

    private data class Group(
        val candidate: Candidate,
        val count: Int,
        val score: Double,
        val match: KnowledgeBase.Match
    ) {
        val info: PlantInfo get() = match.info
    }

    private fun StringBuilder.line(s: String = "") {
        append(s).append('\n')
    }

    private fun pct(score: Double) = (score * 100).roundToInt()

    private fun priority(g: Group): Int {
        val h = g.info.hazards
        var p = 0
        if (Hazard.NOXIOUS in h) p += 3
        if (Hazard.DEADLY in h) p += 3
        if (Hazard.PHOTOTOXIC in h || Hazard.TOXIC in h || Hazard.URUSHIOL in h) p += 2
        when (g.info.strategy) {
            ANNUAL_BROADLEAF, ANNUAL_GRASS, BIENNIAL -> p += 2
            KNOTWEED, AQUATIC -> p += 2
            CREEPING, BULB, VINE -> p += 1
            else -> {}
        }
        if (g.score < 0.3) p -= 4
        return p
    }

    private fun reasons(g: Group): String {
        val h = g.info.hazards
        val r = mutableListOf<String>()
        if (g.score < 0.3) r += "confirm the ID first"
        if (Hazard.NOXIOUS in h) r += "often legally required to control"
        if (Hazard.DEADLY in h || Hazard.TOXIC in h || Hazard.PHOTOTOXIC in h || Hazard.URUSHIOL in h) r += "safety risk to people or pets"
        when (g.info.strategy) {
            ANNUAL_BROADLEAF, ANNUAL_GRASS, BIENNIAL -> r += "stop it before it seeds"
            CREEPING, BULB, KNOTWEED -> r += "spreads underground, so start early"
            VINE -> r += "smothers other plants"
            AQUATIC -> r += "spreads through water; check permits"
            else -> {}
        }
        if (r.isEmpty()) r += "lower urgency"
        return r.joinToString("; ").replaceFirstChar { it.uppercase() }
    }

    private fun levelText(level: String) = when (level) {
        "species" -> "species-specific advice"
        "genus" -> "based on its genus"
        "family" -> "rough guess from its plant family"
        else -> "general advice"
    }

    private fun section(sb: StringBuilder, title: String, items: List<String>) {
        if (items.isEmpty()) return
        sb.line("### $title")
        items.forEach { sb.line("• $it") }
    }

    fun generate(plants: List<PlantRecord>, now: Date = Date()): String {
        val selected = plants.mapNotNull { it.selected }
        if (selected.isEmpty()) {
            return "# No plants scanned\nScan one or more plants, then generate a plan."
        }

        val groups = selected
            .groupBy { it.scientificName.lowercase() }
            .map { (_, cs) ->
                val best = cs.maxByOrNull { it.score } ?: cs.first()
                Group(best, cs.size, best.score, KnowledgeBase.lookup(best))
            }
            .sortedByDescending { priority(it) }

        val sb = StringBuilder()
        val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(now)

        sb.line("# Plant eradication plan")
        sb.line("$date · ${selected.size} scan(s) · ${groups.size} species")
        sb.line()

        // ---- Safety first
        sb.line("## Before you start")
        val lowConf = groups.filter { it.score < 0.5 }
        if (lowConf.isNotEmpty()) {
            sb.line("• Double-check the ID of: ${lowConf.joinToString { it.candidate.displayName }}. The match was uncertain — rescan a close-up of a flower or leaf, or ask your county extension office. Acting on a wrong ID can kill a plant you want or expose you to a toxic one.")
        }
        val hazards = groups.flatMap { it.info.hazards }.toSortedSet()
        for (h in hazards) {
            val who = groups.filter { h in it.info.hazards }.joinToString { it.candidate.displayName }
            sb.line("• ⚠ ${h.label} ($who): ${h.advice}")
        }
        sb.line("• Mechanical methods come first. Herbicides are listed as a last resort: always read and follow the product label (it's legally binding), wear the protective gear it lists, don't spray in wind, heat or before rain, keep it out of water unless it's labeled for aquatic use, and keep children and pets off treated areas until dry.")
        sb.line("• Don't compost or mulch with plants treated with clopyralid or aminopyralid — they persist and damage vegetables and flowers.")
        sb.line("• Before digging out large roots or stumps, have buried utility lines marked (call 811 in the US).")
        sb.line()

        // ---- Priority order
        sb.line("## Priority order")
        groups.forEachIndexed { i, g ->
            val times = if (g.count > 1) " ×${g.count}" else ""
            sb.line("${i + 1}. ${g.candidate.displayName}$times — ${g.info.strategy.label}. ${reasons(g)}.")
        }

        // ---- Per-plant plans
        groups.forEachIndexed { i, g ->
            val c = g.candidate
            val pb = Playbooks.forStrategy(g.info.strategy)
            sb.line()
            sb.line("## ${i + 1}. ${c.displayName}")
            val family = if (c.family.isNotBlank()) " · ${c.family}" else ""
            sb.line("${c.scientificName}$family")
            val seen = if (g.count > 1) " · scanned ${g.count}×" else ""
            sb.line("Match ${pct(g.score)}% · ${g.info.strategy.label} · ${levelText(g.match.level)}$seen")
            if (g.info.hazards.isNotEmpty()) {
                sb.line("⚠ ${g.info.hazards.joinToString { it.label }} — see safety notes above.")
            }
            if (g.info.note.isNotBlank()) sb.line("Note: ${g.info.note}")
            if (g.match.level == "none" || g.match.level == "family") {
                sb.line("Is it really a weed? This plant isn't in the app's weed list. Check whether it's native, protected or deliberately planted before removing it.")
            }
            section(sb, "When to act", listOf(pb.timing))
            section(sb, "How to remove it", pb.steps)
            section(sb, "Stop it coming back", pb.prevent)
            section(sb, "Herbicide options (last resort)", pb.chemical)
            section(sb, "Disposal", pb.disposal)
            section(sb, "Follow-up", pb.followUp)
        }

        // ---- Supplies
        sb.line()
        sb.line("## Supplies checklist")
        val supplies = (groups.flatMap { Playbooks.forStrategy(it.info.strategy).supplies } +
            hazards.flatMap { it.supplies }).distinct()
        supplies.forEach { sb.line("☐ $it") }

        // ---- Calendar
        val strategies = groups.map { it.info.strategy }.toSet()
        sb.line()
        sb.line("## Follow-up calendar")
        sb.line("• This week: start on items 1–${minOf(3, groups.size)} of the priority list, and remove any flowers or seed heads on everything else.")
        sb.line("• In 2 weeks: walk every treated spot; pull seedlings and resprouts while they're small.")
        sb.line("• In 4–6 weeks: re-cut or re-dig resprouts; check that any smothering covers are still in place.")
        if (strategies.any { it in setOf(CREEPING, TAPROOT, WOODY, VINE, TREE, KNOTWEED) }) {
            sb.line("• Late summer to early fall: best window for treating perennials, shrubs, vines and trees, as they pull energy down into their roots.")
        }
        if (strategies.any { it in setOf(ANNUAL_BROADLEAF, ANNUAL_GRASS, BIENNIAL) }) {
            sb.line("• Early next spring: sweep for seedlings and rosettes; mulch or apply pre-emergent before they germinate.")
        }
        if (strategies.any { it in setOf(KNOTWEED, CREEPING, BULB) }) {
            sb.line("• Monthly for the next 2–3 years: check for regrowth from roots, runners or tubers.")
        }
        sb.line("• Next year: rescan the area with PlantScout to catch newcomers and confirm what's gone.")
        sb.line()
        sb.line("This plan is general guidance. Rules, climate and site conditions vary — your county extension office or weed department can give advice for your area.")
        return sb.toString()
    }

    /** Strips heading markers for sharing as plain text. */
    fun toPlainText(plan: String): String =
        plan.lines().joinToString("\n") { it.replaceFirst(Regex("^#{1,3} "), "") }
}
