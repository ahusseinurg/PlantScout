package com.plantscout.app

import com.plantscout.app.Hazard.*
import com.plantscout.app.Strategy.*

/** How a plant grows, which decides how it has to be removed. */
enum class Strategy(val label: String) {
    ANNUAL_BROADLEAF("Annual broadleaf weed"),
    ANNUAL_GRASS("Annual grassy weed"),
    TAPROOT("Taprooted perennial"),
    BIENNIAL("Biennial (rosette, then flowers)"),
    CREEPING("Creeping perennial (spreads by roots/runners)"),
    BULB("Bulb, tuber or sedge"),
    WOODY("Woody shrub"),
    VINE("Vine / climber"),
    TREE("Tree"),
    KNOTWEED("Knotweed (extreme regrowth)"),
    AQUATIC("Aquatic or wetland plant"),
    GENERIC("Unprofiled plant")
}

enum class Hazard(val label: String, val advice: String, val supplies: List<String>) {
    DEADLY(
        "Extremely poisonous",
        "Can be fatal if eaten, even in small amounts. Wear gloves, never handle with bare skin, wash hands and tools afterwards, keep children, pets and livestock away, and never burn it.",
        listOf("Waterproof gloves")
    ),
    TOXIC(
        "Poisonous",
        "Harmful if eaten. Wear gloves, wash hands afterwards, and keep children, pets and livestock away from pulled material.",
        listOf("Gloves")
    ),
    LIVESTOCK(
        "Toxic to livestock",
        "Remove it from pastures and hay fields; animals may eat wilted pulled plants, so don't leave them lying in grazing areas.",
        emptyList()
    ),
    SKIN(
        "Skin irritant",
        "Sap or hairs irritate skin and eyes. Wear gloves, long sleeves and eye protection.",
        listOf("Gloves", "Long sleeves", "Eye protection")
    ),
    PHOTOTOXIC(
        "Sap burns skin in sunlight",
        "Sap causes severe blistering burns when skin is exposed to sunlight. Cover all skin, wear a face shield, work on overcast days, wash any contact immediately with soap and water and keep that skin out of the sun for 48 hours. Get medical help for eye contact or burns.",
        listOf("Waterproof gloves", "Face shield", "Waterproof long-sleeved clothing")
    ),
    URUSHIOL(
        "Causes allergic rash",
        "Its oil causes a severe rash. NEVER burn it — the smoke can cause dangerous lung reactions. Wear disposable gloves over work gloves, and wash tools and clothing separately with degreasing soap. Dead plants and roots stay potent for years.",
        listOf("Disposable gloves", "Degreasing soap for tools and skin")
    ),
    SPINES(
        "Spines or thorns",
        "Wear thick leather gloves, long sleeves and eye protection.",
        listOf("Leather gloves", "Eye protection")
    ),
    NOXIOUS(
        "Often a regulated noxious weed",
        "Many states and counties legally require landowners to control it, and some require reporting. Check your state or county noxious weed list — your county weed department may offer free advice or cost-share help.",
        emptyList()
    )
}

data class PlantInfo(
    val strategy: Strategy,
    val hazards: Set<Hazard> = emptySet(),
    val note: String = ""
)

data class Playbook(
    val timing: String,
    val steps: List<String>,
    val prevent: List<String>,
    val chemical: List<String>,
    val disposal: List<String>,
    val followUp: List<String>,
    val supplies: List<String>
)

object KnowledgeBase {

    private fun p(s: Strategy, vararg h: Hazard, note: String = "") = PlantInfo(s, h.toSet(), note)

    /** Keyed by lowercase "genus species". */
    private val species: Map<String, PlantInfo> = mapOf(
        "taraxacum officinale" to p(TAPROOT),
        "cirsium arvense" to p(CREEPING, SPINES, NOXIOUS, note = "Canada thistle roots can run several metres deep and sideways; pulling alone won't clear an established patch. Repeated cutting plus fall treatment works best."),
        "cirsium vulgare" to p(BIENNIAL, SPINES, NOXIOUS, note = "Bull thistle: cutting the taproot below the rosette kills it."),
        "euphorbia esula" to p(CREEPING, SKIN, NOXIOUS, LIVESTOCK, note = "Leafy spurge roots can reach several metres deep. Plan on multiple years; grazing goats or approved biocontrol beetles are used on large infestations."),
        "euphorbia myrsinites" to p(CREEPING, SKIN, NOXIOUS, note = "Myrtle spurge: dig out with the root crown while wearing eye protection — the sap is especially harmful to eyes."),
        "bromus tectorum" to p(ANNUAL_GRASS, note = "Cheatgrass dries out early and is a major wildfire fuel. Reseeding with perennial grasses is key to keeping it out."),
        "bassia scoparia" to p(ANNUAL_BROADLEAF, note = "Kochia becomes a tumbleweed that spreads seed as it rolls; remove before it dries. Herbicide-resistant populations are common."),
        "kochia scoparia" to p(ANNUAL_BROADLEAF, note = "Kochia becomes a tumbleweed that spreads seed as it rolls; remove before it dries. Herbicide-resistant populations are common."),
        "portulaca oleracea" to p(ANNUAL_BROADLEAF, note = "Purslane re-roots from stem pieces and keeps ripening seeds after being pulled — bag it rather than leaving it on the soil."),
        "tribulus terrestris" to p(ANNUAL_BROADLEAF, SPINES, NOXIOUS, note = "Puncturevine burs flatten tires and injure pets. Pull before burs form and sweep up any that have dropped."),
        "cyperus esculentus" to p(BULB, note = "Yellow nutsedge: pulling snaps the stem and leaves tubers that each sprout again."),
        "cyperus rotundus" to p(BULB, note = "Purple nutsedge: one of the world's worst weeds; tubers chain together underground."),
        "allium vineale" to p(BULB),
        "ficaria verna" to p(BULB, TOXIC, note = "Lesser celandine forms many small tubers and bulbils; don't disturb it once it has flowered."),
        "ranunculus ficaria" to p(BULB, TOXIC, note = "Lesser celandine forms many small tubers and bulbils; don't disturb it once it has flowered."),
        "lepidium draba" to p(CREEPING, NOXIOUS, note = "Whitetop (hoary cress) spreads by deep creeping roots; mowing before flowering and repeated treatment are needed."),
        "cardaria draba" to p(CREEPING, NOXIOUS, note = "Whitetop (hoary cress) spreads by deep creeping roots; mowing before flowering and repeated treatment are needed."),
        "lepidium latifolium" to p(CREEPING, NOXIOUS, note = "Perennial pepperweed: roots regrow from small fragments. Avoid digging; use repeated cutting and treatment."),
        "alliaria petiolata" to p(BIENNIAL, note = "Garlic mustard: pull the whole root before seed pods form; bag flowering plants because seeds keep ripening."),
        "conium maculatum" to p(BIENNIAL, DEADLY, NOXIOUS, note = "Poison hemlock: all parts are deadly. Never use a string trimmer on it — it sprays sap and plant fragments. Dig with full protection or cut and bag."),
        "cicuta maculata" to p(TAPROOT, DEADLY, note = "Water hemlock is among the most poisonous plants in North America; the roots are the most toxic part. Consider professional removal."),
        "cicuta douglasii" to p(TAPROOT, DEADLY, note = "Water hemlock is among the most poisonous plants in North America; the roots are the most toxic part. Consider professional removal."),
        "pastinaca sativa" to p(BIENNIAL, PHOTOTOXIC, note = "Wild parsnip: sap on skin plus sunlight causes serious burns. Cut the root below the crown with a spade rather than pulling by hand."),
        "heracleum mantegazzianum" to p(BIENNIAL, PHOTOTOXIC, NOXIOUS, note = "Giant hogweed is a federally regulated noxious weed in the US and regulated in many other countries. Report it to your state agency — professional removal is strongly recommended."),
        "urtica dioica" to p(CREEPING, SKIN),
        "rhamnus cathartica" to p(WOODY, note = "Common buckthorn holds green leaves late into fall — the easiest time to spot it."),
        "frangula alnus" to p(WOODY, note = "Glossy buckthorn: treat cut stumps immediately or it resprouts vigorously."),
        "lonicera japonica" to p(VINE, note = "Japanese honeysuckle stays semi-evergreen, so late-fall treatment avoids hitting dormant natives."),
        "lonicera maackii" to p(WOODY),
        "lonicera tatarica" to p(WOODY),
        "lonicera morrowii" to p(WOODY),
        "rosa multiflora" to p(WOODY, SPINES),
        "elaeagnus angustifolia" to p(WOODY, SPINES, NOXIOUS, note = "Russian olive resprouts from roots and stumps; cut-stump treatment is usually required."),
        "elaeagnus umbellata" to p(WOODY),
        "berberis thunbergii" to p(WOODY, SPINES),
        "rubus armeniacus" to p(WOODY, SPINES, note = "Himalayan blackberry: canes root where tips touch the ground — dig the root crowns."),
        "pueraria montana" to p(VINE, NOXIOUS, note = "Kudzu: the root crown (the knot at soil level) must be destroyed or the vine regrows."),
        "hedera helix" to p(VINE, TOXIC, note = "English ivy's waxy leaves shed herbicide; cutting and pulling works better than spraying."),
        "hedera hibernica" to p(VINE, TOXIC, note = "Ivy's waxy leaves shed herbicide; cutting and pulling works better than spraying."),
        "celastrus orbiculatus" to p(VINE, note = "Oriental bittersweet: don't cut without treating — cutting alone triggers heavy root suckering."),
        "wisteria sinensis" to p(VINE, TOXIC),
        "wisteria floribunda" to p(VINE, TOXIC),
        "ailanthus altissima" to p(TREE, note = "Tree of heaven: DO NOT cut it down first — that triggers dozens of root suckers. Treat it (hack-and-squirt or basal bark) and wait until it has died back before felling."),
        "ulmus pumila" to p(TREE),
        "acer platanoides" to p(TREE),
        "robinia pseudoacacia" to p(TREE, SPINES, TOXIC, note = "Black locust suckers aggressively after cutting; treat before or at cutting."),
        "reynoutria japonica" to p(KNOTWEED, NOXIOUS),
        "fallopia japonica" to p(KNOTWEED, NOXIOUS),
        "polygonum cuspidatum" to p(KNOTWEED, NOXIOUS),
        "reynoutria sachalinensis" to p(KNOTWEED, NOXIOUS),
        "fallopia sachalinensis" to p(KNOTWEED, NOXIOUS),
        "phragmites australis" to p(AQUATIC, note = "Invasive common reed: some native subspecies look similar; confirm before treating. Large stands need permits."),
        "lythrum salicaria" to p(AQUATIC, NOXIOUS, note = "Purple loosestrife: one plant can produce millions of seeds — cut and bag flower spikes first."),
        "eichhornia crassipes" to p(AQUATIC, NOXIOUS),
        "pontederia crassipes" to p(AQUATIC, NOXIOUS),
        "ricinus communis" to p(ANNUAL_BROADLEAF, DEADLY, note = "Castor bean seeds contain ricin. Wear gloves and bag all seed pods."),
        "nerium oleander" to p(WOODY, DEADLY, note = "Oleander: every part is poisonous. Never burn or chip it where smoke or dust could be inhaled."),
        "hypericum perforatum" to p(CREEPING, LIVESTOCK, NOXIOUS),
        "isatis tinctoria" to p(BIENNIAL, NOXIOUS, note = "Dyer's woad: dig rosettes with a sharp spade below the crown; bag any yellow-flowering plants."),
        "linaria dalmatica" to p(CREEPING, NOXIOUS, note = "Dalmatian toadflax has waxy leaves and deep creeping roots; expect several years of follow-up."),
        "linaria vulgaris" to p(CREEPING, NOXIOUS),
        "centaurea stoebe" to p(TAPROOT, NOXIOUS, SKIN, note = "Spotted knapweed: wear gloves — handling can irritate skin. Bag flowering plants."),
        "centaurea diffusa" to p(BIENNIAL, NOXIOUS, note = "Diffuse knapweed breaks off and tumbles to spread seed; remove before it dries."),
        "centaurea solstitialis" to p(ANNUAL_BROADLEAF, SPINES, NOXIOUS, LIVESTOCK, note = "Yellow starthistle is toxic to horses."),
        "onopordum acanthium" to p(BIENNIAL, SPINES, NOXIOUS),
        "carduus nutans" to p(BIENNIAL, SPINES, NOXIOUS),
        "arctium minus" to p(BIENNIAL),
        "arctium lappa" to p(BIENNIAL),
        "tanacetum vulgare" to p(CREEPING, TOXIC, NOXIOUS),
        "jacobaea vulgaris" to p(BIENNIAL, LIVESTOCK, NOXIOUS),
        "senecio jacobaea" to p(BIENNIAL, LIVESTOCK, NOXIOUS),
        "salsola tragus" to p(ANNUAL_BROADLEAF, SPINES, note = "Russian thistle breaks off as a tumbleweed; remove while green."),
        "salsola kali" to p(ANNUAL_BROADLEAF, SPINES),
        "tamarix ramosissima" to p(WOODY, NOXIOUS, note = "Saltcedar (tamarisk) has very deep roots and resprouts readily; cut-stump or basal-bark treatment is standard. Near water, only aquatic-labeled products may be used."),
        "tamarix chinensis" to p(WOODY, NOXIOUS),
        "equisetum arvense" to p(CREEPING, LIVESTOCK, note = "Field horsetail shrugs off most herbicides. Keep cutting shoots, improve drainage, and raise soil pH over time."),
        "aegopodium podagraria" to p(CREEPING, note = "Goutweed/bishop's weed regrows from tiny root pieces; smothering for a full season is usually most effective."),
        "glechoma hederacea" to p(CREEPING),
        "convolvulus arvensis" to p(CREEPING, NOXIOUS, note = "Field bindweed roots go several metres deep and seeds survive decades in soil. Consistent cutting every 2–3 weeks slowly starves it."),
        "calystegia sepium" to p(CREEPING),
        "elymus repens" to p(CREEPING, note = "Quackgrass: sift out every white rhizome — each node can sprout."),
        "elytrigia repens" to p(CREEPING, note = "Quackgrass: sift out every white rhizome — each node can sprout."),
        "cynodon dactylon" to p(CREEPING, note = "Bermudagrass spreads above and below ground; edge beds deeply and smother patches."),
        "sorghum halepense" to p(CREEPING, NOXIOUS, LIVESTOCK),
        "digitaria sanguinalis" to p(ANNUAL_GRASS),
        "digitaria ischaemum" to p(ANNUAL_GRASS),
        "poa annua" to p(ANNUAL_GRASS),
        "datura stramonium" to p(ANNUAL_BROADLEAF, DEADLY, note = "Jimsonweed: all parts, especially seeds, are highly toxic. Bag seed pods."),
        "datura wrightii" to p(TAPROOT, DEADLY),
        "ambrosia artemisiifolia" to p(ANNUAL_BROADLEAF, note = "Common ragweed is a major hay fever trigger — remove before it flowers in late summer."),
        "ambrosia trifida" to p(ANNUAL_BROADLEAF),
        "trifolium repens" to p(CREEPING, note = "White clover adds nitrogen and feeds pollinators — many gardeners now keep it in lawns on purpose."),
        "plantago major" to p(TAPROOT),
        "plantago lanceolata" to p(TAPROOT),
        "galium aparine" to p(ANNUAL_BROADLEAF),
        "stellaria media" to p(ANNUAL_BROADLEAF),
        "oxalis stricta" to p(ANNUAL_BROADLEAF),
        "oxalis pes-caprae" to p(BULB),
        "toxicodendron radicans" to p(VINE, URUSHIOL),
        "toxicodendron rydbergii" to p(WOODY, URUSHIOL),
        "toxicodendron diversilobum" to p(WOODY, URUSHIOL),
        "toxicodendron vernix" to p(WOODY, URUSHIOL),
        "cenchrus longispinus" to p(ANNUAL_GRASS, SPINES),
        "solanum carolinense" to p(CREEPING, SPINES, TOXIC),
        "solanum elaeagnifolium" to p(CREEPING, SPINES, TOXIC, NOXIOUS)
    )

    /** Keyed by lowercase genus. */
    private val genera: Map<String, PlantInfo> = mapOf(
        "taraxacum" to p(TAPROOT),
        "rumex" to p(TAPROOT, note = "Docks have long, branched taproots; dig deep and remove seed stalks before they turn brown."),
        "arctium" to p(BIENNIAL, note = "Burdock burs cling to pets and clothing; cut before burs form."),
        "cirsium" to p(BIENNIAL, SPINES),
        "carduus" to p(BIENNIAL, SPINES, NOXIOUS),
        "onopordum" to p(BIENNIAL, SPINES, NOXIOUS),
        "centaurea" to p(TAPROOT, NOXIOUS),
        "convolvulus" to p(CREEPING),
        "calystegia" to p(CREEPING),
        "equisetum" to p(CREEPING, LIVESTOCK),
        "digitaria" to p(ANNUAL_GRASS),
        "setaria" to p(ANNUAL_GRASS),
        "echinochloa" to p(ANNUAL_GRASS),
        "eleusine" to p(ANNUAL_GRASS),
        "bromus" to p(ANNUAL_GRASS),
        "cenchrus" to p(ANNUAL_GRASS, SPINES),
        "amaranthus" to p(ANNUAL_BROADLEAF),
        "chenopodium" to p(ANNUAL_BROADLEAF),
        "portulaca" to p(ANNUAL_BROADLEAF),
        "stellaria" to p(ANNUAL_BROADLEAF),
        "lamium" to p(ANNUAL_BROADLEAF),
        "capsella" to p(ANNUAL_BROADLEAF),
        "ambrosia" to p(ANNUAL_BROADLEAF),
        "erigeron" to p(ANNUAL_BROADLEAF),
        "conyza" to p(ANNUAL_BROADLEAF),
        "galium" to p(ANNUAL_BROADLEAF),
        "tribulus" to p(ANNUAL_BROADLEAF, SPINES),
        "salsola" to p(ANNUAL_BROADLEAF, SPINES),
        "bassia" to p(ANNUAL_BROADLEAF),
        "solanum" to p(ANNUAL_BROADLEAF, TOXIC, note = "Nightshades: unripe berries and leaves are poisonous."),
        "datura" to p(ANNUAL_BROADLEAF, DEADLY),
        "euphorbia" to p(ANNUAL_BROADLEAF, SKIN, note = "Spurges have milky sap that irritates skin and eyes."),
        "oxalis" to p(BULB),
        "cyperus" to p(BULB),
        "allium" to p(BULB),
        "plantago" to p(TAPROOT),
        "verbascum" to p(BIENNIAL, note = "Mullein: easy to pull as a rosette; one flowering stalk can shed over 100,000 long-lived seeds."),
        "senecio" to p(BIENNIAL, LIVESTOCK),
        "urtica" to p(CREEPING, SKIN),
        "glechoma" to p(CREEPING),
        "ranunculus" to p(CREEPING, TOXIC),
        "hieracium" to p(CREEPING),
        "pilosella" to p(CREEPING),
        "trifolium" to p(CREEPING),
        "linaria" to p(CREEPING, NOXIOUS),
        "lepidium" to p(ANNUAL_BROADLEAF),
        "hypericum" to p(CREEPING, LIVESTOCK),
        "tanacetum" to p(CREEPING),
        "aegopodium" to p(CREEPING),
        "elymus" to p(CREEPING),
        "cynodon" to p(CREEPING),
        "rhamnus" to p(WOODY),
        "frangula" to p(WOODY),
        "ligustrum" to p(WOODY, TOXIC, note = "Privet berries are mildly toxic; remove fruiting shrubs first so birds don't spread seed."),
        "berberis" to p(WOODY, SPINES),
        "elaeagnus" to p(WOODY),
        "tamarix" to p(WOODY, NOXIOUS),
        "rubus" to p(WOODY, SPINES),
        "rosa" to p(WOODY, SPINES),
        "lonicera" to p(WOODY),
        "pueraria" to p(VINE),
        "hedera" to p(VINE, TOXIC),
        "celastrus" to p(VINE),
        "wisteria" to p(VINE, TOXIC),
        "vitis" to p(VINE),
        "parthenocissus" to p(VINE, SKIN),
        "toxicodendron" to p(VINE, URUSHIOL),
        "ailanthus" to p(TREE, note = "Tree of heaven: DO NOT cut it down first — treat it and wait for dieback before felling."),
        "ulmus" to p(TREE),
        "robinia" to p(TREE, SPINES, TOXIC),
        "reynoutria" to p(KNOTWEED, NOXIOUS),
        "phragmites" to p(AQUATIC),
        "lythrum" to p(AQUATIC, NOXIOUS),
        "myriophyllum" to p(AQUATIC),
        "hydrilla" to p(AQUATIC, NOXIOUS),
        "egeria" to p(AQUATIC),
        "typha" to p(AQUATIC, note = "Cattails are native in many areas and valuable wildlife habitat — confirm they're a problem before removing."),
        "conium" to p(BIENNIAL, DEADLY),
        "cicuta" to p(TAPROOT, DEADLY),
        "heracleum" to p(BIENNIAL, PHOTOTOXIC),
        "pastinaca" to p(BIENNIAL, PHOTOTOXIC),
        "ricinus" to p(ANNUAL_BROADLEAF, DEADLY),
        "nerium" to p(WOODY, DEADLY)
    )

    /** Keyed by lowercase family: a rough fallback only. */
    private val families: Map<String, PlantInfo> = mapOf(
        "poaceae" to p(ANNUAL_GRASS, note = "Grass family. If it spreads by runners or underground stems rather than forming a clump, use the creeping-perennial approach instead."),
        "cyperaceae" to p(BULB),
        "asteraceae" to p(TAPROOT),
        "brassicaceae" to p(ANNUAL_BROADLEAF),
        "amaranthaceae" to p(ANNUAL_BROADLEAF),
        "caryophyllaceae" to p(ANNUAL_BROADLEAF),
        "polygonaceae" to p(TAPROOT),
        "solanaceae" to p(ANNUAL_BROADLEAF, TOXIC, note = "Nightshade family: many members are poisonous."),
        "euphorbiaceae" to p(ANNUAL_BROADLEAF, SKIN),
        "apiaceae" to p(BIENNIAL, SKIN, note = "Carrot family: several look-alikes are deadly or cause sunlight burns (hemlocks, wild parsnip, hogweed). Treat as dangerous until confirmed."),
        "anacardiaceae" to p(WOODY, SKIN, note = "Cashew family includes poison ivy, oak and sumac. Treat as a rash risk until confirmed."),
        "convolvulaceae" to p(CREEPING),
        "vitaceae" to p(VINE),
        "tamaricaceae" to p(WOODY),
        "elaeagnaceae" to p(WOODY),
        "rhamnaceae" to p(WOODY),
        "equisetaceae" to p(CREEPING),
        "typhaceae" to p(AQUATIC),
        "haloragaceae" to p(AQUATIC),
        "hydrocharitaceae" to p(AQUATIC),
        "pinaceae" to p(TREE),
        "cupressaceae" to p(TREE),
        "fagaceae" to p(TREE),
        "sapindaceae" to p(TREE),
        "salicaceae" to p(TREE),
        "ulmaceae" to p(TREE),
        "betulaceae" to p(TREE),
        "simaroubaceae" to p(TREE)
    )

    data class Match(val info: PlantInfo, val level: String)

    fun lookup(c: Candidate): Match {
        val parts = c.scientificName.lowercase().trim().split(Regex("\\s+"))
        val binomial = parts.take(2).joinToString(" ")
        species[binomial]?.let { return Match(it, "species") }
        val genus = c.genus.lowercase().trim().ifBlank { parts.firstOrNull() ?: "" }
        genera[genus]?.let { return Match(it, "genus") }
        families[c.family.lowercase().trim()]?.let { return Match(it, "family") }
        return Match(PlantInfo(GENERIC), "none")
    }
}

object Playbooks {

    fun forStrategy(s: Strategy): Playbook = when (s) {
        ANNUAL_BROADLEAF -> Playbook(
            timing = "Act before it flowers. Annuals only come back from seed, so stopping seed production is the whole game. Young seedlings are the easiest to kill.",
            steps = listOf(
                "Hand-pull or hoe when the soil is moist, getting the top of the root.",
                "For large patches, use a stirrup (scuffle) hoe on a dry, sunny day so cut seedlings shrivel.",
                "If it's already flowering, snip or bag the flower and seed heads first so seeds don't drop while you work.",
                "Where pulling isn't practical, mow or trim before seeds form."
            ),
            prevent = listOf(
                "Mulch bare soil 5–8 cm (2–3 in) deep — most annual weed seeds need light to germinate.",
                "Keep lawns thick (overseed, mow high) and garden beds densely planted.",
                "Avoid deep tilling, which brings buried seeds to the surface."
            ),
            chemical = listOf(
                "Lawns: a selective broadleaf herbicide (2,4-D, dicamba or MCPP blends) kills broadleaf weeds without harming grass.",
                "Beds and paths: a pre-emergent in early spring stops new seedlings; spot-treat with glyphosate only where it can't drift onto plants you want."
            ),
            disposal = listOf("Plants without seeds can be composted. Bag anything with flowers or seeds — home compost rarely gets hot enough to kill them."),
            followUp = listOf(
                "Re-check every 2–3 weeks through the growing season; new flushes follow rain or watering.",
                "Expect seedlings from the soil seed bank for several years — each seed-free season shrinks it."
            ),
            supplies = listOf("Gloves", "Hoe or hand weeder", "Mulch", "Bags for seed heads")
        )

        ANNUAL_GRASS -> Playbook(
            timing = "Remove it before seed heads ripen. For next year's crop, pre-emergent timing matters most: apply when soil reaches about 13 °C (55 °F) in spring — roughly when forsythia finishes blooming.",
            steps = listOf(
                "Pull young clumps by grasping at the base and wiggling out the roots.",
                "Cut or mow seed heads before they ripen.",
                "In lawns, dig out clumps and reseed the bare spot with your lawn grass right away."
            ),
            prevent = listOf(
                "Mow lawns high (7–9 cm / 3–3.5 in) to shade out seedlings.",
                "Water deeply and less often rather than a little every day.",
                "Mulch beds so soil isn't left bare."
            ),
            chemical = listOf(
                "Pre-emergent (prodiamine, dithiopyr or pendimethalin) in early spring. It also stops grass seed you sow, so don't overseed the same area that season.",
                "After it's up: in lawns, quinclorac or fenoxaprop for crabgrass-type grasses; in flower beds, clethodim or sethoxydim (they don't harm broadleaf plants)."
            ),
            disposal = listOf("Bag seed heads; compost the rest."),
            followUp = listOf(
                "Check every 2 weeks from late spring to fall.",
                "Repeat pre-emergent for 2–3 springs to exhaust the seed bank."
            ),
            supplies = listOf("Gloves", "Hand weeder", "Grass seed for patching", "Pre-emergent (optional)")
        )

        TAPROOT -> Playbook(
            timing = "Best removed in spring before flowering, or in fall. Dig when the soil is moist so the whole root comes out.",
            steps = listOf(
                "Push a long forked weeder or narrow spade straight down beside the crown and lever the root out.",
                "Get at least the top 10–15 cm (4–6 in) of taproot — broken pieces left behind often resprout.",
                "If you can't dig today, at least remove the flowers so it can't seed."
            ),
            prevent = listOf(
                "Mulch beds and keep lawns dense.",
                "Fill holes left by digging with soil and seed or mulch so new weeds don't move in."
            ),
            chemical = listOf(
                "Spot-treat with a selective broadleaf herbicide in lawns (2,4-D/dicamba/MCPP; clopyralid for thistles and knapweeds), or glyphosate in beds. Fall treatment works best, when plants pull sugars down to their roots."
            ),
            disposal = listOf("Dry roots in the sun on pavement before composting them; bag flower and seed heads."),
            followUp = listOf(
                "Check the same spots after 3–4 weeks for resprouts and dig again.",
                "Re-inspect next spring."
            ),
            supplies = listOf("Gloves", "Forked weeder or narrow spade", "Bags")
        )

        BIENNIAL -> Playbook(
            timing = "Year 1 it's a flat rosette of leaves; year 2 it sends up a flowering stalk, sets seed and dies. Killing rosettes in fall or early spring is the easiest and most effective approach.",
            steps = listOf(
                "Dig rosettes out, cutting the taproot 5–10 cm (2–4 in) below the soil surface with a sharp spade.",
                "If it has already bolted, cut the stalk at ground level before the flowers open.",
                "If it's already flowering, cut the heads off into a bag first — cut flowers can still ripen seed.",
                "Don't simply mow bolted plants; they often re-flower lower down."
            ),
            prevent = listOf(
                "Keep ground covered with desirable plants or mulch — rosettes establish in bare or disturbed soil.",
                "Walk the area every year; seeds can stay viable for years."
            ),
            chemical = listOf(
                "Rosettes: spot-spray a broadleaf herbicide (2,4-D, clopyralid or triclopyr) in fall or spring. Plants that have bolted respond poorly — cut them instead."
            ),
            disposal = listOf("Bag and bin all flowering or seeding material. Rosettes without flowers can dry out and be composted."),
            followUp = listOf(
                "Check monthly through the growing season.",
                "Expect new rosettes for 3–5 years from the seed bank."
            ),
            supplies = listOf("Gloves", "Sharp spade", "Heavy-duty bags")
        )

        CREEPING -> Playbook(
            timing = "This plant spreads through underground roots or runners, and even small fragments can become new plants. Plan on at least a full season. Its weak point is late summer to early fall, when it moves energy down into its roots.",
            steps = listOf(
                "Do not rototill or chop up the soil — that multiplies it.",
                "Option A, exhaust it: cut or pull the tops every 2–3 weeks all season. Each regrowth drains the roots; this takes 1–3 seasons.",
                "Option B, smother it: cut to the ground, cover with overlapping cardboard plus 10 cm (4 in) of mulch, or heavy black plastic extending 30 cm (1 ft) past the patch, and leave it for at least one full growing season.",
                "For small patches in loose soil, fork the roots out carefully and sift for every fragment."
            ),
            prevent = listOf(
                "Install root barrier or edging 20–30 cm (8–12 in) deep along borders.",
                "Replant cleared ground densely once it's clean."
            ),
            chemical = listOf(
                "Systemic herbicide (glyphosate, or 2,4-D, dicamba or clopyralid for broadleaf types) applied in late summer to early fall to actively growing plants. If you've been cutting, let it regrow to 15–20 cm (6–8 in) first so there's leaf to absorb it.",
                "Expect to repeat treatment the following season."
            ),
            disposal = listOf("Never compost roots or runners. Bag them, or seal them in a bucket of water for a month to rot before composting."),
            followUp = listOf(
                "Check every 2 weeks in the first season and remove every new shoot.",
                "Keep monitoring for 2–3 years."
            ),
            supplies = listOf("Gloves", "Garden fork", "Cardboard and mulch or heavy black sheeting", "Root barrier edging", "Bags")
        )

        BULB -> Playbook(
            timing = "Spreads by bulbs, bulblets, tubers or nutlets that break off when pulled. Act early in the season, before the plant has 5–6 leaves and starts making new tubers (usually by early summer).",
            steps = listOf(
                "Don't just pull — the tops snap off and leave the underground parts behind.",
                "Dig 15–20 cm (6–8 in) deep with a trowel and lift the whole clump with its soil.",
                "Sift the soil and remove every bulb or tuber.",
                "In lawns, keep removing the leaves weekly to starve the underground parts."
            ),
            prevent = listOf(
                "Fix drainage and avoid overwatering — nutsedge and many bulbous weeds love wet soil.",
                "Don't bring in soil from infested areas, and clean tools after use."
            ),
            chemical = listOf(
                "Nutsedge: halosulfuron or sulfentrazone (ordinary broadleaf killers don't work on it).",
                "Wild garlic/onion: 2,4-D blends with a surfactant, repeated. Oxalis in lawns: triclopyr or fluroxypyr."
            ),
            disposal = listOf("Bin the soil and bulbs — don't compost them."),
            followUp = listOf("Recheck every 2–3 weeks; expect 2–3 seasons to use up the underground bank."),
            supplies = listOf("Trowel", "Soil sieve", "Bags")
        )

        WOODY -> Playbook(
            timing = "Can be removed any time, but cut-stump treatments work best from late summer through fall. Many invasive shrubs leaf out earlier and hold leaves later than native plants, making them easy to spot in early spring or late fall.",
            steps = listOf(
                "Seedlings and small shrubs (stems under about 2 cm / ¾ in): pull by hand or with a weed wrench when the soil is moist, getting the root crown.",
                "Larger shrubs: cut close to the ground with loppers or a saw. Most will resprout unless the stump is treated or the root crown is dug out.",
                "Without herbicide: cut resprouts every few weeks for 2–3 years, or cover the stump with a thick black bag tied tightly around it."
            ),
            prevent = listOf(
                "Replant gaps with native shrubs to limit reinvasion from bird-spread seed.",
                "If time is short, remove berry-bearing shrubs first."
            ),
            chemical = listOf(
                "Cut-stump: within 5–15 minutes of cutting, paint the outer ring of the stump with triclopyr or glyphosate at the label's cut-stump rate.",
                "Basal bark (stems under about 15 cm / 6 in): triclopyr ester in oil applied to the lower 30–45 cm (12–18 in) of bark."
            ),
            disposal = listOf("Branches without berries can be chipped or piled for wildlife. Bag berries and seeds. Keep cut stems off moist soil — some species re-root."),
            followUp = listOf(
                "Check stumps for resprouts after 1 month and again at 3 months.",
                "Pull new seedlings every spring for several years."
            ),
            supplies = listOf("Loppers or pruning saw", "Weed wrench (optional)", "Heavy gloves", "Eye protection", "Paintbrush applicator (if treating stumps)")
        )

        VINE -> Playbook(
            timing = "Anytime, but free trees and structures first, before the vines add weight and shade. Cut before fruit or seed forms.",
            steps = listOf(
                "Cut every vine stem at about waist height and again at ground level, leaving a gap. Leave the upper vine to die in place — pulling it down can strip bark or bring branches down.",
                "Pull ground runners and dig out the root crowns; many vines resprout from roots.",
                "For vines carpeting the ground, cut along the edges and roll the mat up like a rug."
            ),
            prevent = listOf(
                "Keep a vine-free zone around tree trunks and fence lines; check twice a year.",
                "Replant cleared ground quickly."
            ),
            chemical = listOf(
                "Paint freshly cut stems immediately with triclopyr or glyphosate.",
                "Spray regrowth once it's knee-high, keeping spray off plants you want."
            ),
            disposal = listOf("Bag seeds and berries. Let cut vines dry fully off the ground before composting — many root from fragments."),
            followUp = listOf(
                "Check monthly for resprouts during the first season.",
                "Expect 2–3 years of follow-up for established vines."
            ),
            supplies = listOf("Loppers or hand saw", "Gloves", "Bags", "Paintbrush applicator (if treating stems)")
        )

        TREE -> Playbook(
            timing = "Choose the method before cutting — many unwanted trees respond to cutting by sending up dozens of root suckers. Late summer to fall treatment is most effective.",
            steps = listOf(
                "Seedlings and saplings: pull or dig out the whole root, ideally when the soil is moist.",
                "Large trees, or any tree near buildings, fences or power lines: hire a certified arborist.",
                "Without herbicide, girdle it: remove a 5–10 cm (2–4 in) band of bark and the green layer beneath all the way around the trunk. It dies over 1–2 years; cut suckers as they appear."
            ),
            prevent = listOf(
                "Remove seed-producing trees first to stop the spread.",
                "Pull seedlings every year — they're easiest in their first season."
            ),
            chemical = listOf(
                "Hack-and-squirt: make downward angled cuts around the trunk with a hatchet and put a small amount of triclopyr, glyphosate or imazapyr into each cut. Wait until the tree has died before felling it.",
                "Cut-stump: paint herbicide on the outer ring of the fresh stump within minutes of cutting."
            ),
            disposal = listOf("Use the wood as firewood or chip it, but don't move firewood long distances (it spreads pests). Bag seeds."),
            followUp = listOf(
                "Check monthly for root suckers in the first season, and cut or treat them.",
                "Re-inspect for 2–3 years."
            ),
            supplies = listOf("Saw or hatchet", "Helmet, eye and ear protection", "Certified arborist for large trees")
        )

        KNOTWEED -> Playbook(
            timing = "One of the hardest plants to eradicate — its rhizomes reach metres deep and a fragment the size of a fingernail can regrow. Plan for 3–5 years. Treat in late summer to fall, at or after flowering.",
            steps = listOf(
                "Don't dig, mow, chip or strim it casually — that spreads it. Never move soil from the area.",
                "Cut canes at ground level every 2–3 weeks through the growing season to weaken it, and bag every piece.",
                "Or smother: cut canes, cover with heavy-duty tarp reaching 2 m (6 ft) beyond the patch, weigh it down, and leave it for years, stamping down any shoots underneath.",
                "Near buildings, or where lenders or local rules require a management plan, use a licensed specialist."
            ),
            prevent = listOf(
                "Clean tools, boots and tires before leaving the area.",
                "Monitor well beyond the edge of the visible patch."
            ),
            chemical = listOf("Glyphosate or imazapyr sprayed in late summer to fall, or injected into each cane. Expect 2–4 years of repeat treatment."),
            disposal = listOf("Never compost it. Bag it, dry it completely and burn where legal, or take it to a licensed landfill — many places regulate knotweed waste."),
            followUp = listOf("Inspect monthly in the growing season for at least 3 years after the last shoot appears."),
            supplies = listOf("Loppers", "Heavy-duty tarp", "Heavy-duty bags", "Licensed specialist (recommended)")
        )

        AQUATIC -> Playbook(
            timing = "Plants in or beside water are usually covered by water regulations. Check with your state or local natural resources agency before doing anything beyond hand removal.",
            steps = listOf(
                "Hand-pull or rake small infestations, lifting whole plants including roots.",
                "Put a fine net downstream to catch fragments — many aquatic plants regrow from tiny pieces.",
                "Skim floating plants out regularly.",
                "Large infestations need professional or agency help."
            ),
            prevent = listOf(
                "Clean, drain and dry boats, waders and gear between waters.",
                "Never dump aquarium or pond plants into waterways."
            ),
            chemical = listOf("Only products labeled for aquatic use, and in most places only applied by a licensed applicator with a permit."),
            disposal = listOf("Drag material well away from water, let it dry completely, then bag it or compost it far from any waterway."),
            followUp = listOf("Check every 2–4 weeks in the growing season; fragments establish quickly."),
            supplies = listOf("Rake or net", "Waders", "Bags", "Permit (if required)")
        )

        GENERIC -> Playbook(
            timing = "No specific profile for this plant yet. First make sure it really is a problem — many plants are native, beneficial, or legally protected. Your county extension office or a native plant society can confirm.",
            steps = listOf(
                "Work out its life cycle: does it spread only by seed (annual or biennial), by roots or runners (creeping perennial), or is it woody (shrub, vine, tree)? Then use the matching method.",
                "Small non-woody plants: dig out as much root as possible while the soil is moist.",
                "Woody plants: cut and dig out the root crown, or cut and treat the stump.",
                "Remove flowers and seed heads before they mature."
            ),
            prevent = listOf("Mulch and plant densely.", "Monitor the area regularly."),
            chemical = listOf("If needed, spot-treat with a product whose label lists this kind of plant, starting with the least toxic effective option."),
            disposal = listOf("Bag anything with seeds or spreading roots; compost the rest."),
            followUp = listOf("Check every 2–4 weeks for resprouts or seedlings."),
            supplies = listOf("Gloves", "Spade or weeder", "Bags")
        )
    }
}
