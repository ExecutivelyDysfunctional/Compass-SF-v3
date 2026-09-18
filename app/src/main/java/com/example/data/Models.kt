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
