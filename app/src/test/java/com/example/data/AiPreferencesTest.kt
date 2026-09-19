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

    @Test
    fun testBuildSystemInstructionForDifferentStyles() {
        // Quick Street Action
        val quickPrefs = AiPreferences(responseStyle = AiResponseStyle.QUICK_STREET_ACTION, includeStreetTips = true, includeEligibilityDetails = false)
        val quickInstruction = AiService.buildSystemInstruction(quickPrefs)
        assertTrue(quickInstruction.contains("QUICK STREET ACTION"))
        assertTrue(quickInstruction.contains("STREET TIPS: ENABLED"))
        assertTrue(quickInstruction.contains("ELIGIBILITY DETAILS: DISABLED"))

        // Step By Step Guide
        val stepPrefs = AiPreferences(responseStyle = AiResponseStyle.STEP_BY_STEP_GUIDE, includeStreetTips = false, includeEligibilityDetails = true)
        val stepInstruction = AiService.buildSystemInstruction(stepPrefs)
        assertTrue(stepInstruction.contains("STEP-BY-STEP GUIDE"))
        assertTrue(stepInstruction.contains("STREET TIPS: DISABLED"))
        assertTrue(stepInstruction.contains("ELIGIBILITY DETAILS: ENABLED"))

        // Comprehensive Caseworker
        val casePrefs = AiPreferences(responseStyle = AiResponseStyle.COMPREHENSIVE_CASEWORKER, includeStreetTips = true, includeEligibilityDetails = true)
        val caseInstruction = AiService.buildSystemInstruction(casePrefs)
        assertTrue(caseInstruction.contains("COMPREHENSIVE CASEWORKER MODE"))
        assertTrue(caseInstruction.contains("STREET TIPS: ENABLED"))
        assertTrue(caseInstruction.contains("ELIGIBILITY DETAILS: ENABLED"))
    }

    @Test
    fun testBuildUserPrompt() {
        val sampleResource = Resource(
            id = 101,
            name = "St. Anthony Dining Room",
            category = "food",
            alsoOffers = listOf("clothing", "hygiene"),
            summary = "Hot sit-down meals for all",
            description = "Daily lunch service in Tenderloin",
            address = "121 Golden Gate Ave",
            neighborhood = "Tenderloin",
            phone = "(415) 241-2600",
            website = "https://stanthonysf.org",
            hoursText = "10:00 AM - 1:30 PM",
            hours = emptyList(),
            open24 = false,
            requirements = listOf("No ID required"),
            bring = listOf("Bags for takeaway if available"),
            eligibility = "All community members welcome",
            cost = "Free",
            languages = listOf("English", "Spanish"),
            tags = listOf("meals", "free lunch", "tenderloin"),
            aiTips = "Arrive before 11:30 AM to avoid the main line rush."
        )

        val promptWithTipsAndEligibility = AiService.buildUserPrompt(
            question = "Where can I get a hot lunch in the Tenderloin?",
            resources = listOf(sampleResource),
            neighborhoodFilter = "Tenderloin",
            openNowFilter = false,
            prefs = AiPreferences(
                responseStyle = AiResponseStyle.QUICK_STREET_ACTION,
                includeStreetTips = true,
                includeEligibilityDetails = true
            )
        )

        assertTrue(promptWithTipsAndEligibility.contains("Tenderloin"))
        assertTrue(promptWithTipsAndEligibility.contains("St. Anthony Dining Room"))
        assertTrue(promptWithTipsAndEligibility.contains("StreetTips: Arrive before 11:30 AM"))
        assertTrue(promptWithTipsAndEligibility.contains("No ID required"))

        val promptWithoutTips = AiService.buildUserPrompt(
            question = "Where can I get lunch?",
            resources = listOf(sampleResource),
            neighborhoodFilter = "",
            openNowFilter = false,
            prefs = AiPreferences(
                responseStyle = AiResponseStyle.STEP_BY_STEP_GUIDE,
                includeStreetTips = false,
                includeEligibilityDetails = false
            )
        )
        assertFalse(promptWithoutTips.contains("StreetTips:"))
        assertFalse(promptWithoutTips.contains("Requirements: No ID required"))
    }

    @Test
    fun testOfflineAskQuickStreetAction() {
        val testResources = listOf(
            Resource(
                id = 1,
                name = "GLIDE Daily Free Meals",
                category = "food",
                alsoOffers = listOf("shelter", "harm reduction"),
                summary = "3 free hot meals served daily 365 days a year",
                description = "Breakfast, lunch, and dinner walk-in service",
                address = "330 Ellis St",
                neighborhood = "Tenderloin",
                phone = "(415) 674-6000",
                website = "https://glide.org",
                hoursText = "Breakfast 8-9am, Lunch 12-1:30pm, Dinner 4-5:30pm",
                hours = emptyList(),
                open24 = false,
                requirements = emptyList(),
                bring = emptyList(),
                eligibility = "Open to everyone",
                cost = "Free",
                languages = listOf("English", "Spanish"),
                tags = listOf("meals", "food", "hot lunch"),
                aiTips = "Line forms along Ellis St 15 minutes before meal times."
            ),
            Resource(
                id = 2,
                name = "San Francisco Public Library Main Branch",
                category = "connect",
                alsoOffers = listOf("hygiene", "charging"),
                summary = "Public computers, restrooms, charging stations",
                description = "Central library with free wifi",
                address = "100 Larkin St",
                neighborhood = "Civic Center",
                phone = "(415) 557-4400",
                website = "https://sfpl.org",
                hoursText = "Mon-Sun 10am-6pm",
                hours = emptyList(),
                open24 = false,
                requirements = emptyList(),
                bring = emptyList(),
                eligibility = "Public",
                cost = "Free",
                languages = listOf("English", "Spanish", "Cantonese"),
                tags = listOf("wifi", "computer", "restrooms", "charging"),
                aiTips = "5th floor has quiet study pods and fast Wi-Fi."
            )
        )

        val quickResponse = AiService.runOfflineAsk(
            question = "Where can I get free meals in Tenderloin?",
            resources = testResources,
            neighborhoodFilter = "Tenderloin",
            openNowFilter = false,
            aiPreferences = AiPreferences(
                responseStyle = AiResponseStyle.QUICK_STREET_ACTION,
                includeStreetTips = true,
                includeEligibilityDetails = true
            )
        )

        assertTrue(quickResponse.answer.startsWith("Offline Action:"))
        assertTrue(quickResponse.answer.contains("GLIDE"))
        assertTrue(quickResponse.picks.isNotEmpty())
        assertEquals(1, quickResponse.picks.first().id)
        assertTrue(quickResponse.nextSteps.size <= 3)
        assertTrue(quickResponse.nextSteps.any { it.contains("GLIDE") })
    }

    @Test
    fun testOfflineAskStepByStepAndCaseworkerStyles() {
        val testResources = listOf(
            Resource(
                id = 1,
                name = "Medical Respite Center",
                category = "health",
                alsoOffers = listOf("shelter"),
                summary = "Post-hospital medical recovery and shelter beds",
                description = "Medical respite program for unhoused adults",
                address = "1171 Mission St",
                neighborhood = "SoMa",
                phone = "(415) 555-0199",
                website = "https://sf.gov/respite",
                hoursText = "24/7",
                hours = emptyList(),
                open24 = true,
                requirements = listOf("Hospital referral or street clinic triage"),
                bring = listOf("Discharge papers", "Medication list"),
                eligibility = "Adults recovering from acute illness",
                cost = "Free",
                languages = listOf("English"),
                tags = listOf("health", "medical", "clinic", "shelter"),
                aiTips = "Ring the doorbell at the side gate if arriving after 8 PM."
            )
        )

        // Step by Step
        val stepResponse = AiService.runOfflineAsk(
            question = "I need medical help and shelter in SoMa",
            resources = testResources,
            neighborhoodFilter = "SoMa",
            openNowFilter = false,
            aiPreferences = AiPreferences(
                responseStyle = AiResponseStyle.STEP_BY_STEP_GUIDE,
                includeStreetTips = true,
                includeEligibilityDetails = true
            )
        )
        assertTrue(stepResponse.answer.contains("Offline Step-by-Step Guide:"))
        assertTrue(stepResponse.nextSteps.any { it.contains("Arrival:") })
        assertTrue(stepResponse.nextSteps.any { it.contains("Check-in:") })
        assertTrue(stepResponse.nextSteps.any { it.contains("Documentation:") })
        assertTrue(stepResponse.nextSteps.any { it.contains("Navigation Tip:") })

        // Comprehensive Caseworker
        val caseResponse = AiService.runOfflineAsk(
            question = "I need medical help and shelter in SoMa",
            resources = testResources,
            neighborhoodFilter = "SoMa",
            openNowFilter = false,
            aiPreferences = AiPreferences(
                responseStyle = AiResponseStyle.COMPREHENSIVE_CASEWORKER,
                includeStreetTips = true,
                includeEligibilityDetails = true
            )
        )
        assertTrue(caseResponse.answer.contains("Offline Caseworker Assessment:"))
        assertTrue(caseResponse.nextSteps.any { it.contains("Primary Placement:") })
        assertTrue(caseResponse.nextSteps.any { it.contains("Intake & Eligibility:") })
        assertTrue(caseResponse.nextSteps.any { it.contains("Operational Advisory:") })
    }
}
