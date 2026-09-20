package com.example.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class Chunk5PortabilityAndPrivacyTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun testBackupGroupEnumCoverageAndFromId() {
        // Verify all 8 granular backup buckets are defined
        val expectedGroups = setOf(
            BackupGroup.FAVORITES_NOTES,
            BackupGroup.VISITS,
            BackupGroup.TASKS,
            BackupGroup.CUSTOM_PLACES,
            BackupGroup.CAPTURES,
            BackupGroup.SETTINGS,
            BackupGroup.RESOURCE_USER_DATA,
            BackupGroup.RMP_LOCATIONS
        )
        assertEquals(expectedGroups, BackupGroup.entries.toSet())

        // Test fromId for each
        for (group in BackupGroup.entries) {
            assertEquals(group, BackupGroup.fromId(group.id))
            assertEquals(group, BackupGroup.fromId(group.id.uppercase()))
        }

        // Unknown IDs return null gracefully
        assertNull(BackupGroup.fromId("unknown_group_id"))
        assertNull(BackupGroup.fromId(""))
    }

    @Test
    fun testCompassBackupSerializationRoundTrip() {
        val metadata = BackupMetadata(
            app = "Compass SF",
            version = 1,
            exportedAt = 1700000000000L,
            exportedDateFormatted = "Nov 14, 2023 at 10:00 AM",
            totalFavorites = 2,
            totalNotes = 1,
            totalVisits = 1,
            totalTasks = 1,
            totalCustomPlaces = 1,
            totalCaptures = 1,
            totalSettings = 2,
            includedGroups = listOf("favorites_notes", "tasks", "visits"),
            provenanceSummary = mapOf(
                "seed" to 10,
                "user" to 2,
                "imported" to 1,
                "ai" to 1
            )
        )

        val backup = CompassBackup(
            metadata = metadata,
            resources = listOf(
                ResourceUserDataBackup(
                    resourceId = 101,
                    name = "St. Anthony Dining Room",
                    favorite = true,
                    personalNotes = "Great warm lunches daily",
                    createdVia = "seed"
                )
            ),
            visits = listOf(
                VisitBackup(
                    id = 1,
                    resourceId = 101,
                    resourceName = "St. Anthony Dining Room",
                    visitedAt = 1700000000000L,
                    outcome = "got_help",
                    waitMinutes = 15,
                    rating = 5,
                    notes = "Fast intake process"
                )
            ),
            tasks = listOf(
                TaskBackup(
                    id = 1,
                    title = "Pick up lunch voucher",
                    notes = "Bring photo ID",
                    done = false,
                    priority = 1
                )
            ),
            settings = mapOf(
                "theme_mode" to "midnight",
                "font_scale" to "large"
            )
        )

        val encoded = json.encodeToString(backup)
        assertTrue(encoded.contains("\"app\": \"Compass SF\""))
        assertTrue(encoded.contains("\"St. Anthony Dining Room\""))
        assertTrue(encoded.contains("\"favorites_notes\""))

        val decoded = json.decodeFromString<CompassBackup>(encoded)
        assertEquals("Compass SF", decoded.metadata.app)
        assertEquals(1, decoded.resources.size)
        assertEquals("St. Anthony Dining Room", decoded.resources[0].name)
        assertTrue(decoded.resources[0].favorite)
        assertEquals(1, decoded.visits.size)
        assertEquals("got_help", decoded.visits[0].outcome)
        assertEquals(1, decoded.tasks.size)
        assertEquals("Pick up lunch voucher", decoded.tasks[0].title)
        assertEquals("midnight", decoded.settings["theme_mode"])
        assertEquals(4, decoded.metadata.provenanceSummary.size)
    }

    @Test
    fun testDeserializationWithMissingOrEmptyFields() {
        // Deserializing an empty JSON string should safely use all default values without crashing
        val emptyJson = "{}"
        val decoded = json.decodeFromString<CompassBackup>(emptyJson)

        assertEquals("Compass SF", decoded.metadata.app)
        assertEquals(1, decoded.metadata.version)
        assertTrue(decoded.resources.isEmpty())
        assertTrue(decoded.rmpLocations.isEmpty())
        assertTrue(decoded.visits.isEmpty())
        assertTrue(decoded.tasks.isEmpty())
        assertTrue(decoded.customResources.isEmpty())
        assertTrue(decoded.customRmpLocations.isEmpty())
        assertTrue(decoded.captures.isEmpty())
        assertTrue(decoded.settings.isEmpty())
    }

    @Test
    fun testDeserializationWithUnknownAndExtraFields() {
        // Ensures forward/backward compatibility when new fields are introduced
        val forwardCompatibleJson = """
            {
                "metadata": {
                    "app": "Compass SF Pro",
                    "futureFeatureFlag": true,
                    "unknownRatingMetric": 99.5
                },
                "unknownRootArray": [1, 2, 3],
                "resources": [
                    {
                        "name": "Mission Neighborhood Resource Center",
                        "favorite": true,
                        "unrecognizedField": "legacy"
                    }
                ]
            }
        """.trimIndent()

        val decoded = json.decodeFromString<CompassBackup>(forwardCompatibleJson)
        assertEquals("Compass SF Pro", decoded.metadata.app)
        assertEquals(1, decoded.resources.size)
        assertEquals("Mission Neighborhood Resource Center", decoded.resources[0].name)
        assertTrue(decoded.resources[0].favorite)
    }

    @Test
    fun testProvenanceTallyLogic() {
        var seed = 0
        var user = 0
        var imported = 0
        var ai = 0

        fun tally(via: String) {
            when (via.lowercase().trim()) {
                "seed" -> seed++
                "imported" -> imported++
                "ai" -> ai++
                else -> user++
            }
        }

        val testSources = listOf(
            "seed", "SEED", "seed ",
            "manual", "user", "", "custom",
            "imported", "IMPORTED",
            "ai", "AI"
        )

        testSources.forEach { tally(it) }

        assertEquals(3, seed)
        assertEquals(4, user)
        assertEquals(2, imported)
        assertEquals(2, ai)
    }

    @Test
    fun testPhotoCacheStatsCalculation() {
        val count = 5
        val stats = PhotoCacheStats(
            captureCount = count,
            estimatedSizeKb = count * 28L,
            hasPendingPhoto = count > 0
        )

        assertEquals(5, stats.captureCount)
        assertEquals(140L, stats.estimatedSizeKb)
        assertTrue(stats.hasPendingPhoto)

        val emptyStats = PhotoCacheStats(
            captureCount = 0,
            estimatedSizeKb = 0L,
            hasPendingPhoto = false
        )
        assertEquals(0, emptyStats.captureCount)
        assertEquals(0L, emptyStats.estimatedSizeKb)
        assertFalse(emptyStats.hasPendingPhoto)
    }

    @Test
    fun testRecentSearchListManagement() {
        // Simulates query recording and FIFO cap at 10 items
        val currentSearches = mutableListOf<String>()

        fun record(query: String, isIncognito: Boolean) {
            val trimmed = query.trim()
            if (trimmed.isBlank() || isIncognito) return
            currentSearches.remove(trimmed)
            currentSearches.add(0, trimmed)
            while (currentSearches.size > 10) {
                currentSearches.removeAt(currentSearches.size - 1)
            }
        }

        // Test normal recording
        record("shelter", false)
        record("food pantry", false)
        record("showers", false)
        assertEquals(listOf("showers", "food pantry", "shelter"), currentSearches)

        // Duplicate moves to front
        record("shelter", false)
        assertEquals(listOf("shelter", "showers", "food pantry"), currentSearches)

        // Incognito mode prevents persistence
        record("private query", true)
        assertFalse(currentSearches.contains("private query"))
        assertEquals(3, currentSearches.size)

        // Test 10-item cap
        for (i in 1..15) {
            record("query_$i", false)
        }
        assertEquals(10, currentSearches.size)
        assertEquals("query_15", currentSearches[0])
    }
}
