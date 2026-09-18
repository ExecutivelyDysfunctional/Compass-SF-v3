package com.example.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AiPreferencesTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun testDefaultPreferences() {
        val prefs = AiPreferences.DEFAULT
        assertEquals(AiResponseStyle.QUICK_STREET_ACTION, prefs.responseStyle)
        assertEquals(AiConnectionMode.AUTOMATIC, prefs.connectionMode)
        assertTrue(prefs.includeStreetTips)
        assertTrue(prefs.includeEligibilityDetails)
    }

    @Test
    fun testEnumFromIdValid() {
        // AiResponseStyle
        assertEquals(AiResponseStyle.QUICK_STREET_ACTION, AiResponseStyle.fromId("quick_street_action"))
        assertEquals(AiResponseStyle.STEP_BY_STEP_GUIDE, AiResponseStyle.fromId("step_by_step_guide"))
        assertEquals(AiResponseStyle.COMPREHENSIVE_CASEWORKER, AiResponseStyle.fromId("comprehensive_caseworker"))

        // Case-insensitive & trimmed parsing
        assertEquals(AiResponseStyle.STEP_BY_STEP_GUIDE, AiResponseStyle.fromId(" STEP_BY_STEP_GUIDE "))

        // AiConnectionMode
        assertEquals(AiConnectionMode.AUTOMATIC, AiConnectionMode.fromId("automatic"))
        assertEquals(AiConnectionMode.OFFLINE_ONLY, AiConnectionMode.fromId("offline_only"))
        assertEquals(AiConnectionMode.OFFLINE_ONLY, AiConnectionMode.fromId(" OFFLINE_ONLY "))
    }

    @Test
    fun testEnumFromIdSafeFallback() {
        // Invalid IDs fall back to safe defaults without throwing exceptions
        assertEquals(AiResponseStyle.QUICK_STREET_ACTION, AiResponseStyle.fromId(null))
        assertEquals(AiResponseStyle.QUICK_STREET_ACTION, AiResponseStyle.fromId(""))
        assertEquals(AiResponseStyle.QUICK_STREET_ACTION, AiResponseStyle.fromId("unknown_style_xyz"))

        assertEquals(AiConnectionMode.AUTOMATIC, AiConnectionMode.fromId(null))
        assertEquals(AiConnectionMode.AUTOMATIC, AiConnectionMode.fromId(""))
        assertEquals(AiConnectionMode.AUTOMATIC, AiConnectionMode.fromId("corrupted_connection_mode"))
    }

    @Test
    fun testSerializationAndDeserialization() {
        val original = AiPreferences(
            responseStyle = AiResponseStyle.COMPREHENSIVE_CASEWORKER,
            connectionMode = AiConnectionMode.OFFLINE_ONLY,
            includeStreetTips = false,
            includeEligibilityDetails = true
        )

        val serialized = json.encodeToString(original)
        assertTrue(serialized.contains("comprehensive_caseworker") || serialized.contains("COMPREHENSIVE_CASEWORKER"))

        val deserialized = json.decodeFromString<AiPreferences>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun testUpdatingAndRestoringPreferences() {
        var current = AiPreferences.DEFAULT

        // Update response style
        current = current.copy(responseStyle = AiResponseStyle.STEP_BY_STEP_GUIDE)
        assertEquals(AiResponseStyle.STEP_BY_STEP_GUIDE, current.responseStyle)
        assertEquals(AiConnectionMode.AUTOMATIC, current.connectionMode)

        // Update connection mode
        current = current.copy(connectionMode = AiConnectionMode.OFFLINE_ONLY)
        assertEquals(AiConnectionMode.OFFLINE_ONLY, current.connectionMode)

        // Toggle flags
        current = current.copy(includeStreetTips = false, includeEligibilityDetails = false)
        assertFalse(current.includeStreetTips)
        assertFalse(current.includeEligibilityDetails)

        // Reset to default
        current = AiPreferences.DEFAULT
        assertEquals(AiResponseStyle.QUICK_STREET_ACTION, current.responseStyle)
        assertEquals(AiConnectionMode.AUTOMATIC, current.connectionMode)
        assertTrue(current.includeStreetTips)
        assertTrue(current.includeEligibilityDetails)
    }
}
