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

@Entity(tableName = "street_reminders")
@Serializable
data class StreetReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val reminderType: String = "meal_deadline", // meal_deadline | shelter_intake | hygiene_cutoff | task_due | custom | clinic_intake | weather
    val resourceId: Int? = null,
    val resourceName: String? = null,
    val taskId: Int? = null,
    val targetTimeText: String = "", // e.g. "1:30 PM"
    val leadMinutes: Int = 30, // alert X minutes before event
    val triggerHour: Int = 12, // 0..23
    val triggerMinute: Int = 0, // 0..59
    val daysOfWeek: List<Int> = emptyList(), // 1..7 (Calendar.SUNDAY..SATURDAY) or empty for every day / one-off
    val enabled: Boolean = true,
    val isSnoozed: Boolean = false,
    val snoozeUntilMillis: Long? = null,
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
    fun fromIntList(value: List<Int>): String = json.encodeToString(value)

    @TypeConverter
    fun toIntList(value: String): List<Int> = try {
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
data class StreetReminderBackup(
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val reminderType: String = "meal_deadline",
    val resourceId: Int? = null,
    val resourceName: String? = null,
    val targetTimeText: String = "",
    val leadMinutes: Int = 30,
    val triggerHour: Int = 12,
    val triggerMinute: Int = 0,
    val enabled: Boolean = true
)

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
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val exportedDateFormatted: String = "",
    val totalFavorites: Int = 0,
    val totalNotes: Int = 0,
    val totalVisits: Int = 0,
    val totalTasks: Int = 0,
    val totalReminders: Int = 0,
    val totalCustomPlaces: Int = 0
)

@Serializable
data class CompassBackup(
    val metadata: BackupMetadata = BackupMetadata(),
    val resources: List<ResourceUserDataBackup> = emptyList(),
    val rmpLocations: List<RmpUserDataBackup> = emptyList(),
    val visits: List<VisitBackup> = emptyList(),
    val tasks: List<TaskBackup> = emptyList(),
    val reminders: List<StreetReminderBackup> = emptyList(),
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
    val reminderCount: Int = 0,
    val customPlacesCount: Int,
    val rawBackup: CompassBackup
)

data class ImportResult(
    val favoritesUpdated: Int,
    val notesUpdated: Int,
    val visitsRestored: Int,
    val tasksRestored: Int,
    val remindersRestored: Int = 0,
    val customPlacesRestored: Int
)

// --- Granular Portability Selection (Chunk 5) ---
@Serializable
data class BackupEntitySelection(
    val includeFavorites: Boolean = true,
    val includeNotes: Boolean = true,
    val includeVisits: Boolean = true,
    val includeTasks: Boolean = true,
    val includeReminders: Boolean = true,
    val includeCustomPlaces: Boolean = true
) {
    val noneSelected: Boolean
        get() = !includeFavorites && !includeNotes && !includeVisits && !includeTasks && !includeReminders && !includeCustomPlaces

    val allSelected: Boolean
        get() = includeFavorites && includeNotes && includeVisits && includeTasks && includeReminders && includeCustomPlaces

    val countSelected: Int
        get() = listOf(includeFavorites, includeNotes, includeVisits, includeTasks, includeReminders, includeCustomPlaces).count { it }
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

// --- Reminders & Street Alerts Subsystem (Chunk 6) ---

enum class AlertUrgency {
    CRITICAL, // < 30 mins to closing or urgent lottery deadline
    WARNING,  // 30 - 60 mins to closing
    INFO      // Upcoming opening / daily briefing / weather
}

data class ActiveStreetAlert(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val message: String = "",
    val category: String = "meal", // meal | food | shelter | hygiene | health | weather | task
    val urgency: AlertUrgency = AlertUrgency.WARNING,
    val severity: String = "warning", // info | warning | critical
    val icon: String = "⏰",
    val cutoffTime: String? = null,
    val deadlineTimeText: String = "", // e.g. "Closes at 1:30 PM (in 25 mins)"
    val minutesRemaining: Int = 30,
    val locationName: String = "",
    val resourceName: String? = null,
    val neighborhood: String = "",
    val resourceId: Int? = null,
    val actionLabel: String = "View Details",
    val isReminderSet: Boolean = false
)

data class MorningBriefingData(
    val headline: String = "",
    val dateHeadline: String = "",
    val weatherSummary: String = "",
    val streetWeatherNotice: String = "",
    val openMealsCount: Int = 0,
    val pendingTasksCount: Int = 0,
    val openKeyServices: List<Resource> = emptyList(),
    val urgentTasks: List<Task> = emptyList(),
    val topMealRecommendation: Resource? = null,
    val topShelterNotice: String = "",
    val isColdWeatherActivated: Boolean = false
)

object StreetAlertEngine {

    // Well-known critical daily SF meal and intake cutoff rules
    private val KNOWN_CUTOFFS = listOf(
        StreetCutoffRule("St. Anthony's Free Lunch", "food", 10, 0, 13, 30, "Tenderloin", "Free daily sit-down lunch line cutoff at 1:30 PM (121 Golden Gate Ave)"),
        StreetCutoffRule("Glide Memorial Lunch", "food", 12, 0, 13, 0, "Tenderloin", "Daily free lunch meal service closes at 1:00 PM (330 Ellis St)"),
        StreetCutoffRule("Glide Memorial Dinner", "food", 16, 0, 17, 30, "Tenderloin", "Daily free dinner service closes at 5:30 PM (330 Ellis St)"),
        StreetCutoffRule("Martin de Porres Lunch", "food", 12, 0, 14, 0, "Potrero Hill", "Free hospitality soup kitchen lunch closes at 2:00 PM (225 Potrero Ave)"),
        StreetCutoffRule("City Hope Cafe Dinner", "food", 18, 0, 21, 30, "Tenderloin", "Free evening cafe dinner & safe community room closes at 9:30 PM"),
        StreetCutoffRule("MSC South Drop-In Triage", "shelter", 8, 0, 19, 0, "SoMa", "Multi-Service Center South evening intake lottery cutoff at 7:00 PM (525 5th St)"),
        StreetCutoffRule("NextDoor Shelter Intake", "shelter", 9, 0, 16, 0, "Tenderloin", "EPIC coordinated entry assessment intake closes at 4:00 PM (1001 Polk St)"),
        StreetCutoffRule("Larkin Street Youth Drop-In", "shelter", 9, 0, 17, 0, "Tenderloin", "Youth & TAY (ages 12-24) drop-in services & food intake cutoff at 5:00 PM"),
        StreetCutoffRule("A Woman's Place Drop-In", "shelter", 8, 0, 18, 0, "Mission", "Drop-in support & overnight intake triage closes at 6:00 PM (1049 Howard St)"),
        StreetCutoffRule("LavaMaeX Mobile Showers", "hygiene", 9, 0, 13, 30, "Civic Center", "Mobile shower trailer registration cutoff at 1:30 PM"),
        StreetCutoffRule("Mission Resource Center Showers", "hygiene", 7, 0, 16, 0, "Mission", "Drop-in shower list closes at 4:00 PM (165 Capp St)"),
        StreetCutoffRule("Tom Waddell Urban Health Triage", "health", 8, 30, 16, 30, "Civic Center", "Same-day urgent street health walk-in triage closes at 4:30 PM (230 Golden Gate Ave)")
    )

    data class StreetCutoffRule(
        val resourceName: String,
        val category: String,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
        val neighborhood: String,
        val description: String
    )

    fun evaluateActiveAlerts(
        allResources: List<Resource>,
        allReminders: List<StreetReminder> = emptyList(),
        allTasks: List<Task> = emptyList(),
        alertsMealCutoffsEnabled: Boolean = true,
        leadMinutes: Int = 30,
        alertsWeatherShelterEnabled: Boolean = true
    ): List<ActiveStreetAlert> {
        return calculateLiveStreetAlerts(
            resources = allResources,
            existingReminders = allReminders,
            tasks = allTasks,
            alertsMealCutoffsEnabled = alertsMealCutoffsEnabled,
            leadMinutes = leadMinutes,
            alertsWeatherShelterEnabled = alertsWeatherShelterEnabled
        )
    }

    fun calculateLiveStreetAlerts(
        resources: List<Resource>,
        existingReminders: List<StreetReminder> = emptyList(),
        tasks: List<Task> = emptyList(),
        alertsMealCutoffsEnabled: Boolean = true,
        leadMinutes: Int = 30,
        alertsWeatherShelterEnabled: Boolean = true,
        currentHour: Int? = null,
        currentMinute: Int? = null,
        currentDayOfWeek: Int? = null
    ): List<ActiveStreetAlert> {
        val calendar = java.util.Calendar.getInstance()
        val hour = currentHour ?: calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = currentMinute ?: calendar.get(java.util.Calendar.MINUTE)
        val dayOfWeek = currentDayOfWeek ?: calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val dayIndex = dayOfWeek - 1 // 0 (Sun) .. 6 (Sat)
        val currentTotalMinutes = hour * 60 + minute

        val alerts = mutableListOf<ActiveStreetAlert>()

        if (alertsMealCutoffsEnabled) {
            // 1. Evaluate known time-sensitive civic rules
            for (rule in KNOWN_CUTOFFS) {
                val endTotalMinutes = rule.endHour * 60 + rule.endMinute
                val minutesUntilClose = endTotalMinutes - currentTotalMinutes

                // If within max(leadMinutes + 30, 90) minutes of closing
                if (minutesUntilClose in 1..(leadMinutes + 45)) {
                    val matchedRes = resources.find { it.name.contains(rule.resourceName, ignoreCase = true) || rule.resourceName.contains(it.name, ignoreCase = true) }
                    val urgency = if (minutesUntilClose <= 30) AlertUrgency.CRITICAL else AlertUrgency.WARNING
                    val severity = if (minutesUntilClose <= 30) "critical" else "warning"
                    val formattedCloseTime = formatHourMinute(rule.endHour, rule.endMinute)
                    val isReminderSet = existingReminders.any { it.enabled && (it.resourceId == matchedRes?.id || it.title.contains(rule.resourceName, ignoreCase = true)) }

                    alerts.add(
                        ActiveStreetAlert(
                            id = "cutoff_${rule.resourceName.hashCode()}",
                            title = "${if (urgency == AlertUrgency.CRITICAL) "🚨 Closing Soon:" else "⏰ Upcoming Cutoff:"} ${rule.resourceName}",
                            subtitle = rule.description,
                            message = "${rule.description} Closes at $formattedCloseTime ($minutesUntilClose min remaining).",
                            category = rule.category,
                            urgency = urgency,
                            severity = severity,
                            icon = if (rule.category == "food") "🍱" else if (rule.category == "shelter") "🏠" else "⏰",
                            cutoffTime = formattedCloseTime,
                            deadlineTimeText = "Closes at $formattedCloseTime ($minutesUntilClose mins left)",
                            minutesRemaining = minutesUntilClose,
                            locationName = rule.resourceName,
                            resourceName = rule.resourceName,
                            neighborhood = rule.neighborhood,
                            resourceId = matchedRes?.id,
                            actionLabel = "View Resource",
                            isReminderSet = isReminderSet
                        )
                    )
                }
            }

            // 2. Evaluate database resources dynamic closing times
            for (res in resources) {
                if (res.status != "active" || res.hours.isEmpty()) continue
                // Avoid duplicate alerts already covered by known cutoffs
                if (alerts.any { it.resourceId == res.id || it.locationName.equals(res.name, ignoreCase = true) }) continue

                for (hb in res.hours) {
                    if (hb.day == dayIndex) {
                        val openTotal = parseTimeToMinutes(hb.open)
                        val closeTotal = parseTimeToMinutes(hb.close)

                        // If currently within operating hours and closing soon
                        if (closeTotal > openTotal && currentTotalMinutes in openTotal until closeTotal) {
                            val remaining = closeTotal - currentTotalMinutes
                            if (remaining in 1..leadMinutes && (res.category == "food" || res.category == "shelter" || res.category == "hygiene" || res.category == "health")) {
                                val urgency = if (remaining <= 30) AlertUrgency.CRITICAL else AlertUrgency.WARNING
                                val severity = if (remaining <= 30) "critical" else "warning"
                                val formattedClose = hb.close
                                val isReminder = existingReminders.any { it.enabled && it.resourceId == res.id }

                                alerts.add(
                                    ActiveStreetAlert(
                                        id = "res_close_${res.id}",
                                        title = "${if (urgency == AlertUrgency.CRITICAL) "🚨 Closing Soon:" else "⏰ Cutoff:"} ${res.name}",
                                        subtitle = if (res.summary.isNotBlank()) res.summary else "${res.category.replaceFirstChar { it.uppercase() }} in ${res.neighborhood}",
                                        message = "Service line closing at $formattedClose ($remaining min left) at ${res.address.ifBlank { res.neighborhood }}.",
                                        category = res.category,
                                        urgency = urgency,
                                        severity = severity,
                                        icon = if (res.category == "food") "🍱" else if (res.category == "shelter") "🏠" else "⏰",
                                        cutoffTime = formattedClose,
                                        deadlineTimeText = "Closes at $formattedClose ($remaining mins left)",
                                        minutesRemaining = remaining,
                                        locationName = res.name,
                                        resourceName = res.name,
                                        neighborhood = res.neighborhood,
                                        resourceId = res.id,
                                        actionLabel = "View Resource",
                                        isReminderSet = isReminder
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Severe Weather / Cold Night Shelter Activation Status Notice
        if (alertsWeatherShelterEnabled) {
            val isWinterSeason = calendar.get(java.util.Calendar.MONTH) in listOf(java.util.Calendar.NOVEMBER, java.util.Calendar.DECEMBER, java.util.Calendar.JANUARY, java.util.Calendar.FEBRUARY, java.util.Calendar.MARCH)
            if (isWinterSeason) {
                alerts.add(
                    ActiveStreetAlert(
                        id = "weather_cold_shelter",
                        title = "SF Cold Weather Protocol Active",
                        subtitle = "Expanded overnight shelter beds and warming centers active across Tenderloin & SoMa.",
                        message = "Emergency cold weather mats open overnight at NextDoor and MSC South.",
                        category = "weather",
                        urgency = AlertUrgency.INFO,
                        severity = "info",
                        icon = "❄️",
                        cutoffTime = "Overnight",
                        deadlineTimeText = "Overnight Intake Open",
                        minutesRemaining = 180,
                        locationName = "MSC South & NextDoor",
                        resourceName = "MSC South & NextDoor",
                        neighborhood = "Civic Center / SoMa",
                        actionLabel = "Shelter Info"
                    )
                )
            }
        }

        return alerts.sortedWith(compareBy({ it.urgency.ordinal }, { it.minutesRemaining }))
    }

    fun generateMorningBriefing(
        allResources: List<Resource> = emptyList(),
        allTasks: List<Task> = emptyList(),
        anchorNeighborhood: String = "Tenderloin",
        resources: List<Resource> = allResources,
        tasks: List<Task> = allTasks,
        preferredNeighborhood: String = anchorNeighborhood
    ): MorningBriefingData {
        val targetResources = if (allResources.isNotEmpty()) allResources else resources
        val targetTasks = if (allTasks.isNotEmpty()) allTasks else tasks
        val targetNeighborhood = if (anchorNeighborhood.isNotBlank()) anchorNeighborhood else preferredNeighborhood

        val calendar = java.util.Calendar.getInstance()
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val dayIndex = dayOfWeek - 1
        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val todayName = dayNames[dayOfWeek - 1]

        val openMeals = targetResources.filter { res ->
            res.category == "food" && res.status == "active" &&
            (res.hours.isEmpty() || res.hours.any { it.day == dayIndex })
        }

        val pendingTasks = targetTasks.filter { !it.done }

        // Find best meal recommendation in user's anchor neighborhood
        val topMeal = openMeals.find {
            it.neighborhood.equals(targetNeighborhood, ignoreCase = true) &&
            (it.name.contains("St. Anthony", ignoreCase = true) || it.name.contains("Glide", ignoreCase = true) || it.favorite)
        } ?: openMeals.firstOrNull()

        val isWinter = calendar.get(java.util.Calendar.MONTH) in listOf(java.util.Calendar.NOVEMBER, java.util.Calendar.DECEMBER, java.util.Calendar.JANUARY, java.util.Calendar.FEBRUARY, java.util.Calendar.MARCH)

        val keyServices = targetResources.filter {
            (it.category == "food" || it.category == "shelter" || it.category == "hygiene") &&
            it.status == "active" && (it.favorite || it.neighborhood.equals(targetNeighborhood, ignoreCase = true))
        }.take(6)

        return MorningBriefingData(
            headline = "$todayName Digest: ${openMeals.size} meal sites open",
            dateHeadline = "$todayName in San Francisco",
            weatherSummary = "Mild SF bay breeze • 61°F • Morning fog clearing to afternoon sun",
            streetWeatherNotice = "Morning fog clearing to mild 62°F. Afternoon west winds 14 mph.",
            openMealsCount = openMeals.size,
            pendingTasksCount = pendingTasks.size,
            openKeyServices = if (keyServices.isNotEmpty()) keyServices else openMeals.take(4),
            urgentTasks = pendingTasks,
            topMealRecommendation = topMeal,
            topShelterNotice = if (isWinter) "Cold weather shelter expansion active across SoMa/Tenderloin" else "Standard shelter lotteries open at 4:00 PM (NextDoor) & 7:00 PM (MSC South)",
            isColdWeatherActivated = isWinter
        )
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        if (timeStr.isBlank()) return 0
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    private fun formatHourMinute(hour: Int, minute: Int): String {
        val ampm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val minStr = if (minute < 10) "0$minute" else "$minute"
        return "$displayHour:$minStr $ampm"
    }
}


