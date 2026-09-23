package com.example

import com.example.data.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ResourceRelevanceTest {

    private fun sampleResource(
        id: Int,
        name: String,
        category: String,
        neighborhood: String = "Tenderloin",
        tags: List<String> = emptyList(),
        alsoOffers: List<String> = emptyList(),
        lat: Double? = 37.7749,
        lng: Double? = -122.4194,
        confidence: String = "verified",
        favorite: Boolean = false,
        website: String = "https://example.org",
        phone: String = "415-555-0100"
    ): Resource {
        return Resource(
            id = id,
            name = name,
            category = category,
            alsoOffers = alsoOffers,
            summary = "Summary for $name",
            description = "Description for $name",
            address = "123 Market St",
            neighborhood = neighborhood,
            lat = lat,
            lng = lng,
            phone = phone,
            website = website,
            open24 = false,
            hours = emptyList(),
            confidence = confidence,
            status = "active",
            tags = tags,
            source = "ShelterTech SF Service Guide",
            favorite = favorite
        )
    }

    @Test
    fun testUserProfileDefaultsAndJsonRoundtrip() {
        val defaultProfile = UserProfile()
        assertTrue(defaultProfile.isEmpty)
        assertEquals(0, defaultProfile.activePreferenceCount)

        val customProfile = UserProfile(
            primaryNeeds = setOf("food", "hygiene"),
            demographics = setOf(DemographicPreset.YOUTH),
            dietary = setOf(DietaryPreset.VEG_VEGAN),
            accessibilityMobility = true,
            preferredNeighborhood = "Mission",
            preferredAccessMethods = setOf("walk_in"),
            preferredLanguages = setOf("Spanish")
        )

        assertFalse(customProfile.isEmpty)
        assertEquals(8, customProfile.activePreferenceCount)

        val json = Json { ignoreUnknownKeys = true }.encodeToString(customProfile)
        val restored = Json { ignoreUnknownKeys = true }.decodeFromString<UserProfile>(json)

        assertEquals(customProfile, restored)
    }

    @Test
    fun testPrimaryNeedBoosting() {
        val foodRes = sampleResource(1, "St. Anthony Dining Room", "food", alsoOffers = listOf("hygiene"))
        val shelterRes = sampleResource(2, "NextDoor Shelter", "shelter")
        val medicalRes = sampleResource(3, "Glide Health Clinic", "medical")

        val profile = UserProfile(primaryNeeds = setOf("food"))
        val ranked = ResourceRelevance.rankResources(listOf(shelterRes, medicalRes, foodRes), profile)

        assertEquals("St. Anthony Dining Room", ranked.first().name)
    }

    @Test
    fun testDemographicBoostingAffirmingResources() {
        val youthRes = sampleResource(1, "Larkin Street Youth Services", "community", tags = listOf("youth", "young_adult", "drop-in"))
        val generalRes = sampleResource(2, "General Resource Hub", "community")

        val profile = UserProfile(demographics = setOf(DemographicPreset.YOUTH))
        val ranked = ResourceRelevance.rankResources(listOf(generalRes, youthRes), profile)

        assertEquals("Larkin Street Youth Services", ranked.first().name)
    }

    @Test
    fun testDietaryBoosting() {
        val veganPantry = sampleResource(1, "Compass Veg Food Pantry", "food", tags = listOf("vegetarian", "vegan", "groceries"))
        val standardPantry = sampleResource(2, "Standard SF Pantry", "food", tags = listOf("canned_goods"))

        val profile = UserProfile(dietary = setOf(DietaryPreset.VEG_VEGAN))
        val ranked = ResourceRelevance.rankResources(listOf(standardPantry, veganPantry), profile)

        assertEquals("Compass Veg Food Pantry", ranked.first().name)
    }

    @Test
    fun testAccessibilityBoosting() {
        val accessibleClinic = sampleResource(1, "Tenderloin Community Health Center", "medical", tags = listOf("wheelchair", "accessible", "step-free"))
        val inaccessibleClinic = sampleResource(2, "Walkup Clinic", "medical", tags = listOf("stairs"))

        val profile = UserProfile(accessibilityMobility = true)
        val ranked = ResourceRelevance.rankResources(listOf(inaccessibleClinic, accessibleClinic), profile)

        assertEquals("Tenderloin Community Health Center", ranked.first().name)
    }

    @Test
    fun testNeighborhoodAnchorBoosting() {
        val missionHub = sampleResource(1, "Mission Resource Center", "community", neighborhood = "Mission")
        val somaHub = sampleResource(2, "SoMa Resource Center", "community", neighborhood = "SoMa")

        val profile = UserProfile(preferredNeighborhood = "Mission")
        val ranked = ResourceRelevance.rankResources(listOf(somaHub, missionHub), profile)

        assertEquals("Mission Resource Center", ranked.first().name)
    }

    @Test
    fun testNeverStrictlyExcludesResources() {
        val res1 = sampleResource(1, "General Service A", "community")
        val res2 = sampleResource(2, "General Service B", "shelter")
        val res3 = sampleResource(3, "Specialized C", "food", tags = listOf("veteran"))

        val profile = UserProfile(
            primaryNeeds = setOf("medical"),
            demographics = setOf(DemographicPreset.YOUTH),
            preferredNeighborhood = "Richmond"
        )

        val ranked = ResourceRelevance.rankResources(listOf(res1, res2, res3), profile)

        // All 3 resources are still returned in the list even if none match all profile boosts
        assertEquals(3, ranked.size)
        assertTrue(ranked.any { it.id == 1 })
        assertTrue(ranked.any { it.id == 2 })
        assertTrue(ranked.any { it.id == 3 })
    }
}

