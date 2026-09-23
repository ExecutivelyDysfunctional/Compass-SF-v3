package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class HourBlock(
    val day: Int,       // 0 (Sun) - 6 (Sat)
    val open: String,    // "HH:MM"
    val close: String,   // "HH:MM"
    val label: String? = null
)

@Serializable
data class PlanStop(
    val resourceId: Int?,
    val name: String,
    val time: String,
    val need: String,
    val why: String,
    val address: String,
    val phone: String,
    val tip: String
)

@Entity(tableName = "resources")
@Serializable
data class Resource(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String = "other",
    val alsoOffers: List<String> = emptyList(),
    val summary: String = "",
    val description: String = "",
    val address: String = "",
    val neighborhood: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val phone: String = "",
    val website: String = "",
    val hoursText: String = "",
    val hours: List<HourBlock> = emptyList(),
    val open24: Boolean = false,
    val requirements: List<String> = emptyList(),
    val bring: List<String> = emptyList(),
    val eligibility: String = "",
    val cost: String = "Free",
    val languages: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val confidence: String = "unverified", // unverified | reported | verified
    val status: String = "active",         // active | temporarily_closed | closed
    val favorite: Boolean = false,
    val hidden: Boolean = false,
    val phoneLine: Boolean = false,
    val personalNotes: String = "",
    val source: String = "",
    val createdVia: String = "manual",     // ai | manual | seed
    val aiTips: String = "",
    val lastVerifiedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "visits")
@Serializable
data class Visit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val resourceId: Int,
    val visitedAt: Long = System.currentTimeMillis(),
    val outcome: String = "got_help", // got_help | partial | turned_away | closed | just_looking
    val waitMinutes: Int? = null,
    val rating: Int? = null,
    val notes: String = ""
)

@Entity(tableName = "tasks")
@Serializable
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val notes: String = "",
    val kind: String = "errand", // appointment | errand | document | benefit | housing | health | other
    val dueAt: Long? = null,
    val resourceId: Int? = null,
    val done: Boolean = false,
    val priority: Int = 2,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "plans")
@Serializable
data class Plan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val forDate: String = "",
    val headline: String = "",
    val stops: List<PlanStop> = emptyList(),
    val generatedBy: String = "local", // local | ai
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "rmp_locations")
@Serializable
data class RmpLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String = "",
    val neighborhood: String = "",
    val zip: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val cuisine: String = "other", // pizza | chinese | burgers | chicken | halal etc.
    val chain: Boolean = false,
    val phone: String = "",
    val hoursText: String = "",
    val hours: List<HourBlock> = emptyList(),
    val open24: Boolean = false,
    val notes: String = "",
    val tips: String = "",
    val personalNotes: String = "",
    val confidence: String = "unverified", // sfhsa | reported | unverified
    val status: String = "active",         // active | closed | not_accepting
    val favorite: Boolean = false,
    val hidden: Boolean = false,
    val timesUsed: Int = 0,
    val lastUsedAt: Long? = null,
    val source: String = "",
    val createdVia: String = "manual",     // manual | seed
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "captures")
data class Capture(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rawText: String,
    val status: String = "pending", // pending | saved | dismissed
    val resourceId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// --- Type Converters for Room ---
class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun fromHourBlockList(value: List<HourBlock>): String = json.encodeToString(value)

    @TypeConverter
    fun toHourBlockList(value: String): List<HourBlock> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun fromPlanStopList(value: List<PlanStop>): String = json.encodeToString(value)

    @TypeConverter
    fun toPlanStopList(value: String): List<PlanStop> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }
}

// --- Backup and Restore Models ---

@Serializable
data class ResourceUserDataBackup(
    val resourceId: Int? = null,
    val name: String,
    val address: String = "",
    val favorite: Boolean = false,
    val personalNotes: String = ""
)

@Serializable
data class RmpUserDataBackup(
    val rmpId: Int? = null,
    val name: String,
    val address: String = "",
    val favorite: Boolean = false,
    val personalNotes: String = "",
    val timesUsed: Int = 0,
    val lastUsedAt: Long? = null
)

@Serializable
data class VisitBackup(
    val id: Int = 0,
    val resourceId: Int,
    val resourceName: String = "",
    val resourceAddress: String = "",
    val visitedAt: Long = System.currentTimeMillis(),
    val outcome: String = "got_help",
    val waitMinutes: Int? = null,
    val rating: Int? = null,
    val notes: String = ""
)

@Serializable
data class TaskBackup(
    val id: Int = 0,
    val title: String,
    val notes: String = "",
    val kind: String = "errand",
    val dueAt: Long? = null,
    val resourceId: Int? = null,
    val resourceName: String? = null,
    val done: Boolean = false,
    val priority: Int = 2,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class BackupMetadata(
    val app: String = "Compass SF",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val exportedDateFormatted: String = "",
    val totalFavorites: Int = 0,
    val totalNotes: Int = 0,
    val totalVisits: Int = 0,
    val totalTasks: Int = 0,
    val totalCustomPlaces: Int = 0
)

@Serializable
data class CompassBackup(
    val metadata: BackupMetadata = BackupMetadata(),
    val resources: List<ResourceUserDataBackup> = emptyList(),
    val rmpLocations: List<RmpUserDataBackup> = emptyList(),
    val visits: List<VisitBackup> = emptyList(),
    val tasks: List<TaskBackup> = emptyList(),
    val customResources: List<Resource> = emptyList(),
    val customRmpLocations: List<RmpLocation> = emptyList()
)

data class ImportPreview(
    val app: String,
    val exportedDate: String,
    val favoriteCount: Int,
    val notesCount: Int,
    val visitCount: Int,
    val taskCount: Int,
    val customPlacesCount: Int,
    val rawBackup: CompassBackup
)

data class ImportResult(
    val favoritesUpdated: Int,
    val notesUpdated: Int,
    val visitsRestored: Int,
    val tasksRestored: Int,
    val customPlacesRestored: Int
)

// --- Granular Portability Selection (Chunk 5) ---
@Serializable
data class BackupEntitySelection(
    val includeFavorites: Boolean = true,
    val includeNotes: Boolean = true,
    val includeVisits: Boolean = true,
    val includeTasks: Boolean = true,
    val includeCustomPlaces: Boolean = true
) {
    val noneSelected: Boolean
        get() = !includeFavorites && !includeNotes && !includeVisits && !includeTasks && !includeCustomPlaces

    val allSelected: Boolean
        get() = includeFavorites && includeNotes && includeVisits && includeTasks && includeCustomPlaces

    val countSelected: Int
        get() = listOf(includeFavorites, includeNotes, includeVisits, includeTasks, includeCustomPlaces).count { it }
}

// --- Photo Cache Manager Models (Chunk 5) ---
data class CachedPhotoInfo(
    val fileName: String,
    val absolutePath: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val formattedSize: String
)

// --- Factory Reseed Results (Chunk 5) ---
data class ReseedResult(
    val resourcesUpdated: Int,
    val resourcesAdded: Int,
    val rmpUpdated: Int,
    val rmpAdded: Int,
    val tasksChecked: Int
) {
    val totalChanges: Int
        get() = resourcesUpdated + resourcesAdded + rmpUpdated + rmpAdded
}

// --- Data Source Identifiers (Chunk 5) ---
enum class DirectorySource(
    val key: String,
    val displayName: String,
    val badge: String,
    val description: String,
    val icon: String
) {
    SHELTERTECH(
        key = "sheltertech",
        displayName = "SF Service Guide",
        badge = "Grassroots",
        description = "Curated directory by ShelterTech volunteer network & homeless advocates",
        icon = "🤝"
    ),
    DATASF(
        key = "datasf",
        displayName = "DataSF Open Data",
        badge = "Official City Data",
        description = "San Francisco municipal public health, food access, and clinic registry",
        icon = "🏛️"
    ),
    BAYAREA_211(
        key = "211",
        displayName = "211 Bay Area",
        badge = "Hotline Network",
        description = "Eden I&R health and human service database across the Bay Area",
        icon = "📞"
    ),
    COMMUNITY(
        key = "community",
        displayName = "Community Submissions",
        badge = "User Added",
        description = "Locally added resources and custom flyer intakes parsed on-device",
        icon = "📍"
    );

    companion object {
        fun fromKey(key: String): DirectorySource? = entries.find { it.key.equals(key, ignoreCase = true) }
    }
}

// --- Search, Accessibility & Demographic Presets (Chunk 3) ---

@Serializable
enum class DemographicPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val keywords: List<String>
) {
    YOUTH(
        id = "youth",
        title = "Youth & Young Adults",
        subtitle = "TAY under 25, drop-ins, shelters & education",
        icon = "🎒",
        keywords = listOf("youth", "tay", "young adult", "under 25", "transitional age", "larkin", "huckleberry", "lyric", "3rd street", "teen", "student")
    ),
    SENIORS(
        id = "seniors",
        title = "Seniors & Elders (60+)",
        subtitle = "Aging & adult services, senior dining & care",
        icon = "🧓",
        keywords = listOf("senior", "seniors", "elder", "elders", "60+", "aging", "adult day", "curry senior", "older adult", "golden gate senior")
    ),
    FAMILIES(
        id = "families",
        title = "Families with Children",
        subtitle = "Parents, children, family shelters & diaper banks",
        icon = "👨‍👩‍👧",
        keywords = listOf("family", "families", "children", "kids", "compass family", "hamilton family", "parent", "parents", "pregnant", "infant", "diaper", "child")
    ),
    VETERANS(
        id = "veterans",
        title = "Veterans",
        subtitle = "VA benefits, veteran housing & legal support",
        icon = "🎖️",
        keywords = listOf("veteran", "veterans", "va ", "military", "vet", "swords to plowshares", "vfw", "american legion")
    ),
    LGBTQ(
        id = "lgbtq",
        title = "LGBTQ+ Focused",
        subtitle = "Trans, queer, non-binary & affirming safe spaces",
        icon = "🏳️‍🌈",
        keywords = listOf("lgbtq", "lgbt", "trans", "transgender", "queer", "gay", "lesbian", "lyric", "san francisco aids foundation", "strut", "sfaf", "sf lagc", "api wellness")
    ),
    WOMEN_NB(
        id = "women_nb",
        title = "Women / Non-binary Only",
        subtitle = "Women's drop-ins, shelters, maternal care & DV support",
        icon = "👩",
        keywords = listOf("women", "woman", "non-binary", "female", "mary elizabeth", "wrc", "women's resource center", "rose", "domestic violence", "dv", "maternal", "mothers")
    );

    companion object {
        fun fromId(id: String): DemographicPreset? = entries.find { it.id.equals(id, ignoreCase = true) }
    }
}

@Serializable
enum class DietaryPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val keywords: List<String>
) {
    HALAL(
        id = "halal",
        title = "Halal",
        subtitle = "Certified Halal meals, pantries & Middle Eastern / Mediterranean",
        icon = "🥩",
        keywords = listOf("halal", "muslim", "middle eastern", "mediterranean", "kabob", "falafel", "shawarma", "islamic")
    ),
    VEG_VEGAN(
        id = "veg_vegan",
        title = "Vegetarian / Vegan",
        subtitle = "Plant-based dining, meatless meals & fresh produce pantries",
        icon = "🥗",
        keywords = listOf("vegetarian", "vegan", "plant-based", "meatless", "produce", "salad", "veggie", "vegetable", "fruit", "beans", "groceries")
    ),
    KOSHER(
        id = "kosher",
        title = "Kosher",
        subtitle = "Certified Kosher meals, delis & Jewish community food services",
        icon = "🥯",
        keywords = listOf("kosher", "jewish", "jcc", "deli", "synagogue", "hebrew")
    );

    companion object {
        fun fromId(id: String): DietaryPreset? = entries.find { it.id.equals(id, ignoreCase = true) }
    }
}

object PresetMatcher {
    private val ACCESSIBILITY_KEYWORDS = listOf(
        "accessible", "wheelchair", "step-free", "ground floor", "ground-floor",
        "elevator", "ramp", "ada", "mobility", "handicap", "flat entrance", "no stairs"
    )

    // Known central San Francisco community service sites with standard ground-floor / step-free street level entrances
    private val KNOWN_ACCESSIBLE_NAMES = listOf(
        "glide", "st. anthony", "mission neighborhood resource center", "sf-marin food bank",
        "mother brown", "curry senior", "compass family", "swords to plowshares",
        "hospitality house", "project open hand", "larkin street", "san francisco public library",
        "subway", "taco bell", "el farolito", "mc donald", "mcdonald", "carl's jr", "denny", "jack in the box"
    )

    fun matchesDemographic(resource: Resource, selectedPresets: Set<DemographicPreset>): Boolean {
        if (selectedPresets.isEmpty()) return true
        val fullText = "${resource.name} ${resource.summary} ${resource.description} ${resource.eligibility} ${resource.tags.joinToString(" ")} ${resource.requirements.joinToString(" ")} ${resource.alsoOffers.joinToString(" ")}".lowercase()
        return selectedPresets.any { preset ->
            preset.keywords.any { kw -> fullText.contains(kw.lowercase()) }
        }
    }

    fun matchesAccessibility(resource: Resource, mobilityModeEnabled: Boolean): Boolean {
        if (!mobilityModeEnabled) return true
        if (resource.phoneLine) return true

        val fullText = "${resource.name} ${resource.summary} ${resource.description} ${resource.requirements.joinToString(" ")} ${resource.tags.joinToString(" ")} ${resource.eligibility}".lowercase()
        val hasExplicitKeyword = ACCESSIBILITY_KEYWORDS.any { kw -> fullText.contains(kw) }
        val isKnownAccessible = KNOWN_ACCESSIBLE_NAMES.any { name -> resource.name.lowercase().contains(name) }
        
        // Return true if explicitly marked accessible, or known ground-floor street access
        return hasExplicitKeyword || isKnownAccessible || resource.tags.any { it.equals("accessible", ignoreCase = true) }
    }

    fun matchesDietary(resource: Resource, selectedDietary: Set<DietaryPreset>): Boolean {
        if (selectedDietary.isEmpty()) return true
        val fullText = "${resource.name} ${resource.summary} ${resource.description} ${resource.tags.joinToString(" ")} ${resource.requirements.joinToString(" ")}".lowercase()
        return selectedDietary.any { preset ->
            preset.keywords.any { kw -> fullText.contains(kw.lowercase()) }
        }
    }

    fun matchesRmpDemographic(location: RmpLocation, selectedPresets: Set<DemographicPreset>): Boolean {
        if (selectedPresets.isEmpty()) return true
        val fullText = "${location.name} ${location.notes} ${location.tips} ${location.cuisine} ${location.neighborhood}".lowercase()
        return selectedPresets.any { preset ->
            preset.keywords.any { kw -> fullText.contains(kw.lowercase()) }
        }
    }

    fun matchesRmpAccessibility(location: RmpLocation, mobilityModeEnabled: Boolean): Boolean {
        if (!mobilityModeEnabled) return true
        val fullText = "${location.name} ${location.notes} ${location.tips} ${location.address}".lowercase()
        val hasKeyword = ACCESSIBILITY_KEYWORDS.any { kw -> fullText.contains(kw) }
        val isKnownAccessible = KNOWN_ACCESSIBLE_NAMES.any { name -> location.name.lowercase().contains(name) }
        return hasKeyword || isKnownAccessible || location.chain // Chain stores almost universally have ADA ground-level entrances
    }

    fun matchesRmpDietary(location: RmpLocation, selectedDietary: Set<DietaryPreset>): Boolean {
        if (selectedDietary.isEmpty()) return true
        val fullText = "${location.name} ${location.cuisine} ${location.notes} ${location.tips}".lowercase()
        return selectedDietary.any { preset ->
            preset.keywords.any { kw -> fullText.contains(kw.lowercase()) }
        }
    }
}

fun Resource.isNonLocationOrHelpline(): Boolean {
    if (this.phoneLine) return true
    if (this.lat == null || this.lng == null) return true
    if (this.address.isBlank() ||
        this.address.equals("N/A", ignoreCase = true) ||
        this.address.contains("Phone", ignoreCase = true) ||
        this.address.contains("Helpline", ignoreCase = true) ||
        this.address.contains("Hotline", ignoreCase = true) ||
        this.address.contains("Online", ignoreCase = true) ||
        this.address.contains("Virtual", ignoreCase = true)
    ) return true
    val nonLocTags = setOf("helpline", "hotline", "phone", "virtual", "online", "non-location", "phoneline", "call-in", "remote")
    if (this.tags.any { tag -> nonLocTags.contains(tag.lowercase().trim()) }) return true
    return false
}

// --- Relevance Profile & Personalization Models ---

@Serializable
data class NeedCategory(
    val id: String,
    val title: String,
    val icon: String,
    val keywords: List<String> = emptyList()
)

@Serializable
data class AccessMethod(
    val id: String,
    val title: String,
    val icon: String,
    val description: String = ""
)

object ProfileConstants {
    val PRIMARY_NEEDS = listOf(
        NeedCategory("food", "Food & Meals", "🍱", listOf("food", "meal", "pantry", "groceries", "dining", "breakfast", "lunch", "dinner", "soup")),
        NeedCategory("shelter", "Shelter & Beds", "🏠", listOf("shelter", "housing", "beds", "night shelter", "mat", "navigation center")),
        NeedCategory("hygiene", "Hygiene & Showers", "🧼", listOf("shower", "hygiene", "restroom", "toilet", "laundry", "clean")),
        NeedCategory("health", "Medical & Dental", "🩺", listOf("clinic", "health", "medical", "doctor", "dental", "nurse", "prescription")),
        NeedCategory("mental", "Crisis & Mental Health", "🧠", listOf("mental", "crisis", "counseling", "therapy", "psychiatric", "substance", "harm reduction", "recovery")),
        NeedCategory("documents", "ID & Documents", "📄", listOf("id", "documents", "cal id", "birth certificate", "mail", "ssn", "dmv")),
        NeedCategory("benefits", "Benefits & EBT", "💳", listOf("ebt", "calfresh", "benefits", "general assistance", "ga", "medi-cal", "cash")),
        NeedCategory("legal", "Legal & Rights", "⚖️", listOf("legal", "lawyer", "attorney", "eviction", "tenant", "rights", "immigration", "court")),
        NeedCategory("work", "Jobs & Training", "💼", listOf("job", "employment", "work", "training", "resume", "vocational", "career")),
        NeedCategory("transportation", "Transportation", "🚌", listOf("transit", "bus", "bart", "muni", "clipper", "transportation", "ride", "token")),
        NeedCategory("pets", "Pets & Animal Care", "🐾", listOf("pet", "pets", "dog", "cat", "animal", "vet", "pet food")),
        NeedCategory("community", "Community & Drop-in", "🤝", listOf("community", "drop-in", "day center", "social", "support group", "activities"))
    )

    val ACCESS_METHODS = listOf(
        AccessMethod("walk_in", "Walk-in / In-Person", "🚶", "Drop in directly with no appointment required"),
        AccessMethod("phone", "Phone / Hotline", "📞", "Phone intakes, crisis helplines & telephone advice"),
        AccessMethod("website", "Online / Web Portal", "🌐", "Online applications, digital portals & web intakes")
    )

    val LANGUAGES = listOf(
        "English", "Spanish", "Cantonese", "Mandarin", "Tagalog", "Vietnamese", "Arabic", "Russian"
    )

    val NEIGHBORHOODS = listOf(
        "Tenderloin", "Mission", "SoMa", "Bayview", "Castro", "Chinatown",
        "Richmond", "Sunset", "Haight-Ashbury", "Western Addition", "Downtown / Civic Center"
    )
}

@Serializable
data class UserProfile(
    val primaryNeeds: Set<String> = emptySet(),
    val demographics: Set<DemographicPreset> = emptySet(),
    val dietary: Set<DietaryPreset> = emptySet(),
    val accessibilityMobility: Boolean = false,
    val preferredAccessMethods: Set<String> = emptySet(),
    val preferredNeighborhood: String = "",
    val preferredLanguages: Set<String> = emptySet()
) {
    val isEmpty: Boolean
        get() = primaryNeeds.isEmpty() &&
                demographics.isEmpty() &&
                dietary.isEmpty() &&
                !accessibilityMobility &&
                preferredAccessMethods.isEmpty() &&
                preferredNeighborhood.isBlank() &&
                preferredLanguages.isEmpty()

    val activePreferenceCount: Int
        get() {
            var count = primaryNeeds.size + demographics.size + dietary.size + preferredAccessMethods.size + preferredLanguages.size
            if (accessibilityMobility) count++
            if (preferredNeighborhood.isNotBlank()) count++
            return count
        }
}

data class RelevanceResult(
    val score: Int,
    val reasons: List<String> = emptyList()
)

object ResourceRelevance {
    fun scoreResource(resource: Resource, profile: UserProfile): RelevanceResult {
        var score = 100 // baseline score
        val reasons = mutableListOf<String>()

        // Status signal
        when (resource.status.lowercase()) {
            "active" -> score += 10
            "temporarily_closed" -> score -= 50
            "closed" -> score -= 200
        }

        // Verification signal
        when (resource.confidence.lowercase()) {
            "verified" -> {
                score += 15
                val lastVer = resource.lastVerifiedAt
                if (lastVer != null) {
                    val ageDays = (System.currentTimeMillis() - lastVer) / (1000L * 60 * 60 * 24)
                    if (ageDays < 30) score += 10
                    else if (ageDays < 90) score += 5
                }
            }
            "reported" -> score += 5
            "unverified" -> score += 0
        }

        if (resource.favorite) {
            score += 15
            reasons.add("Favorite")
        }

        if (profile.isEmpty) {
            return RelevanceResult(score, reasons)
        }

        val fullText = "${resource.name} ${resource.category} ${resource.summary} ${resource.description} ${resource.eligibility} ${resource.requirements.joinToString(" ")} ${resource.tags.joinToString(" ")} ${resource.alsoOffers.joinToString(" ")}".lowercase()

        // 1. Primary Needs / Categories Match
        if (profile.primaryNeeds.isNotEmpty()) {
            for (needId in profile.primaryNeeds) {
                val needObj = ProfileConstants.PRIMARY_NEEDS.find { it.id == needId }
                val directCategoryMatch = resource.category.equals(needId, ignoreCase = true) ||
                        resource.alsoOffers.any { it.equals(needId, ignoreCase = true) }
                val keywordMatch = needObj?.keywords?.any { fullText.contains(it.lowercase()) } == true
                if (directCategoryMatch || keywordMatch) {
                    score += 40
                    needObj?.let { reasons.add(it.title) }
                }
            }
        }

        // 2. Demographic Presets Match
        if (profile.demographics.isNotEmpty()) {
            for (demo in profile.demographics) {
                val matches = demo.keywords.any { fullText.contains(it.lowercase()) }
                if (matches) {
                    score += 30
                    reasons.add(demo.title)
                }
            }
        }

        // 3. Dietary Presets Match
        if (profile.dietary.isNotEmpty()) {
            for (diet in profile.dietary) {
                val matches = diet.keywords.any { fullText.contains(it.lowercase()) }
                if (matches) {
                    score += 25
                    reasons.add(diet.title)
                }
            }
        }

        // 4. Accessibility & Mobility
        if (profile.accessibilityMobility) {
            val isAcc = PresetMatcher.matchesAccessibility(resource, true)
            if (isAcc) {
                score += 25
                reasons.add("Accessible")
            }
        }

        // 5. Preferred Neighborhood
        if (profile.preferredNeighborhood.isNotBlank()) {
            val prefNh = profile.preferredNeighborhood.trim().lowercase()
            val resNh = resource.neighborhood.trim().lowercase()
            if (resNh.isNotBlank() && (resNh.contains(prefNh) || prefNh.contains(resNh))) {
                score += 35
                reasons.add(resource.neighborhood)
            }
        }

        // 6. Access Methods Match
        if (profile.preferredAccessMethods.isNotEmpty()) {
            if (profile.preferredAccessMethods.contains("walk_in") && !resource.phoneLine && resource.address.isNotBlank() && !resource.isNonLocationOrHelpline()) {
                score += 20
            }
            if (profile.preferredAccessMethods.contains("phone") && (resource.phoneLine || resource.phone.isNotBlank())) {
                score += 20
            }
            if (profile.preferredAccessMethods.contains("website") && resource.website.isNotBlank()) {
                score += 15
            }
        }

        // 7. Preferred Languages
        if (profile.preferredLanguages.isNotEmpty()) {
            val hasLangMatch = profile.preferredLanguages.any { lang ->
                resource.languages.any { it.equals(lang, ignoreCase = true) } ||
                fullText.contains(lang.lowercase())
            }
            if (hasLangMatch) {
                score += 25
                reasons.add("Language Match")
            }
        }

        return RelevanceResult(score, reasons.distinct())
    }

    fun rankResources(resources: List<Resource>, profile: UserProfile): List<Resource> {
        if (profile.isEmpty) {
            return resources.sortedByDescending { res ->
                var base = 0
                if (res.status == "active") base += 50
                if (res.favorite) base += 20
                if (res.confidence == "verified") base += 10
                base
            }
        }
        return resources.sortedByDescending { scoreResource(it, profile).score }
    }
}

