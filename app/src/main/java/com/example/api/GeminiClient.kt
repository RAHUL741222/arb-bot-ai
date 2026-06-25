package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class GeminiResponse(
        val text: String,
        val searchQueries: List<String> = emptyList(),
        val searchSources: List<SearchSource> = emptyList()
    )

    data class SearchSource(
        val title: String,
        val uri: String
    )

    suspend fun queryGemini(prompt: String, systemInstruction: String? = null): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResponse(
                text = "এপিআই কী (API Key) কনফিগার করা নেই। অনুগ্রহ করে আপনার Google AI Studio-র 'Secrets' প্যানেলে 'GEMINI_API_KEY' যুক্ত করুন।"
            )
        }

        val url = "$BASE_URL?key=$apiKey"

        try {
            // Build the JSON payload using standard org.json.JSONObject to be highly robust and compatible
            val root = JSONObject()

            // Contents array
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            contentObj.put("role", "user")
            
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            // Add Tools for Google Search Grounding
            val toolsArray = JSONArray()
            val toolObj = JSONObject()
            toolObj.put("googleSearch", JSONObject()) // Enables Google Search grounding
            toolsArray.put(toolObj)
            root.put("tools", toolsArray)

            // Add system instruction if provided
            if (systemInstruction != null) {
                val sysInstObj = JSONObject()
                val sysPartsArray = JSONArray()
                val sysPartObj = JSONObject()
                sysPartObj.put("text", systemInstruction)
                sysPartsArray.put(sysPartObj)
                sysInstObj.put("parts", sysPartsArray)
                root.put("systemInstruction", sysInstObj)
            }

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.5)
            root.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = root.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Request failed with code ${response.code}: $bodyString")
                    return@withContext GeminiResponse(
                        text = "দুঃখিত, এপিআই কল করার সময় একটি সমস্যা হয়েছে। কোড: ${response.code}। অনুগ্রহ করে আবার চেষ্টা করুন।"
                    )
                }

                if (bodyString == null) {
                    return@withContext GeminiResponse(text = "কোন উত্তর পাওয়া যায়নি।")
                }

                // Parse the response
                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext GeminiResponse(text = "মডেল কোনো উত্তর প্রদান করতে পারেনি।")
                }

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val textParts = content?.optJSONArray("parts")
                var responseText = ""
                if (textParts != null && textParts.length() > 0) {
                    responseText = textParts.getJSONObject(0).optString("text", "")
                }

                // Extract grounding metadata if available (Search Grounding)
                val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
                val searchQueries = mutableListOf<String>()
                val searchSources = mutableListOf<SearchSource>()

                if (groundingMetadata != null) {
                    val webSearchQueriesJson = groundingMetadata.optJSONArray("webSearchQueries")
                    if (webSearchQueriesJson != null) {
                        for (i in 0 until webSearchQueriesJson.length()) {
                            searchQueries.add(webSearchQueriesJson.getString(i))
                        }
                    }

                    val groundingChunksJson = groundingMetadata.optJSONArray("groundingChunks")
                    if (groundingChunksJson != null) {
                        for (i in 0 until groundingChunksJson.length()) {
                            val chunk = groundingChunksJson.getJSONObject(i)
                            val webObj = chunk.optJSONObject("web")
                            if (webObj != null) {
                                val title = webObj.optString("title", "উৎস")
                                val uri = webObj.optString("uri", "")
                                if (uri.isNotEmpty()) {
                                    searchSources.add(SearchSource(title, uri))
                                }
                            }
                        }
                    }
                }

                if (responseText.isEmpty()) {
                    responseText = "মডেল থেকে সঠিক উত্তর পাওয়া যায়নি।"
                }

                GeminiResponse(
                    text = responseText,
                    searchQueries = searchQueries,
                    searchSources = searchSources.distinctBy { it.uri }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in queryGemini", e)
            GeminiResponse(text = "ত্রুটি ঘটেছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}")
        }
    }
}
