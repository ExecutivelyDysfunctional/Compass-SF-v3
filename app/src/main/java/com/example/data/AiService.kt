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
    val text: String? = null
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
        if (apiKey.isEmpty() || apiKey.startsWith("AQ.")) {
            // Wait, if it's the default placeholder key or empty, let's run the offline parser as fallback
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
     * Answers queries based on the available resources and picks matches.
     */
    suspend fun askAi(
        question: String,
        resources: List<Resource>,
        neighborhoodFilter: String,
        openNowFilter: Boolean
    ): AskResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey.startsWith("AQ.")) {
            return@withContext runOfflineAsk(question, resources)
        }

        // Prepare context
        val contextText = resources.joinToString("\n") { r ->
            "- ID: ${r.id}, Name: ${r.name}, Category: ${r.category}, Neighborhood: ${r.neighborhood}, Summary: ${r.summary}, Address: ${r.address}, Phone: ${r.phone}, Open24: ${r.open24}, Requirements: ${r.requirements.joinToString()}, Tags: ${r.tags.joinToString()}"
        }

        val prompt = """
            User Question: "$question"
            Selected Neighborhood Filter: "$neighborhoodFilter"
            Open Now Only: $openNowFilter
            
            Here is the list of available resources:
            $contextText
        """.trimIndent()

        val systemInstruction = """
            You are the street-smart AI navigator for Compass SF.
            Answer the user's question directly, clearly, and companionably in 2-3 sentences.
            Provide 2-3 logical nextSteps in order.
            Crucially, select the best matching resources from the provided list (IDs must exist in the context).
            Return a JSON object with fields:
            - "answer": string (warm conversational guidance)
            - "nextSteps": array of strings (action steps)
            - "picks": array of objects, each with "id" (Int) and "why" (String explanation of why this place is perfect for their exact request).
        """.trimIndent()

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
                temperature = 0.3f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext runOfflineAsk(question, resources)
            Log.d("AiService", "Ask AI response: $jsonText")
            return@withContext json.decodeFromString<AskResponse>(jsonText)
        } catch (e: Exception) {
            Log.e("AiService", "Failed to ask AI, falling back to offline", e)
            return@withContext runOfflineAsk(question, resources)
        }
    }

    // --- Offline Fallbacks ---

    private fun runOfflineParser(text: String): ResourceDraft {
        // Simple regex fallback
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

    private fun runOfflineAsk(question: String, resources: List<Resource>): AskResponse {
        val lowerQ = question.lowercase()
        // Simple search and score
        val ranked = resources.map { r ->
            var score = 0
            if (lowerQ.contains(r.category)) score += 5
            for (t in r.tags) {
                if (lowerQ.contains(t.lowercase())) score += 3
            }
            if (lowerQ.contains(r.name.lowercase())) score += 10
            r to score
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(3)

        val picks = ranked.map { (r, _) ->
            PickDraft(id = r.id, why = "Matches your interest in ${r.category}.")
        }

        return AskResponse(
            answer = "Offline navigator fallback: I found ${picks.size} resources that might help you based on keyword matching.",
            nextSteps = listOf("Review the matching locations below.", "Visit GLIDE or St. Anthony's for on-the-spot support."),
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
