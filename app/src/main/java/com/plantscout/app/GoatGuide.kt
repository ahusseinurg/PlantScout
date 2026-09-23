package com.plantscout.app

import com.plantscout.app.Strategy.*

/** How useful goats are for controlling a plant. */
enum class GoatFit(val label: String, val rank: Int) {
    EXCELLENT("Excellent", 4),
    GOOD("Good", 3),
    PARTIAL("Partial — combine with other methods", 2),
    CAUTION("Use with caution", 1),
    UNSAFE("Not safe — keep goats away", 0)
}

data class GoatAdvice(val fit: GoatFit, val text: String)

/** Targeted grazing (goats) advice per plant, plus general how-to guidance. */
object GoatGuide {

    private fun g(fit: GoatFit, text: String) = GoatAdvice(fit, text)

    private val bySpecies: Map<String, GoatAdvice> = mapOf(
        "euphorbia esula" to g(GoatFit.EXCELLENT, "Goats eat leafy spurge readily and tolerate it well (cattle and horses avoid it). Graze 2–3 times a season from when it starts to bolt in spring. Over several years this thins stands dramatically, especially combined with biocontrol beetles or fall treatment."),
        "euphorbia myrsinites" to g(GoatFit.CAUTION, "The sap is harsh on eyes and bare skin; goats may nibble it but it's better dug out by hand."),
        "rosa multiflora" to g(GoatFit.EXCELLENT, "Goats love multiflora rose and push into thorny thickets other animals avoid. Repeat grazing for 2–3 seasons, then dig or treat the remaining crowns."),
        "centaurea solstitialis" to g(GoatFit.GOOD, "Goats and sheep can graze yellow starthistle in the early bolting stage, before spines harden. Never graze horses on it — it's fatal to them."),
        "bromus tectorum" to g(GoatFit.PARTIAL, "Graze hard in early spring while it's green, before seed heads form — the ripe awns injure mouths and eyes. Goats prefer browse, so sheep or cattle are often used for cheatgrass."),
        "sorghum halepense" to g(GoatFit.UNSAFE, "Johnsongrass can build up cyanide (especially after frost or drought) and nitrates, which can kill livestock."),
        "hypericum perforatum" to g(GoatFit.UNSAFE, "St John's wort causes severe sunburn-like skin damage (photosensitization) in grazing animals."),
        "tribulus terrestris" to g(GoatFit.UNSAFE, "Puncturevine can cause photosensitization ('bighead') in goats and sheep, and the burs injure mouths and feet."),
        "tamarix ramosissima" to g(GoatFit.PARTIAL, "Goats browse young tamarisk regrowth. Best used after cutting, to keep resprouts down."),
        "salsola tragus" to g(GoatFit.PARTIAL, "Edible only while young and soft; once it turns spiny goats avoid it."),
        "lepidium draba" to g(GoatFit.PARTIAL, "Goats and sheep graze whitetop early in the season, which cuts seed production, but won't kill the deep roots."),
        "cardaria draba" to g(GoatFit.PARTIAL, "Goats and sheep graze whitetop early in the season, which cuts seed production, but won't kill the deep roots."),
        "ailanthus altissima" to g(GoatFit.CAUTION, "Goats will browse tree-of-heaven suckers, but the leaves are mildly toxic. Use only as part of a mixed diet, mainly to suppress suckers after the main trees are treated."),
        "phragmites australis" to g(GoatFit.GOOD, "Goats graze young phragmites shoots and repeated grazing weakens stands, but grazing wetlands can damage banks and may need permits.")
    )

    private val byGenus: Map<String, GoatAdvice> = mapOf(
        "euphorbia" to g(GoatFit.CAUTION, "Goats tolerate many spurges, but the milky sap can irritate eyes and light-skinned muzzles."),
        "reynoutria" to g(GoatFit.GOOD, "Goats eat knotweed shoots eagerly. Graze every 3–4 weeks to starve the rhizomes — it weakens knotweed but won't eradicate it alone. Clean hooves and hold goats before moving them, as fragments can regrow."),
        "fallopia" to g(GoatFit.GOOD, "Goats eat knotweed shoots eagerly. Graze every 3–4 weeks to starve the rhizomes — it weakens knotweed but won't eradicate it alone."),
        "polygonum" to g(GoatFit.GOOD, "Goats eat knotweed shoots eagerly; repeated grazing weakens it but won't eradicate it alone."),
        "pueraria" to g(GoatFit.EXCELLENT, "Goats are a classic kudzu control — they strip leaves and vines again and again. Graze 2–4 times a season for 2–3 years, then remove surviving root crowns."),
        "toxicodendron" to g(GoatFit.EXCELLENT, "Goats eat poison ivy, oak and sumac without getting a rash. The oil stays on their coat, so wear gloves when handling them afterwards. They strip leaves but don't kill roots — graze repeatedly and dig out crowns."),
        "rubus" to g(GoatFit.EXCELLENT, "Goats love blackberry and bramble and push through thorny thickets. Repeat grazing weakens canes; follow up by digging root crowns."),
        "rosa" to g(GoatFit.GOOD, "Goats browse wild roses readily; repeat grazing, then dig or treat the crowns."),
        "lonicera" to g(GoatFit.EXCELLENT, "Honeysuckle is a goat favourite. Repeated browsing strips it back; cut and treat or dig the remaining stems."),
        "celastrus" to g(GoatFit.EXCELLENT, "Goats strip bittersweet vines well. Repeated grazing plus pulling roots gives the best results."),
        "hedera" to g(GoatFit.CAUTION, "Goats eat ivy, but it's mildly toxic in quantity. Make sure they have other forage and it isn't their only food."),
        "rhamnus" to g(GoatFit.GOOD, "Goats browse buckthorn leaves and strip bark on small stems. Combine with cut-stump treatment on large shrubs."),
        "frangula" to g(GoatFit.GOOD, "Goats browse buckthorn leaves and strip bark on small stems. Combine with cut-stump treatment on large shrubs."),
        "elaeagnus" to g(GoatFit.GOOD, "Goats browse Russian and autumn olive leaves and young stems. Use them to knock back regrowth, then cut and treat stumps."),
        "berberis" to g(GoatFit.GOOD, "Goats browse barberry despite the spines. Repeat grazing, then dig remaining plants."),
        "ligustrum" to g(GoatFit.CAUTION, "Privet leaves and berries are mildly toxic. Use goats only with plenty of other forage."),
        "wisteria" to g(GoatFit.UNSAFE, "Wisteria, especially its seeds and pods, is poisonous to livestock."),
        "robinia" to g(GoatFit.CAUTION, "Black locust bark and leaves are toxic to livestock; goats are more tolerant than horses but it isn't a safe main food."),
        "ulmus" to g(GoatFit.GOOD, "Goats browse elm seedlings, suckers and bark well."),
        "cirsium" to g(GoatFit.GOOD, "Goats eat thistle leaves and flower buds, stopping seed. Canada thistle needs repeated grazing through the season."),
        "carduus" to g(GoatFit.GOOD, "Goats eat thistle rosettes and flower buds, stopping seed."),
        "onopordum" to g(GoatFit.GOOD, "Goats eat thistle rosettes and flower buds, stopping seed."),
        "centaurea" to g(GoatFit.GOOD, "Goats and sheep graze knapweed well at the bolting and bud stage; repeated grazing sharply reduces seed."),
        "linaria" to g(GoatFit.PARTIAL, "Toadflax is mildly toxic and not a favourite; sheep are often used instead. Grazing reduces seed but doesn't kill the roots."),
        "convolvulus" to g(GoatFit.PARTIAL, "Goats eat bindweed, but grazing won't kill the deep roots and it's mildly toxic in large amounts. Use it to suppress growth between other treatments."),
        "equisetum" to g(GoatFit.UNSAFE, "Horsetail destroys vitamin B1 (thiamine) in livestock and can cause nerve problems."),
        "solanum" to g(GoatFit.UNSAFE, "Nightshades are poisonous to goats."),
        "senecio" to g(GoatFit.CAUTION, "Goats tolerate ragwort better than cattle or horses, but long-term grazing can still cause liver damage. Get vet advice first."),
        "jacobaea" to g(GoatFit.CAUTION, "Goats tolerate ragwort better than cattle or horses, but long-term grazing can still cause liver damage. Get vet advice first."),
        "tanacetum" to g(GoatFit.CAUTION, "Tansy is toxic in quantity; don't rely on goats for it."),
        "hypericum" to g(GoatFit.UNSAFE, "St John's wort causes photosensitization in grazing animals."),
        "pastinaca" to g(GoatFit.CAUTION, "Goats eat wild parsnip, but the sap can burn light-skinned muzzles and udders in sunlight. Graze only young growth and watch for blisters."),
        "heracleum" to g(GoatFit.CAUTION, "Goats and sheep eat hogweed, but the sap can burn light-skinned muzzles and udders in sunlight. Graze only young growth and watch for blisters."),
        "bassia" to g(GoatFit.CAUTION, "Kochia is palatable when young but can cause nitrate poisoning and photosensitization in large amounts. Mix with other forage."),
        "kochia" to g(GoatFit.CAUTION, "Kochia is palatable when young but can cause nitrate poisoning and photosensitization in large amounts. Mix with other forage."),
        "amaranthus" to g(GoatFit.CAUTION, "Pigweed can build up nitrates and oxalates. Fine as part of a mixed diet, not as the only food."),
        "chenopodium" to g(GoatFit.CAUTION, "Lambsquarters can build up nitrates. Fine as part of a mixed diet, not as the only food."),
        "rumex" to g(GoatFit.CAUTION, "Docks contain oxalates. Small amounts are fine; don't let goats eat mostly dock."),
        "oxalis" to g(GoatFit.CAUTION, "Oxalis contains oxalates. Small amounts are fine; don't let goats eat mostly oxalis."),
        "trifolium" to g(GoatFit.CAUTION, "Lush clover can cause bloat. Introduce goats gradually on a full stomach."),
        "glechoma" to g(GoatFit.CAUTION, "Ground ivy is mildly toxic in large amounts; not a good main food."),
        "taraxacum" to g(GoatFit.PARTIAL, "Goats happily eat dandelions and flowers (stopping seed), but won't kill the taproot."),
        "urtica" to g(GoatFit.GOOD, "Goats eat nettles, especially slightly wilted; repeated grazing weakens patches."),
        "arctium" to g(GoatFit.GOOD, "Goats eat burdock leaves and stalks. Graze before burs form — burs tangle badly in their coats."),
        "alliaria" to g(GoatFit.GOOD, "Goats eat garlic mustard readily. Graze before seed pods form."),
        "aegopodium" to g(GoatFit.GOOD, "Goats eat goutweed; repeated grazing weakens it."),
        "ambrosia" to g(GoatFit.GOOD, "Goats eat ragweed. Graze before it flowers."),
        "allium" to g(GoatFit.PARTIAL, "Goats eat wild onion and garlic, but it taints milk and won't kill the bulbs."),
        "cyperus" to g(GoatFit.PARTIAL, "Grazing removes leaves but won't kill the tubers."),
        "verbascum" to g(GoatFit.PARTIAL, "Goats dislike the fuzzy leaves; they may eat flower stalks. Better to dig rosettes."),
        "lythrum" to g(GoatFit.PARTIAL, "Goats eat loosestrife, but wetland grazing needs permits and care; biocontrol beetles are more common."),
        "tamarix" to g(GoatFit.PARTIAL, "Goats browse young tamarisk regrowth; best used after cutting to suppress resprouts.")
    )

    private fun defaultFor(s: Strategy): GoatAdvice = when (s) {
        ANNUAL_BROADLEAF -> g(GoatFit.GOOD, "Goats will eat it. Graze before it sets seed and repeat as new flushes appear.")
        ANNUAL_GRASS -> g(GoatFit.PARTIAL, "Goats prefer shrubs and broadleaf plants over grass; sheep or cattle usually do better. Graze before seed heads form.")
        TAPROOT -> g(GoatFit.PARTIAL, "Goats eat the leaves and flowers, which stops seeding, but won't kill the root. Combine with digging.")
        BIENNIAL -> g(GoatFit.GOOD, "Graze rosettes and bolting stalks to stop it seeding; repeat through the season.")
        CREEPING -> g(GoatFit.GOOD, "Repeated grazing every 3–4 weeks drains the roots, just like repeated cutting. Expect several seasons.")
        BULB -> g(GoatFit.PARTIAL, "Grazing removes leaves but not the bulbs or tubers; combine with digging.")
        WOODY -> g(GoatFit.EXCELLENT, "Goats are browsers: they strip leaves, eat young stems and girdle bark on small shrubs. Graze 2–3 times a season for 2–3 years, then cut and treat or dig what's left.")
        VINE -> g(GoatFit.EXCELLENT, "Goats strip vines well, including high up if they can stand on something. Graze repeatedly, then remove root crowns.")
        TREE -> g(GoatFit.PARTIAL, "Goats eat seedlings, suckers and low leaves and strip bark on young trees, but won't remove mature trees.")
        KNOTWEED -> g(GoatFit.GOOD, "Goats eat knotweed shoots eagerly; repeated grazing weakens it but won't eradicate it alone.")
        AQUATIC -> g(GoatFit.PARTIAL, "Grazing wetlands can damage banks and water quality and may need permits.")
        GENERIC -> g(GoatFit.PARTIAL, "Check this plant is safe for goats before grazing — ask a vet or your extension office.")
    }

    fun forPlant(c: Candidate, info: PlantInfo): GoatAdvice {
        val binomial = c.scientificName.lowercase().trim().split(Regex("\\s+")).take(2).joinToString(" ")
        val genus = c.genus.lowercase().trim().ifBlank { binomial.substringBefore(" ") }
        if (Hazard.DEADLY in info.hazards) {
            return g(GoatFit.UNSAFE, "Deadly to goats and other livestock. Remove it by hand before letting any animals graze the area.")
        }
        bySpecies[binomial]?.let { return it }
        byGenus[genus]?.let { return it }
        val base = defaultFor(info.strategy)
        return when {
            Hazard.LIVESTOCK in info.hazards ->
                g(GoatFit.UNSAFE, "Known to be toxic to livestock. Remove it before grazing the area.")
            Hazard.TOXIC in info.hazards ->
                g(GoatFit.CAUTION, "Poisonous to some animals. Ask a vet before letting goats graze it, and never let it be their main food.")
            else -> base
        }
    }

    /** General how-to for targeted grazing with goats. */
    val howTo: List<String> = listOf(
        "Goats work best on large or awkward areas: slopes, thickets, fence lines and brush. They weaken and suppress weeds and stop them seeding; most perennials still need follow-up digging, cutting or spot treatment to finish them off.",
        "Renting is usually easiest: many areas have targeted-grazing services that bring the goats, portable electric fencing and water. Search for 'goat grazing' or 'targeted grazing' near you.",
        "Timing: graze when the weeds are tender and before they set seed — usually spring to early summer — then again when regrowth reaches about 15–30 cm (6–12 in). Plan on 2–3 grazings a season for 2–3 years.",
        "Herd size: as a rough guide, around 30 goats clear about 0.2 hectares (half an acre) of dense brush in 3–5 days. Move them on when most of the target plant has been eaten, before they overgraze plants you want.",
        "Fencing: goats test fences constantly. Use electric netting or 4-strand electric fence. Wrap or fence off trees and shrubs you want to keep — goats strip bark.",
        "Care: provide fresh water, shade, and a goat mineral. Hay helps if the forage is mostly one plant. Introduce them gradually to avoid digestive upsets.",
        "Safety check first: walk the area and hand-remove any plants listed as 'Not safe' above. Hungry goats will eat plants they'd normally avoid, so never leave them on bare, overgrazed ground.",
        "Stop the spread: many weed seeds survive a goat's gut. Before moving goats from an infested area to clean land, hold them in a pen on clean hay for about 5 days.",
        "Check local rules: some towns restrict livestock, even temporarily. Let neighbours know.",
        "Afterwards: dig, cut or spot-treat what regrows, and reseed bare ground with desirable plants so weeds don't move back in."
    )
}
