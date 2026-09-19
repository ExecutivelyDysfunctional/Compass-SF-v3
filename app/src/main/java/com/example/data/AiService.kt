package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// --- Gemini Request/Response Models ---

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@Serializable
data class ResponseFormatText(
    val mimeType: String,
    val schema: JsonObject? = null
)

@Serializable
data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>
)

@Serializable
data class Candidate(
    val content: Content
)

// --- Retrofit Setup ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

// --- High-Level AI Operations ---

object AiService {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parses messy raw text into structured resource parameters.
     */
    suspend fun parseMessyText(rawText: String): ResourceDraft? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return@withContext runOfflineParser(rawText)
        }

        val prompt = """
            Analyze the following text about a community resource, flyer, or message. Extract as much structure as possible.
            Ensure you parse hours of operation into structural day blocks.
            
            Text:
            $rawText
        """.trimIndent()

        val systemInstruction = """
            You are a helpful data analyst for Compass SF, a street guide for San Francisco.
            You must parse messy text and output a JSON matching the requested schema.
            Extract details like name, category (one of: "food", "shelter", "hygiene", "health", "mental", "documents", "benefits", "legal", "work", "connect", "storage", "transit", "pets", "community", "other"), address, neighborhood, phone, website, hoursText, structured hours (day is 0 to 6), cost (e.g., "Free"), requirements, and aiTips (smart tips for navigating this place).
        """.trimIndent()

        // Construct schema
        val schemaJson = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("name") { put("type", "STRING") }
                putJsonObject("category") { put("type", "STRING") }
                putJsonObject("alsoOffers") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
                putJsonObject("summary") { put("type", "STRING") }
                putJsonObject("description") { put("type", "STRING") }
                putJsonObject("address") { put("type", "STRING") }
                putJsonObject("neighborhood") { put("type", "STRING") }
                putJsonObject("phone") { put("type", "STRING") }
                putJsonObject("website") { put("type", "STRING") }
                putJsonObject("hoursText") { put("type", "STRING") }
                putJsonObject("hours") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("day") { put("type", "INTEGER") }
                            putJsonObject("open") { put("type", "STRING"); put("description", "HH:MM format") }
                            putJsonObject("close") { put("type", "STRING"); put("description", "HH:MM format") }
                            putJsonObject("label") { put("type", "STRING") }
                        }
                    }
                }
                putJsonObject("open24") { put("type", "BOOLEAN") }
                putJsonObject("requirements") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
                putJsonObject("bring") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
                putJsonObject("eligibility") { put("type", "STRING") }
                putJsonObject("cost") { put("type", "STRING") }
                putJsonObject("languages") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
                putJsonObject("tags") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
                putJsonObject("aiTips") { put("type", "STRING") }
            }
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(ResponseFormatText(mimeType = "application/json", schema = schemaJson)),
                temperature = 0.1f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext runOfflineParser(rawText)
            Log.d("AiService", "Parsed AI response: $jsonText")
            return@withContext json.decodeFromString<ResourceDraft>(jsonText)
        } catch (e: Exception) {
            Log.e("AiService", "Failed to parse via Gemini API, falling back to offline", e)
            return@withContext runOfflineParser(rawText)
        }
    }

    /**
     * Parses a flyer, clinic schedule, or document image into structured resource parameters using Gemini multimodal.
     */
    suspend fun parseFlyerImage(
        base64Image: String,
        mimeType: String = "image/jpeg",
        notes: String = ""
    ): ResourceDraft? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return@withContext runOfflineImageParser(notes)
        }

        val prompt = if (notes.isNotBlank()) {
            """
            Analyze this image of a community flyer, clinic schedule, street bulletin, or food pantry sign.
            Additional context provided by user: "$notes".
            Extract all key details (name, category, address, neighborhood, phone, website, schedule, cost, eligibility, documents to bring, requirements, and navigation tips).
            Ensure you parse any open hours into structured daily blocks (0=Sun, 1=Mon, ..., 6=Sat) with HH:MM format.
            """.trimIndent()
        } else {
            """
            Analyze this image of a community flyer, clinic schedule, street bulletin, or food pantry sign.
            Extract all key details (name, category, address, neighborhood, phone, website, schedule, cost, eligibility, documents to bring, requirements, and navigation tips).
            Ensure you parse any open hours into structured daily blocks (0=Sun, 1=Mon, ..., 6=Sat) with HH:MM format.
            """.trimIndent()
        }

        val systemInstruction = """
            You are an expert community data analyst and street navigator for Compass SF.
            You inspect street flyers, pantry schedules, clinic documents, and notices to build verified resource directory entries.
            Extract details accurately:
            - "name": Official organization or service name on the flyer.
            - "category": Choose best fit among: "food", "shelter", "hygiene", "health", "mental", "documents", "benefits", "legal", "work", "connect", "storage", "transit", "pets", "community", "other".
            - "alsoOffers": List of secondary services offered (e.g. showers, mail service, clothing).
            - "summary": Clear 1-sentence summary of what this program provides.
            - "description": Comprehensive description of services, instructions, and details.
            - "address": Full street address in San Francisco (e.g. "330 Ellis St, San Francisco, CA").
            - "neighborhood": SF neighborhood (e.g. Tenderloin, SoMa, Mission, Bayview, Chinatown, Civic Center, Castro, Richmond, Sunset).
            - "phone": Contact phone number if listed.
            - "website": Website or email if listed.
            - "hoursText": Plain English description of hours/schedule as written on flyer.
            - "hours": Structured array of day blocks (day: 0=Sun..6=Sat, open: "HH:MM", close: "HH:MM", label: e.g. "Lunch" or "Walk-in Clinic").
            - "open24": Boolean indicating 24/7 access.
            - "requirements": List of requirements (e.g. "ID required", "SF resident", "No intake required").
            - "bring": List of documents/items to bring (e.g. "Photo ID", "Proof of income").
            - "eligibility": Specific eligibility criteria or "Open to all".
            - "cost": "Free", "Sliding scale", or cost details.
            - "languages": Spoken/available languages (e.g. "English", "Spanish", "Cantonese").
            - "tags": Relevant keywords for search.
            - "aiTips": Street-smart advice for visitors (e.g. "Arrive 15 minutes before opening to get a ticket", "Lines form on Ellis St").
            Return only valid JSON matching the schema.
        """.trimIndent()

        val schemaJson = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("name") { put("type", "STRING") }
                putJsonObject("category") { put("type", "STRING") }
                putJsonObject("alsoOffers") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
                putJsonObject("summary") { put("type", "STRING") }
                putJsonObject("description") { put("type", "STRING") }
                putJsonObject("address") { put("type", "STRING") }
                putJsonObject("neighborhood") { put("type", "STRING") }
                putJsonObject("phone") { put("type", "STRING") }
                putJsonObject("website") { put("type", "STRING") }
                putJsonObject("hoursText") { put("type", "STRING") }
                putJsonObject("hours") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("day") { put("type", "INTEGER") }
                            putJsonObject("open") { put("type", "STRING"); put("description", "HH:MM format") }
                            putJsonObject("close") { put("type", "STRING"); put("description", "HH:MM format") }
                            putJsonObject("label") { put("type", "STRING") }
                        }
                    }
                }
                putJsonObject("open24") { put("type", "BOOLEAN") }
                putJsonObject("requirements") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
                putJsonObject("bring") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
                putJsonObject("eligibility") { put("type", "STRING") }
                putJsonObject("cost") { put("type", "STRING") }
                putJsonObject("languages") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
                putJsonObject("tags") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
                putJsonObject("aiTips") { put("type", "STRING") }
            }
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = mimeType, data = base64Image))
                    )
                )
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(ResponseFormatText(mimeType = "application/json", schema = schemaJson)),
                temperature = 0.1f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext runOfflineImageParser(notes)
            Log.d("AiService", "Parsed flyer image response: $jsonText")
            return@withContext json.decodeFromString<ResourceDraft>(jsonText)
        } catch (e: Exception) {
            Log.e("AiService", "Failed to parse image via Gemini API, falling back to offline", e)
            return@withContext runOfflineImageParser(notes)
        }
    }

    /**
     * Answers queries based on the available resources and picks matches.
     * Incorporates user's AiPreferences (response style, offline/auto mode, street tips, and eligibility details).
     */
    suspend fun askAi(
        question: String,
        resources: List<Resource>,
        neighborhoodFilter: String = "",
        openNowFilter: Boolean = false,
        aiPreferences: AiPreferences = AiPreferences.DEFAULT
    ): AskResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        // Strict Offline Mode guard or missing API key fallback
        if (aiPreferences.connectionMode == AiConnectionMode.OFFLINE_ONLY || apiKey.isBlank()) {
            return@withContext runOfflineAsk(
                question = question,
                resources = resources,
                neighborhoodFilter = neighborhoodFilter,
                openNowFilter = openNowFilter,
                aiPreferences = aiPreferences
            )
        }

        val prompt = buildUserPrompt(
            question = question,
            resources = resources,
            neighborhoodFilter = neighborhoodFilter,
            openNowFilter = openNowFilter,
            prefs = aiPreferences
        )

        val systemInstruction = buildSystemInstruction(aiPreferences)

        val schemaJson = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("answer") { put("type", "STRING") }
                putJsonObject("nextSteps") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
                putJsonObject("picks") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("id") { put("type", "INTEGER") }
                            putJsonObject("why") { put("type", "STRING") }
                        }
                    }
                }
            }
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(ResponseFormatText(mimeType = "application/json", schema = schemaJson)),
                temperature = 0.2f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext runOfflineAsk(question, resources, neighborhoodFilter, openNowFilter, aiPreferences)
            Log.d("AiService", "Ask AI response: $jsonText")
            val parsedResponse = json.decodeFromString<AskResponse>(jsonText)

            // Validate returned resource IDs
            val availableResourceIds = resources.map { it.id }.toSet()
            val validPicks = parsedResponse.picks.filter { pick -> availableResourceIds.contains(pick.id) }

            val finalPicks = if (validPicks.isNotEmpty()) {
                validPicks
            } else {
                // If all returned picks are hallucinated or empty, supply safe local ranked matches
                val fallbackMatches = rankLocalResources(question, resources, neighborhoodFilter, openNowFilter)
                fallbackMatches.take(3).map { (res, _) ->
                    PickDraft(
                        id = res.id,
                        why = buildOfflinePickWhy(res, question, aiPreferences)
                    )
                }
            }

            return@withContext parsedResponse.copy(picks = finalPicks)
        } catch (e: Exception) {
            Log.e("AiService", "Failed to ask AI, falling back to offline", e)
            return@withContext runOfflineAsk(question, resources, neighborhoodFilter, openNowFilter, aiPreferences)
        }
    }

    // --- Prompt and Instruction Builders ---

    fun buildSystemInstruction(prefs: AiPreferences): String {
        val styleInstruction = when (prefs.responseStyle) {
            AiResponseStyle.QUICK_STREET_ACTION -> """
                RESPONSE STYLE: QUICK STREET ACTION
                - Provide a very short, direct, and action-focused answer in 1-2 sentences maximum.
                - Limit "nextSteps" to at most 3 immediate, high-priority actions (e.g. where to walk right now, which entrance, arrival strategy).
                - Prioritize the immediate next move over background explanation.
            """.trimIndent()
            AiResponseStyle.STEP_BY_STEP_GUIDE -> """
                RESPONSE STYLE: STEP-BY-STEP GUIDE
                - Provide structured, chronological progression from start to finish.
                - In "nextSteps", provide clear, numbered sequential steps (e.g., Step 1: Transit/Arrival -> Step 2: Intake/Desk -> Step 3: Service Access).
                - Make the roadmap easy to follow on a mobile device for someone walking through the process in person.
            """.trimIndent()
            AiResponseStyle.COMPREHENSIVE_CASEWORKER -> """
                RESPONSE STYLE: COMPREHENSIVE CASEWORKER MODE
                - Provide detailed, structured, professional guidance with nuance, eligibility considerations, and logistical context.
                - In "nextSteps", include detailed intake procedures, documentation requirements, alternative backup referrals, and next-step reasoning.
                - Provide comprehensive triage to ensure the client is fully prepared before arriving.
            """.trimIndent()
        }

        val tipsInstruction = if (prefs.includeStreetTips) {
            "STREET TIPS: ENABLED. Include practical street-smart advice (arrival timing, line queuing, entrance location, safe navigation) in the guidance."
        } else {
            "STREET TIPS: DISABLED. Do NOT include informal street tips or arrival advice."
        }

        val eligibilityInstruction = if (prefs.includeEligibilityDetails) {
            "ELIGIBILITY DETAILS: ENABLED. Include specific intake criteria, ID requirements, proof of residency, and documents to bring in the guidance."
        } else {
            "ELIGIBILITY DETAILS: DISABLED. Omit detailed document checklists and eligibility criteria unless strictly required."
        }

        return """
            You are the street-smart AI navigator for Compass SF, an offline-first community resource guide for San Francisco.
            Answer the user's question directly, clearly, and with compassion.
            
            $styleInstruction
            
            $tipsInstruction
            $eligibilityInstruction
            
            Crucially, select the best matching resources from the provided list (picks "id" must exist in the provided resource context).
            Return a JSON object with fields:
            - "answer": string (conversational guidance tailored to the requested response style)
            - "nextSteps": array of strings (actionable steps tailored to the response style)
            - "picks": array of objects, each with "id" (Int) and "why" (String explanation of why this place is a strong match for their request).
        """.trimIndent()
    }

    fun buildUserPrompt(
        question: String,
        resources: List<Resource>,
        neighborhoodFilter: String,
        openNowFilter: Boolean,
        prefs: AiPreferences
    ): String {
        val contextText = resources.joinToString("\n") { r ->
            val tipsPart = if (prefs.includeStreetTips && r.aiTips.isNotBlank()) ", StreetTips: ${r.aiTips}" else ""
            val reqsPart = if (prefs.includeEligibilityDetails && (r.requirements.isNotEmpty() || r.bring.isNotEmpty() || r.eligibility.isNotBlank())) {
                ", Requirements: ${r.requirements.joinToString()}, Bring: ${r.bring.joinToString()}, Eligibility: ${r.eligibility}"
            } else ""
            
            "- ID: ${r.id}, Name: ${r.name}, Category: ${r.category}, AlsoOffers: ${r.alsoOffers.joinToString()}, Neighborhood: ${r.neighborhood}, Summary: ${r.summary}, Address: ${r.address}, Phone: ${r.phone}, Open24: ${r.open24}, Tags: ${r.tags.joinToString()}$tipsPart$reqsPart"
        }

        val neighborhoodLine = if (neighborhoodFilter.isNotBlank() && !neighborhoodFilter.equals("all", ignoreCase = true)) {
            "Selected Neighborhood Filter: \"$neighborhoodFilter\" (prioritize resources in or near this area)"
        } else {
            "Selected Neighborhood Filter: None (search across all San Francisco)"
        }

        val openNowLine = if (openNowFilter) {
            "Open Now Filter: Active (prioritize places open right now or with 24/7 access)"
        } else {
            "Open Now Filter: Inactive"
        }

        val styleLine = "Requested Response Style: ${prefs.responseStyle.title} (${prefs.responseStyle.description})"

        return """
            User Inquiry: "$question"
            $neighborhoodLine
            $openNowLine
            $styleLine
            
            Available San Francisco Community Resources:
            $contextText
        """.trimIndent()
    }

    // --- Offline Fallbacks & Match Ranking ---

    fun runOfflineParser(text: String): ResourceDraft {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val name = lines.firstOrNull() ?: "New Resource"
        var phone = ""
        var address = ""
        for (line in lines) {
            if (line.contains("(") || line.contains("-") && line.any { it.isDigit() }) {
                if (line.replace("[^\\d]".toRegex(), "").length >= 7) {
                    phone = line
                }
            }
            if (line.any { it.isDigit() } && (line.contains("Ave") || line.contains("St") || line.contains("Street"))) {
                address = line
            }
        }
        return ResourceDraft(
            name = name,
            category = "other",
            alsoOffers = emptyList(),
            summary = "Captured manually",
            description = text,
            address = address,
            neighborhood = "",
            phone = phone,
            website = "",
            hoursText = "",
            hours = emptyList(),
            open24 = false,
            requirements = emptyList(),
            bring = emptyList(),
            eligibility = "",
            cost = "Free",
            languages = emptyList(),
            tags = listOf("unverified", "manual"),
            aiTips = "Added via manual entry fallback."
        )
    }

    fun runOfflineImageParser(notes: String): ResourceDraft {
        return ResourceDraft(
            name = if (notes.isNotBlank()) notes.take(30) else "New Flyer Resource",
            category = "food",
            alsoOffers = emptyList(),
            summary = if (notes.isNotBlank()) notes else "Captured from flyer image",
            description = if (notes.isNotBlank()) "Notes: $notes\n(Captured via Flyer Photo Ingestion)" else "Captured from street flyer photo.",
            address = "San Francisco, CA",
            neighborhood = "Tenderloin",
            phone = "",
            website = "",
            hoursText = "Check flyer photo for schedule",
            hours = emptyList(),
            open24 = false,
            requirements = listOf("Check intake rules"),
            bring = listOf("ID if available"),
            eligibility = "Open to community",
            cost = "Free",
            languages = listOf("English", "Spanish"),
            tags = listOf("flyer", "photo-scan", "community"),
            aiTips = "Photo flyer saved. Please review and adjust hours or contact info if needed."
        )
    }

    fun rankLocalResources(
        question: String,
        resources: List<Resource>,
        neighborhoodFilter: String = "",
        openNowFilter: Boolean = false
    ): List<Pair<Resource, Int>> {
        val lowerQ = question.lowercase()
        val stopWords = setOf(
            "i", "need", "the", "a", "an", "in", "and", "or", "to", "for", "of", "some", "my",
            "is", "where", "can", "get", "find", "help", "me", "with", "want", "looking", "please"
        )
        val queryTokens = lowerQ.split(Regex("[^a-zA-Z0-9]+"))
            .map { it.trim() }
            .filter { it.length > 2 && it !in stopWords }

        val filterByNeighborhood = neighborhoodFilter.isNotBlank() && !neighborhoodFilter.equals("all", ignoreCase = true)

        return resources.map { r ->
            var score = 0
            val rNameLower = r.name.lowercase()
            val rCategoryLower = r.category.lowercase()
            val rSummaryLower = r.summary.lowercase()
            val rDescLower = r.description.lowercase()
            val rOffersLower = r.alsoOffers.joinToString(" ").lowercase()
            val rTagsLower = r.tags.joinToString(" ").lowercase()
            val rReqsLower = r.requirements.joinToString(" ").lowercase()
            val rNeighborhoodLower = r.neighborhood.lowercase()

            // 1. Exact or strong phrase match
            if (rNameLower.contains(lowerQ)) score += 25
            if (rCategoryLower.contains(lowerQ)) score += 15

            // 2. Token matches
            for (token in queryTokens) {
                if (rNameLower.contains(token)) score += 15
                if (rCategoryLower.contains(token)) score += 12
                if (rOffersLower.contains(token)) score += 10
                if (rTagsLower.contains(token)) score += 8
                if (rSummaryLower.contains(token)) score += 6
                if (rDescLower.contains(token)) score += 4
                if (rReqsLower.contains(token)) score += 3
            }

            // 3. Neighborhood filter relevance
            if (filterByNeighborhood) {
                if (rNeighborhoodLower.contains(neighborhoodFilter.lowercase())) {
                    score += 10
                } else {
                    score -= 8
                }
            }

            // 4. Open now relevance
            val isOpen = isResourceOpen(r.open24, r.hours)
            if (openNowFilter) {
                if (isOpen) {
                    score += 15
                } else {
                    score -= 10
                }
            } else if (isOpen) {
                score += 2
            }

            // 5. Verification & favorite bonus
            if (r.favorite) score += 3
            if (r.confidence == "verified") score += 2

            r to score
        }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
    }

    fun buildOfflinePickWhy(r: Resource, question: String, prefs: AiPreferences): String {
        val lowerQ = question.lowercase()
        val reason = when {
            lowerQ.contains(r.name.lowercase()) -> "Direct match for ${r.name}."
            r.category.isNotBlank() && lowerQ.contains(r.category.lowercase()) -> "Matches requested ${r.category} services in ${r.neighborhood}."
            r.alsoOffers.isNotEmpty() && r.alsoOffers.any { lowerQ.contains(it.lowercase()) } -> "Offers specialized services matching your inquiry."
            else -> "${r.summary.ifBlank { "Verified community resource in ${r.neighborhood}" }}."
        }

        val tipAddition = if (prefs.includeStreetTips && r.aiTips.isNotBlank()) {
            " Tip: ${r.aiTips}"
        } else {
            ""
        }

        return "$reason$tipAddition"
    }

    fun runOfflineAsk(
        question: String,
        resources: List<Resource>,
        neighborhoodFilter: String = "",
        openNowFilter: Boolean = false,
        aiPreferences: AiPreferences = AiPreferences.DEFAULT
    ): AskResponse {
        val ranked = rankLocalResources(question, resources, neighborhoodFilter, openNowFilter)
        val candidateResources = if (ranked.isNotEmpty()) {
            ranked.take(3).map { it.first }
        } else {
            // Fallback to resources matching active neighborhood/open filters or top emergency anchors
            val filtered = resources.filter { r ->
                val matchesN = neighborhoodFilter.isBlank() || neighborhoodFilter.equals("all", ignoreCase = true) || r.neighborhood.equals(neighborhoodFilter, ignoreCase = true)
                val matchesO = !openNowFilter || isResourceOpen(r.open24, r.hours)
                matchesN && matchesO
            }
            if (filtered.isNotEmpty()) filtered.take(3) else resources.take(3)
        }

        val topResource = candidateResources.firstOrNull()

        val picks = candidateResources.map { r ->
            PickDraft(
                id = r.id,
                why = buildOfflinePickWhy(r, question, aiPreferences)
            )
        }

        val answer: String
        val nextSteps: List<String>

        val areaLabel = if (neighborhoodFilter.isNotBlank() && !neighborhoodFilter.equals("all", ignoreCase = true)) {
            " in $neighborhoodFilter"
        } else {
            " in San Francisco"
        }

        when (aiPreferences.responseStyle) {
            AiResponseStyle.QUICK_STREET_ACTION -> {
                answer = if (topResource != null) {
                    "Offline Action: Head to ${topResource.name} at ${topResource.address}$areaLabel for ${topResource.category} services."
                } else {
                    "Offline Action: No matching San Francisco resources found matching your current filters."
                }

                val steps = mutableListOf<String>()
                if (topResource != null) {
                    steps.add("Go to ${topResource.name} at ${topResource.address}.")
                    if (aiPreferences.includeStreetTips && topResource.aiTips.isNotBlank()) {
                        steps.add("Street Tip: ${topResource.aiTips}")
                    }
                    if (aiPreferences.includeEligibilityDetails && topResource.bring.isNotEmpty()) {
                        steps.add("Bring: ${topResource.bring.joinToString(", ")}.")
                    } else if (aiPreferences.includeEligibilityDetails && topResource.requirements.isNotEmpty()) {
                        steps.add("Intake: ${topResource.requirements.joinToString(", ")}.")
                    }
                }
                if (steps.isEmpty()) {
                    steps.add("Adjust search filters or visit 330 Ellis St (GLIDE) for walk-in triage.")
                }
                nextSteps = steps.take(3)
            }

            AiResponseStyle.STEP_BY_STEP_GUIDE -> {
                answer = if (topResource != null) {
                    "Offline Step-by-Step Guide: Here is a structured roadmap for your inquiry$areaLabel."
                } else {
                    "Offline Step-by-Step Guide: Unable to locate a direct match for this inquiry. Follow general triage steps below."
                }

                val steps = mutableListOf<String>()
                if (topResource != null) {
                    steps.add("Arrival: Proceed to ${topResource.name} located at ${topResource.address} (${topResource.neighborhood}).")
                    steps.add("Check-in: Request ${topResource.category} assistance at the front desk or intake window.")
                    if (aiPreferences.includeEligibilityDetails && topResource.bring.isNotEmpty()) {
                        steps.add("Documentation: Present required items (${topResource.bring.joinToString(", ")}).")
                    }
                    if (aiPreferences.includeStreetTips && topResource.aiTips.isNotBlank()) {
                        steps.add("Navigation Tip: ${topResource.aiTips}")
                    }
                    if (candidateResources.size > 1) {
                        val backup = candidateResources[1]
                        steps.add("Backup Plan: If unavailable or at capacity, head to ${backup.name} at ${backup.address}.")
                    }
                } else {
                    steps.add("Step 1: Check your neighborhood and open-now filter toggles.")
                    steps.add("Step 2: Walk into GLIDE (330 Ellis St) or St. Anthony's (150 Golden Gate Ave) for multi-service walk-in intake.")
                    steps.add("Step 3: Call 211 for 24/7 San Francisco community directory guidance.")
                }
                nextSteps = steps
            }

            AiResponseStyle.COMPREHENSIVE_CASEWORKER -> {
                val countText = if (candidateResources.isNotEmpty()) "${candidateResources.size} resource match(es)" else "no direct records"
                val openStatusText = if (topResource != null) {
                    if (topResource.open24) "Offers 24/7 round-the-clock availability." else if (topResource.hoursText.isNotBlank()) "Scheduled hours: ${topResource.hoursText}." else "Confirm schedule upon arrival."
                } else ""

                answer = if (topResource != null) {
                    "Offline Caseworker Assessment: For inquiry \"$question\"$areaLabel, identified $countText. Primary referral is ${topResource.name} (${topResource.summary.ifBlank { topResource.description }}). $openStatusText"
                } else {
                    "Offline Caseworker Assessment: Inquiry \"$question\" yielded no exact local matches with current filter settings. Alternative emergency referrals have been prepared below."
                }

                val steps = mutableListOf<String>()
                if (topResource != null) {
                    steps.add("Primary Placement: Direct client/traveler to ${topResource.name} (${topResource.address}, ${topResource.neighborhood}).")
                    
                    if (aiPreferences.includeEligibilityDetails) {
                        val reqDetails = mutableListOf<String>()
                        if (topResource.eligibility.isNotBlank()) reqDetails.add("Criteria: ${topResource.eligibility}")
                        if (topResource.requirements.isNotEmpty()) reqDetails.add("Requirements: ${topResource.requirements.joinToString()}")
                        if (topResource.bring.isNotEmpty()) reqDetails.add("Documents to Bring: ${topResource.bring.joinToString()}")
                        if (reqDetails.isNotEmpty()) {
                            steps.add("Intake & Eligibility: ${reqDetails.joinToString(" • ")}")
                        }
                    }

                    if (aiPreferences.includeStreetTips && topResource.aiTips.isNotBlank()) {
                        steps.add("Operational Advisory: ${topResource.aiTips}")
                    }

                    if (candidateResources.size > 1) {
                        val secondary = candidateResources[1]
                        steps.add("Secondary Referral: Alternative referral at ${secondary.name} (${secondary.address}) in case primary intake is closed or at capacity.")
                    }
                } else {
                    steps.add("Triage: Broaden search criteria or reset category and neighborhood filters.")
                    steps.add("Centralized Intake: Utilize SF Service Guide or visit central multi-service centers (GLIDE or St. Anthony's).")
                    steps.add("Verification: Log any updated hours or service availability in the Compass SF directory.")
                }
                nextSteps = steps
            }
        }

        return AskResponse(
            answer = answer,
            nextSteps = nextSteps,
            picks = picks
        )
    }
}

// --- Serialized Draft Classes ---

@Serializable
data class ResourceDraft(
    val name: String = "",
    val category: String = "other",
    val alsoOffers: List<String> = emptyList(),
    val summary: String = "",
    val description: String = "",
    val address: String = "",
    val neighborhood: String = "",
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
    val aiTips: String = ""
)

@Serializable
data class AskResponse(
    val answer: String,
    val nextSteps: List<String> = emptyList(),
    val picks: List<PickDraft> = emptyList()
)

@Serializable
data class PickDraft(
    val id: Int,
    val why: String
)
