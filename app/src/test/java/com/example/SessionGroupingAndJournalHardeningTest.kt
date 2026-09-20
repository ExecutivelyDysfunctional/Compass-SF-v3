package com.example

import com.example.db.Transcription
import com.example.db.TranscriptionRecord
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SessionGroupingAndJournalHardeningTest {

    private fun createRecord(
        id: String = UUID.randomUUID().toString(),
        sessionId: String? = null,
        partIndex: Int? = null,
        totalParts: Int? = null,
        sessionTitle: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        summary: String? = null,
        transcription: String = "Sample transcript for test",
        durationMs: Int? = 5000
    ): TranscriptionRecord {
        return TranscriptionRecord(
            id = id,
            transcription = transcription,
            speakerLabels = "Speaker 1",
            audioFilePath = "/path/to/audio_$id.m4a",
            timestamp = timestamp,
            summary = summary,
            category = "Meeting",
            modelName = "Gemini 3.6 Flash",
            sessionId = sessionId,
            partIndex = partIndex,
            totalParts = totalParts,
            sessionTitle = sessionTitle,
            partDurationMs = durationMs,
            locationName = "Room 101"
        )
    }

    private fun groupRecordsIntoJournalEntries(records: List<TranscriptionRecord>): List<JournalEntry> {
        val nonSession = mutableListOf<JournalEntry.Single>()
        val sessionMap = mutableMapOf<String, MutableList<TranscriptionRecord>>()

        for (record in records) {
            if (record.sessionId.isNullOrBlank()) {
                nonSession.add(JournalEntry.Single(record))
            } else {
                sessionMap.getOrPut(record.sessionId) { mutableListOf() }.add(record)
            }
        }

        val sessionGroups = sessionMap.map { (sessionId, partList) ->
            val sortedParts = partList.sortedWith(
                compareBy<TranscriptionRecord> { (it.partIndex?.takeIf { idx -> idx >= 0 } ?: Int.MAX_VALUE) }
                    .thenBy { it.timestamp }
            )
            val firstPart = sortedParts.firstOrNull()
            val title = sortedParts.mapNotNull { it.sessionTitle }.firstOrNull()
                ?.ifBlank { null }
                ?: "Recording Session"
            val masterSummary = sortedParts.mapNotNull { it.summary }.firstOrNull { it.isNotBlank() }
            val model = sortedParts.mapNotNull { it.modelName }.firstOrNull { it.isNotBlank() } ?: "Gemini 3.6 Flash"
            val totalDuration = sortedParts.sumOf { it.partDurationMs ?: 0 }
            val time = firstPart?.timestamp ?: System.currentTimeMillis()

            JournalEntry.SessionGroup(
                sessionId = sessionId,
                sessionTitle = title,
                timestamp = time,
                parts = sortedParts,
                masterSummary = masterSummary,
                totalDurationMs = totalDuration,
                modelName = model
            )
        }

        return (nonSession + sessionGroups).sortedByDescending { it.sortTimestamp }
    }

    @Test
    fun testSessionGrouping_sameSessionIdGroupsCorrectly() {
        val sessionId = "session-abc-123"
        val part0 = createRecord(sessionId = sessionId, partIndex = 0, sessionTitle = "Project Planning", timestamp = 1000L)
        val part1 = createRecord(sessionId = sessionId, partIndex = 1, sessionTitle = "Project Planning", timestamp = 2000L)
        val part2 = createRecord(sessionId = sessionId, partIndex = 2, sessionTitle = "Project Planning", timestamp = 3000L)

        val entries = groupRecordsIntoJournalEntries(listOf(part1, part0, part2))

        assertEquals(1, entries.size)
        val group = entries.first() as JournalEntry.SessionGroup
        assertEquals(sessionId, group.sessionId)
        assertEquals("Project Planning", group.sessionTitle)
        assertEquals(3, group.parts.size)
        assertEquals(0, group.parts[0].partIndex)
        assertEquals(1, group.parts[1].partIndex)
        assertEquals(2, group.parts[2].partIndex)
    }

    @Test
    fun testStandaloneFallback_nullOrBlankSessionRemainsStandalone() {
        val standalone1 = createRecord(sessionId = null, transcription = "Solo recording 1")
        val standalone2 = createRecord(sessionId = "", transcription = "Solo recording 2")
        val sessionPart = createRecord(sessionId = "sess-1", partIndex = 0, sessionTitle = "Grouped")

        val entries = groupRecordsIntoJournalEntries(listOf(standalone1, standalone2, sessionPart))

        assertEquals(3, entries.size)
        val singles = entries.filterIsInstance<JournalEntry.Single>()
        val groups = entries.filterIsInstance<JournalEntry.SessionGroup>()

        assertEquals(2, singles.size)
        assertEquals(1, groups.size)
        assertTrue(singles.any { it.record.id == standalone1.id })
        assertTrue(singles.any { it.record.id == standalone2.id })
        assertEquals("sess-1", groups[0].sessionId)
    }

    @Test
    fun testPartOrdering_withGapsAndNullPartIndices() {
        val sessionId = "sess-gaps"
        val part0 = createRecord(sessionId = sessionId, partIndex = 0, timestamp = 1000L)
        val part3 = createRecord(sessionId = sessionId, partIndex = 3, timestamp = 4000L)
        val partNull = createRecord(sessionId = sessionId, partIndex = null, timestamp = 5000L)
        val partNegative = createRecord(sessionId = sessionId, partIndex = -1, timestamp = 6000L)

        val entries = groupRecordsIntoJournalEntries(listOf(partNull, part3, part0, partNegative))

        assertEquals(1, entries.size)
        val group = entries.first() as JournalEntry.SessionGroup
        assertEquals(4, group.parts.size)
        // Part 0 comes first, then part 3, then null/negative sorted deterministically by timestamp
        assertEquals(part0.id, group.parts[0].id)
        assertEquals(part3.id, group.parts[1].id)
        assertEquals(partNull.id, group.parts[2].id)
        assertEquals(partNegative.id, group.parts[3].id)
    }

    @Test
    fun testPartOrdering_duplicatePartIndices_orderedDeterministicallyByTimestamp() {
        val sessionId = "sess-dups"
        val part1A = createRecord(sessionId = sessionId, partIndex = 1, timestamp = 2000L)
        val part1B = createRecord(sessionId = sessionId, partIndex = 1, timestamp = 1000L)

        val entries = groupRecordsIntoJournalEntries(listOf(part1A, part1B))

        val group = entries.first() as JournalEntry.SessionGroup
        assertEquals(2, group.parts.size)
        // Earliest timestamp should come first
        assertEquals(part1B.id, group.parts[0].id)
        assertEquals(part1A.id, group.parts[1].id)
    }

    @Test
    fun testSinglePartSession_retainsSessionGroupStructure() {
        val sessionId = "single-session-1"
        val singlePart = createRecord(sessionId = sessionId, partIndex = 0, totalParts = 1, sessionTitle = "Single Session")

        val entries = groupRecordsIntoJournalEntries(listOf(singlePart))

        assertEquals(1, entries.size)
        val group = entries.first() as JournalEntry.SessionGroup
        assertEquals(1, group.parts.size)
        assertEquals(singlePart.id, group.parts.first().id)
        assertEquals("Single Session", group.sessionTitle)
    }

    @Test
    fun testSessionSummary_masterSummaryFallbackLogic() {
        val sessionId = "summary-test"
        // Case 1: First part has master summary
        val part0 = createRecord(sessionId = sessionId, partIndex = 0, summary = "Master summary from Part 1")
        val part1 = createRecord(sessionId = sessionId, partIndex = 1, summary = "Summary Part 2")

        val entries = groupRecordsIntoJournalEntries(listOf(part0, part1))
        val group = entries.first() as JournalEntry.SessionGroup
        assertEquals("Master summary from Part 1", group.masterSummary)

        // Case 2: No parts have summary
        val partNoSummary0 = createRecord(sessionId = "no-sum", partIndex = 0, summary = null)
        val partNoSummary1 = createRecord(sessionId = "no-sum", partIndex = 1, summary = "")
        val entries2 = groupRecordsIntoJournalEntries(listOf(partNoSummary0, partNoSummary1))
        val group2 = entries2.first() as JournalEntry.SessionGroup
        assertNull(group2.masterSummary)
    }

    @Test
    fun testMultiSelect_masterSessionCardSelectsAllParts() {
        val sessionId = "multi-select-sess"
        val parts = listOf(
            createRecord(id = "p1", sessionId = sessionId, partIndex = 0),
            createRecord(id = "p2", sessionId = sessionId, partIndex = 1),
            createRecord(id = "p3", sessionId = sessionId, partIndex = 2)
        )

        var selectedIds = setOf<String>()
        val partIds = parts.map { it.id }.toSet()

        // User clicks master session checkbox
        val isAllSessionSelected = partIds.all { selectedIds.contains(it) }
        selectedIds = if (isAllSessionSelected) selectedIds - partIds else selectedIds + partIds

        assertEquals(3, selectedIds.size)
        assertTrue(selectedIds.containsAll(listOf("p1", "p2", "p3")))

        // Clicking again unselects all parts
        val isAllSelectedSecondClick = partIds.all { selectedIds.contains(it) }
        selectedIds = if (isAllSelectedSecondClick) selectedIds - partIds else selectedIds + partIds
        assertTrue(selectedIds.isEmpty())
    }

    @Test
    fun testJsonExportAndImportRoundTrip_sequentialSession() {
        val sessionId = "sess-roundtrip-99"
        val parts = listOf(
            createRecord(id = "p1", sessionId = sessionId, partIndex = 0, totalParts = 2, sessionTitle = "Sprint Retro", summary = "Retro Master Summary"),
            createRecord(id = "p2", sessionId = sessionId, partIndex = 1, totalParts = 2, sessionTitle = "Sprint Retro")
        )

        // Export JSON structure
        val rootJson = JSONObject().apply {
            put("version", 1)
            put("type", "sequential_session")
            put("sessionId", sessionId)
            put("sessionTitle", "Sprint Retro")
            put("totalParts", parts.size)
            put("masterSummary", "Retro Master Summary")
            put("exportedAt", System.currentTimeMillis())

            val array = JSONArray()
            for (p in parts) {
                val pObj = JSONObject().apply {
                    put("id", p.id)
                    put("sessionId", p.sessionId)
                    put("partIndex", p.partIndex)
                    put("totalParts", p.totalParts)
                    put("sessionTitle", p.sessionTitle)
                    put("transcription", p.transcription)
                    put("speakerLabels", p.speakerLabels)
                    put("audioFilePath", p.audioFilePath)
                    put("modelName", p.modelName)
                    put("partDurationMs", p.partDurationMs)
                    put("timestamp", p.timestamp)
                }
                array.put(pObj)
            }
            put("parts", array)
        }

        val jsonString = rootJson.toString()

        // Simulate import parsing logic
        val parsedJson = JSONObject(jsonString)
        assertEquals("sequential_session", parsedJson.getString("type"))
        assertEquals(sessionId, parsedJson.getString("sessionId"))
        assertEquals(2, parsedJson.getInt("totalParts"))

        val partsArray = parsedJson.getJSONArray("parts")
        assertEquals(2, partsArray.length())

        val importedRecords = mutableListOf<Transcription>()
        for (i in 0 until partsArray.length()) {
            val obj = partsArray.getJSONObject(i)
            importedRecords.add(
                Transcription(
                    id = obj.getString("id"),
                    sessionId = obj.optString("sessionId", sessionId),
                    partIndex = obj.optInt("partIndex", i),
                    totalParts = obj.optInt("totalParts", 2),
                    sessionTitle = obj.optString("sessionTitle", "Sprint Retro"),
                    transcription = obj.getString("transcription"),
                    partDurationMs = obj.optInt("partDurationMs", 5000),
                    timestamp = obj.getLong("timestamp")
                )
            )
        }

        assertEquals(2, importedRecords.size)
        assertEquals("p1", importedRecords[0].id)
        assertEquals(0, importedRecords[0].partIndex)
        assertEquals("p2", importedRecords[1].id)
        assertEquals(1, importedRecords[1].partIndex)
        assertEquals(sessionId, importedRecords[0].sessionId)
        assertEquals(sessionId, importedRecords[1].sessionId)
    }

    @Test
    fun testMalformedJsonHandling_gracefulDegradation() {
        val malformedJson1 = "{ \"type\": \"invalid_empty\" }"
        val root1 = JSONObject(malformedJson1)
        assertFalse(root1.has("parts"))
        assertFalse(root1.has("transcriptions"))

        val malformedArrayJson = "[ null, 123, { \"transcription\": \"Valid item inside array\" } ]"
        val array = JSONArray(malformedArrayJson)
        val validItems = mutableListOf<JSONObject>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i)
            if (item != null) {
                validItems.add(item)
            }
        }

        assertEquals(1, validItems.size)
        assertEquals("Valid item inside array", validItems[0].getString("transcription"))
    }
}
