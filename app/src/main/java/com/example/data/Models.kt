package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

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
@Serializable
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
enum class BackupGroup(
    val id: String,
    val title: String,
    val description: String,
    val icon: String
) {
    FAVORITES_NOTES("favorites_notes", "Favorites & Private Notes", "Saved favorite places and personal notes", "⭐"),
    VISITS("visits", "Visit History Logs", "Logged visits, outcomes, wait times, and ratings", "📌"),
    TASKS("tasks", "Checklist & Tasks", "To-do errands, appointments, and benefit checklists", "📋"),
    CUSTOM_PLACES("custom_places", "Custom Places", "Manually added or AI-extracted resources & locations", "📍"),
    CAPTURES("captures", "Flyer & Photo Captures", "Scanned document/flyer photos and text notes", "📷"),
    SETTINGS("settings", "Settings & Preferences", "App theme, font scaling, navigation, and AI settings", "⚙️"),
    RESOURCE_USER_DATA("resource_user_data", "Resource User Data", "Favorites and notes for directory resources", "🏢"),
    RMP_LOCATIONS("rmp_locations", "RMP Locations", "Restricted Meal Program locations and usage stats", "🍽️");

    companion object {
        fun fromId(id: String): BackupGroup? = entries.find { it.id.equals(id, ignoreCase = true) }
    }
}

@Serializable
data class ResourceUserDataBackup(
    val resourceId: Int? = null,
    val name: String,
    val address: String = "",
    val favorite: Boolean = false,
    val personalNotes: String = "",
    val createdVia: String = "manual",
    val source: String = ""
)

@Serializable
data class RmpUserDataBackup(
    val rmpId: Int? = null,
    val name: String,
    val address: String = "",
    val favorite: Boolean = false,
    val personalNotes: String = "",
    val timesUsed: Int = 0,
    val lastUsedAt: Long? = null,
    val createdVia: String = "manual",
    val source: String = ""
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
    val notes: String = "",
    val createdVia: String = "manual"
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
    val createdAt: Long = System.currentTimeMillis(),
    val createdVia: String = "manual"
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
    val totalCustomPlaces: Int = 0,
    val totalCaptures: Int = 0,
    val totalSettings: Int = 0,
    val includedGroups: List<String> = emptyList(),
    val provenanceSummary: Map<String, Int> = emptyMap()
)

@Serializable
data class CompassBackup(
    val metadata: BackupMetadata = BackupMetadata(),
    val resources: List<ResourceUserDataBackup> = emptyList(),
    val rmpLocations: List<RmpUserDataBackup> = emptyList(),
    val visits: List<VisitBackup> = emptyList(),
    val tasks: List<TaskBackup> = emptyList(),
    val customResources: List<Resource> = emptyList(),
    val customRmpLocations: List<RmpLocation> = emptyList(),
    val captures: List<Capture> = emptyList(),
    val settings: Map<String, String> = emptyMap()
)

data class ImportPreview(
    val app: String,
    val exportedDate: String,
    val favoriteCount: Int,
    val notesCount: Int,
    val visitCount: Int,
    val taskCount: Int,
    val customPlacesCount: Int,
    val captureCount: Int = 0,
    val settingsCount: Int = 0,
    val includedGroups: List<BackupGroup> = BackupGroup.entries,
    val rawBackup: CompassBackup
)

data class ImportResult(
    val favoritesUpdated: Int,
    val notesUpdated: Int,
    val visitsRestored: Int,
    val tasksRestored: Int,
    val customPlacesRestored: Int,
    val capturesRestored: Int = 0,
    val settingsRestored: Int = 0
)

data class PhotoCacheStats(
    val captureCount: Int = 0,
    val estimatedSizeKb: Long = 0,
    val hasPendingPhoto: Boolean = false
)

data class ProvenanceStats(
    val totalRecords: Int = 0,
    val seedCount: Int = 0,
    val userCreatedCount: Int = 0,
    val importedCount: Int = 0,
    val aiAssistedCount: Int = 0
)

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

// --- AI Navigator Customization & Preferences (Chunk 4) ---

@Serializable
enum class AiResponseStyle(
    val id: String,
    val title: String,
    val description: String,
    val badge: String
) {
    QUICK_STREET_ACTION(
        id = "quick_street_action",
        title = "Quick Street Action",
        description = "Concise, punchy 2-3 sentence directions with immediate next steps",
        badge = "⚡ Fast"
    ),
    STEP_BY_STEP_GUIDE(
        id = "step_by_step_guide",
        title = "Step-by-Step Guide",
        description = "Numbered chronological roadmap from arrival to intake",
        badge = "📋 Actionable"
    ),
    COMPREHENSIVE_CASEWORKER(
        id = "comprehensive_caseworker",
        title = "Comprehensive Caseworker Mode",
        description = "In-depth breakdown with document checklists, criteria, and alternative referrals",
        badge = "💼 Detailed"
    );

    companion object {
        val DEFAULT = QUICK_STREET_ACTION
        fun fromId(id: String?): AiResponseStyle =
            entries.find { it.id.equals(id?.trim(), ignoreCase = true) } ?: DEFAULT
    }
}

@Serializable
enum class AiConnectionMode(
    val id: String,
    val title: String,
    val description: String,
    val badge: String
) {
    AUTOMATIC(
        id = "automatic",
        title = "Automatic (Cloud + Offline Fallback)",
        description = "Uses Gemini AI when online with API key; gracefully falls back to local database matchers",
        badge = "✨ Auto"
    ),
    OFFLINE_ONLY(
        id = "offline_only",
        title = "Offline Only (Zero Data / Battery Saver)",
        description = "Bypasses external network calls entirely, using local keyword matching and deterministic rules",
        badge = "🛡️ Offline"
    );

    companion object {
        val DEFAULT = AUTOMATIC
        fun fromId(id: String?): AiConnectionMode =
            entries.find { it.id.equals(id?.trim(), ignoreCase = true) } ?: DEFAULT
    }
}

@Serializable
data class AiPreferences(
    val responseStyle: AiResponseStyle = AiResponseStyle.DEFAULT,
    val connectionMode: AiConnectionMode = AiConnectionMode.DEFAULT,
    val includeStreetTips: Boolean = true,
    val includeEligibilityDetails: Boolean = true
) {
    companion object {
        val DEFAULT = AiPreferences()
    }
}

// --- Resource Time & Open Status Helpers ---

fun isResourceOpen(open24: Boolean, hours: List<HourBlock>, calendar: Calendar = Calendar.getInstance()): Boolean {
    if (open24) return true
    if (hours.isEmpty()) return false
    val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Convert to 0=Mon, ..., 6=Sun
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val currentTimeMinutes = hour * 60 + minute

    for (block in hours) {
        val mappedDay = when (block.day) {
            0 -> 6 // Sun
            else -> block.day - 1
        }
        if (mappedDay == dayOfWeek) {
            val openParts = block.open.split(":")
            val closeParts = block.close.split(":")
            if (openParts.size >= 2 && closeParts.size >= 2) {
                val openMinutes = (openParts[0].toIntOrNull() ?: 0) * 60 + (openParts[1].toIntOrNull() ?: 0)
                val closeMinutes = (closeParts[0].toIntOrNull() ?: 0) * 60 + (closeParts[1].toIntOrNull() ?: 0)
                if (currentTimeMinutes in openMinutes..closeMinutes) {
                    return true
                }
            }
        }
    }
    return false
}

fun formatTime(timeStr: String): String {
    val parts = timeStr.split(":")
    if (parts.size < 2) return timeStr
    val hr = parts[0].toIntOrNull() ?: return timeStr
    val min = parts[1].toIntOrNull() ?: return timeStr
    val suffix = if (hr >= 12) "pm" else "am"
    val displayHr = when {
        hr == 0 -> 12
        hr > 12 -> hr - 12
        else -> hr
    }
    val displayMin = if (min == 0) "" else ":${min.toString().padStart(2, '0')}"
    return "$displayHr$displayMin$suffix"
}


