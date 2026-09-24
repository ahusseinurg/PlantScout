package com.plantscout.app

/**
 * Names of common weeds and invasive plants, used to suggest names when a plant
 * is added by hand, and to turn what was typed into a proper Candidate.
 */
object PlantCatalog {

    data class Entry(val common: String, val scientific: String, val family: String, val native: Boolean = false) {
        /** Text shown in the suggestion list, including any alternate name. */
        val label: String get() = "$common — $scientific"
        /** Main common name, without the alternate name in brackets. */
        val primary: String get() = common.substringBefore(" (").trim()
        /** Alternate names written in brackets, e.g. "goathead" in "Puncturevine (goathead)". */
        val aliases: List<String> get() = Regex("\\(([^)]+)\\)").find(common)?.groupValues?.get(1)
            ?.split(',', '/')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }

    private fun e(common: String, scientific: String, family: String) = Entry(common, scientific, family)
    /** Native to the western / North American range — often valuable for wildlife. */
    private fun n(common: String, scientific: String, family: String) = Entry(common, scientific, family, native = true)

    val entries: List<Entry> = listOf(
        // Asteraceae
        e("Common dandelion", "Taraxacum officinale", "Asteraceae"),
        e("Canada thistle", "Cirsium arvense", "Asteraceae"),
        e("Bull thistle", "Cirsium vulgare", "Asteraceae"),
        e("Musk thistle", "Carduus nutans", "Asteraceae"),
        e("Scotch thistle", "Onopordum acanthium", "Asteraceae"),
        e("Spotted knapweed", "Centaurea stoebe", "Asteraceae"),
        e("Diffuse knapweed", "Centaurea diffusa", "Asteraceae"),
        e("Yellow starthistle", "Centaurea solstitialis", "Asteraceae"),
        e("Russian knapweed", "Rhaponticum repens", "Asteraceae"),
        e("Common burdock", "Arctium minus", "Asteraceae"),
        e("Greater burdock", "Arctium lappa", "Asteraceae"),
        e("Common tansy", "Tanacetum vulgare", "Asteraceae"),
        e("Tansy ragwort", "Jacobaea vulgaris", "Asteraceae"),
        n("Common ragweed", "Ambrosia artemisiifolia", "Asteraceae"),
        n("Giant ragweed", "Ambrosia trifida", "Asteraceae"),
        n("Horseweed", "Erigeron canadensis", "Asteraceae"),
        e("Orange hawkweed", "Pilosella aurantiaca", "Asteraceae"),
        e("Oxeye daisy", "Leucanthemum vulgare", "Asteraceae"),
        e("Chicory", "Cichorium intybus", "Asteraceae"),
        e("Prickly lettuce", "Lactuca serriola", "Asteraceae"),
        e("Common groundsel", "Senecio vulgaris", "Asteraceae"),
        e("Perennial sowthistle", "Sonchus arvensis", "Asteraceae"),
        // Legumes
        e("White clover", "Trifolium repens", "Fabaceae"),
        e("Black medic", "Medicago lupulina", "Fabaceae"),
        e("Black locust", "Robinia pseudoacacia", "Fabaceae"),
        e("Kudzu", "Pueraria montana", "Fabaceae"),
        e("Chinese wisteria", "Wisteria sinensis", "Fabaceae"),
        e("Japanese wisteria", "Wisteria floribunda", "Fabaceae"),
        // Bindweeds
        e("Field bindweed", "Convolvulus arvensis", "Convolvulaceae"),
        e("Hedge bindweed", "Calystegia sepium", "Convolvulaceae"),
        // Grasses and sedges
        e("Cheatgrass", "Bromus tectorum", "Poaceae"),
        e("Quackgrass", "Elymus repens", "Poaceae"),
        e("Bermudagrass", "Cynodon dactylon", "Poaceae"),
        e("Johnsongrass", "Sorghum halepense", "Poaceae"),
        e("Large crabgrass", "Digitaria sanguinalis", "Poaceae"),
        e("Smooth crabgrass", "Digitaria ischaemum", "Poaceae"),
        e("Annual bluegrass", "Poa annua", "Poaceae"),
        e("Green foxtail", "Setaria viridis", "Poaceae"),
        e("Yellow foxtail", "Setaria pumila", "Poaceae"),
        e("Barnyardgrass", "Echinochloa crus-galli", "Poaceae"),
        e("Goosegrass", "Eleusine indica", "Poaceae"),
        e("Longspine sandbur", "Cenchrus longispinus", "Poaceae"),
        e("Medusahead", "Taeniatherum caput-medusae", "Poaceae"),
        e("Common reed (Phragmites)", "Phragmites australis", "Poaceae"),
        e("Yellow nutsedge", "Cyperus esculentus", "Cyperaceae"),
        e("Purple nutsedge", "Cyperus rotundus", "Cyperaceae"),
        // Goosefoot / amaranth family
        e("Kochia", "Bassia scoparia", "Amaranthaceae"),
        e("Russian thistle (tumbleweed)", "Salsola tragus", "Amaranthaceae"),
        e("Redroot pigweed", "Amaranthus retroflexus", "Amaranthaceae"),
        e("Common lambsquarters", "Chenopodium album", "Amaranthaceae"),
        e("Halogeton", "Halogeton glomeratus", "Amaranthaceae"),
        e("Common purslane", "Portulaca oleracea", "Portulacaceae"),
        e("Puncturevine (goathead)", "Tribulus terrestris", "Zygophyllaceae"),
        // Mustards
        e("Whitetop (hoary cress)", "Lepidium draba", "Brassicaceae"),
        e("Perennial pepperweed", "Lepidium latifolium", "Brassicaceae"),
        e("Garlic mustard", "Alliaria petiolata", "Brassicaceae"),
        e("Dyer's woad", "Isatis tinctoria", "Brassicaceae"),
        e("Shepherd's purse", "Capsella bursa-pastoris", "Brassicaceae"),
        e("Flixweed", "Descurainia sophia", "Brassicaceae"),
        e("Tumble mustard", "Sisymbrium altissimum", "Brassicaceae"),
        // Spurges
        e("Leafy spurge", "Euphorbia esula", "Euphorbiaceae"),
        e("Myrtle spurge", "Euphorbia myrsinites", "Euphorbiaceae"),
        e("Spotted spurge", "Euphorbia maculata", "Euphorbiaceae"),
        e("Castor bean", "Ricinus communis", "Euphorbiaceae"),
        // Carrot family (several dangerous)
        e("Poison hemlock", "Conium maculatum", "Apiaceae"),
        n("Western water hemlock", "Cicuta douglasii", "Apiaceae"),
        n("Spotted water hemlock", "Cicuta maculata", "Apiaceae"),
        e("Wild parsnip", "Pastinaca sativa", "Apiaceae"),
        e("Giant hogweed", "Heracleum mantegazzianum", "Apiaceae"),
        e("Goutweed (bishop's weed)", "Aegopodium podagraria", "Apiaceae"),
        // Nightshades
        e("Jimsonweed", "Datura stramonium", "Solanaceae"),
        n("Sacred datura", "Datura wrightii", "Solanaceae"),
        n("Silverleaf nightshade", "Solanum elaeagnifolium", "Solanaceae"),
        n("Horsenettle", "Solanum carolinense", "Solanaceae"),
        e("Black nightshade", "Solanum nigrum", "Solanaceae"),
        n("Buffalobur", "Solanum rostratum", "Solanaceae"),
        // Knotweeds and docks
        e("Japanese knotweed", "Reynoutria japonica", "Polygonaceae"),
        e("Giant knotweed", "Reynoutria sachalinensis", "Polygonaceae"),
        e("Curly dock", "Rumex crispus", "Polygonaceae"),
        e("Broadleaf dock", "Rumex obtusifolius", "Polygonaceae"),
        e("Prostrate knotweed", "Polygonum aviculare", "Polygonaceae"),
        // Plantains, toadflax, mullein
        e("Broadleaf plantain", "Plantago major", "Plantaginaceae"),
        e("Buckhorn plantain", "Plantago lanceolata", "Plantaginaceae"),
        e("Dalmatian toadflax", "Linaria dalmatica", "Plantaginaceae"),
        e("Yellow toadflax (butter-and-eggs)", "Linaria vulgaris", "Plantaginaceae"),
        e("Common mullein", "Verbascum thapsus", "Scrophulariaceae"),
        // Other herbaceous weeds
        e("Creeping Charlie (ground ivy)", "Glechoma hederacea", "Lamiaceae"),
        e("Henbit", "Lamium amplexicaule", "Lamiaceae"),
        e("Stinging nettle", "Urtica dioica", "Urticaceae"),
        e("Cleavers", "Galium aparine", "Rubiaceae"),
        e("Common chickweed", "Stellaria media", "Caryophyllaceae"),
        e("Yellow woodsorrel", "Oxalis stricta", "Oxalidaceae"),
        e("Bermuda buttercup", "Oxalis pes-caprae", "Oxalidaceae"),
        e("Creeping buttercup", "Ranunculus repens", "Ranunculaceae"),
        e("Lesser celandine", "Ficaria verna", "Ranunculaceae"),
        e("Wild garlic", "Allium vineale", "Amaryllidaceae"),
        e("Field horsetail", "Equisetum arvense", "Equisetaceae"),
        e("St John's wort", "Hypericum perforatum", "Hypericaceae"),
        e("Houndstongue", "Cynoglossum officinale", "Boraginaceae"),
        // Shrubs
        e("Common buckthorn", "Rhamnus cathartica", "Rhamnaceae"),
        e("Glossy buckthorn", "Frangula alnus", "Rhamnaceae"),
        e("Amur honeysuckle", "Lonicera maackii", "Caprifoliaceae"),
        e("Tatarian honeysuckle", "Lonicera tatarica", "Caprifoliaceae"),
        e("Multiflora rose", "Rosa multiflora", "Rosaceae"),
        e("Himalayan blackberry", "Rubus armeniacus", "Rosaceae"),
        e("Russian olive", "Elaeagnus angustifolia", "Elaeagnaceae"),
        e("Autumn olive", "Elaeagnus umbellata", "Elaeagnaceae"),
        e("Tamarisk (saltcedar)", "Tamarix ramosissima", "Tamaricaceae"),
        e("Japanese barberry", "Berberis thunbergii", "Berberidaceae"),
        e("Common privet", "Ligustrum vulgare", "Oleaceae"),
        e("Oleander", "Nerium oleander", "Apocynaceae"),
        // Vines
        e("Japanese honeysuckle", "Lonicera japonica", "Caprifoliaceae"),
        e("English ivy", "Hedera helix", "Araliaceae"),
        e("Oriental bittersweet", "Celastrus orbiculatus", "Celastraceae"),
        n("Poison ivy", "Toxicodendron radicans", "Anacardiaceae"),
        n("Western poison ivy", "Toxicodendron rydbergii", "Anacardiaceae"),
        n("Pacific poison oak", "Toxicodendron diversilobum", "Anacardiaceae"),
        n("Poison sumac", "Toxicodendron vernix", "Anacardiaceae"),
        // Trees
        e("Tree of heaven", "Ailanthus altissima", "Simaroubaceae"),
        e("Siberian elm", "Ulmus pumila", "Ulmaceae"),
        e("Norway maple", "Acer platanoides", "Sapindaceae"),
        // Water and wetland
        e("Purple loosestrife", "Lythrum salicaria", "Lythraceae"),
        e("Water hyacinth", "Pontederia crassipes", "Pontederiaceae"),
        e("Eurasian watermilfoil", "Myriophyllum spicatum", "Haloragaceae"),
        n("Broadleaf cattail", "Typha latifolia", "Typhaceae"),
        // Western rangeland and Great Basin plants
        n("Yellow rabbitbrush (green rabbitbrush)", "Chrysothamnus viscidiflorus", "Asteraceae"),
        n("Rubber rabbitbrush", "Ericameria nauseosa", "Asteraceae"),
        n("Prairie sunflower", "Helianthus petiolaris", "Asteraceae"),
        n("Common sunflower", "Helianthus annuus", "Asteraceae"),
        n("Western ragweed (perennial ragweed)", "Ambrosia psilostachya", "Asteraceae"),
        n("Big sagebrush", "Artemisia tridentata", "Asteraceae"),
        n("Broom snakeweed", "Gutierrezia sarothrae", "Asteraceae"),
        n("Curlycup gumweed", "Grindelia squarrosa", "Asteraceae"),
        n("Common cocklebur", "Xanthium strumarium", "Asteraceae"),
        n("Poverty weed", "Iva axillaris", "Asteraceae"),
        e("Rush skeletonweed", "Chondrilla juncea", "Asteraceae"),
        e("Yellow salsify", "Tragopogon dubius", "Asteraceae"),
        n("Greasewood", "Sarcobatus vermiculatus", "Sarcobataceae"),
        n("Fourwing saltbush", "Atriplex canescens", "Amaranthaceae"),
        n("Showy milkweed", "Asclepias speciosa", "Apocynaceae"),
        e("Bur buttercup", "Ceratocephala testiculata", "Ranunculaceae"),
        e("Black henbane", "Hyoscyamus niger", "Solanaceae"),
        e("Redstem filaree (storksbill)", "Erodium cicutarium", "Geraniaceae"),
        e("Common mallow", "Malva neglecta", "Malvaceae"),
        n("Foxtail barley", "Hordeum jubatum", "Poaceae"),
        e("Clasping pepperweed", "Lepidium perfoliatum", "Brassicaceae"),
        n("Plains pricklypear", "Opuntia polyacantha", "Cactaceae")
    )

    private val catalogGenera: Set<String> by lazy { entries.map { it.scientific.substringBefore(' ').lowercase() }.toSet() }

    /** True when the first word is a genus PlantScout knows, so "ambrosia psilostachya" is read as a scientific name. */
    private fun knownGenus(g: String) = g.lowercase() in catalogGenera || KnowledgeBase.isKnownGenus(g)

    val labels: List<String> by lazy { entries.map { it.label } }

    private fun norm(s: String) = s.lowercase().replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

    /** Finds a catalog entry for typed text: a suggestion label, common name or scientific name. */
    fun find(text: String): Entry? {
        val t = text.trim()
        if (t.isEmpty()) return null
        entries.firstOrNull { it.label.equals(t, ignoreCase = true) }?.let { return it }
        val n = norm(t)
        val sciPart = Regex("\\(([^)]+)\\)").find(t)?.groupValues?.get(1)?.let { norm(it) }
        val beforeBracket = norm(t.substringBefore(" ("))
        return entries.firstOrNull { norm(it.scientific) == n || (sciPart != null && norm(it.scientific) == sciPart) }
            ?: entries.firstOrNull { norm(it.common) == n || norm(it.primary) == n }
            ?: entries.firstOrNull { e -> e.aliases.any { norm(it) == n } }
            ?: entries.firstOrNull { norm(it.primary) == beforeBracket }
    }

    /** Turns typed text into a Candidate marked as added manually. */
    fun toCandidate(text: String): Candidate {
        val hit = find(text)
        if (hit != null) {
            return Candidate(
                scientificName = hit.scientific,
                commonName = hit.primary,
                genus = hit.scientific.substringBefore(' '),
                family = hit.family,
                score = 1.0,
                source = Candidate.SOURCE_MANUAL
            )
        }
        // Not in the list: keep exactly what was typed. Any "(Genus species)" part is used as the scientific name.
        val clean = text.trim().replace(Regex("\\s+"), " ")
        val binomial = Regex("^([A-Za-z]+) ([A-Za-z-]+)( [A-Za-z.-]+)*$").find(clean)
        if (binomial != null && !clean.contains('(') && knownGenus(binomial.groupValues[1])) {
            // Looks like "Genus species" with a genus we know: tidy the capitalization.
            val words = clean.split(' ')
            val sci = words.first().lowercase().replaceFirstChar { it.uppercase() } + " " + words.drop(1).joinToString(" ") { it.lowercase() }
            return Candidate(
                scientificName = sci,
                commonName = "",
                genus = sci.substringBefore(' '),
                family = entries.firstOrNull { it.scientific.substringBefore(' ').equals(sci.substringBefore(' '), true) }?.family ?: "",
                score = 1.0,
                source = Candidate.SOURCE_MANUAL
            )
        }
        val sci = Regex("\\(([^)]+)\\)").find(clean)?.groupValues?.get(1)?.trim()
        val common = clean.substringBefore(" (").trim()
        return Candidate(
            scientificName = sci ?: common,
            commonName = common,
            genus = (sci ?: "").substringBefore(' '),
            family = "",
            score = 1.0,
            source = Candidate.SOURCE_MANUAL
        )
    }

    /** "Common (Scientific)", or just the name when both are the same. */
    fun displayLabel(c: Candidate): String =
        if (c.commonName.isNotBlank() && !c.commonName.equals(c.scientificName, ignoreCase = true))
            "${c.commonName} (${c.scientificName})"
        else c.displayName
}
