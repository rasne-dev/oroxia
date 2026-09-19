package com.oroxia.launcher.domain.categorizer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class AppInfoForPrompt(
    val packageName: String,
    val appName: String
)

class GeminiCategorizer(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val localFallback: LocalCategorizer = LocalCategorizer()
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val MODEL_NAME = "gemini-2.0-flash"
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        val TAXONOMY = LocalCategorizer.TAXONOMY
    }

    suspend fun categorizeAppsBatch(
        apiKey: String,
        apps: List<AppInfoForPrompt>
    ): Map<String, String> = withContext(Dispatchers.IO) {
        if (apps.isEmpty()) return@withContext emptyMap()
        if (apiKey.isBlank() || apiKey == "your_gemini_api_key_here") {
            return@withContext apps.associate { it.packageName to localFallback.categorizeApp(it.appName, it.packageName) }
        }

        val result = mutableMapOf<String, String>()
        val chunkSize = 35 // Optimal batch size for free tier quota

        for (chunk in apps.chunked(chunkSize)) {
            try {
                val prompt = buildBatchPrompt(chunk)
                val responseJson = callGeminiApi(apiKey, prompt)
                val parsed = parseGeminiResponse(responseJson)
                for (app in chunk) {
                    val cat = parsed[app.packageName] ?: localFallback.categorizeApp(app.appName, app.packageName)
                    result[app.packageName] = normalizeCategory(cat)
                }
            } catch (e: Exception) {
                // Seamless local fallback if network error or rate limit
                for (app in chunk) {
                    result[app.packageName] = localFallback.categorizeApp(app.appName, app.packageName)
                }
            }
        }
        result
    }

    private fun buildBatchPrompt(apps: List<AppInfoForPrompt>): String {
        val appListString = apps.joinToString(separator = "\n") { "- ${it.packageName}: ${it.appName}" }
        return """
            Sen akıllı bir Android ana ekran düzenleme asistanısın.
            Aşağıda paket adları ve isimleri verilen Android uygulamalarını analiz et ve her birini ŞU KATEGORİLERDEN YALNIZCA BİRİNE ata:
            ${TAXONOMY.joinToString(", ")}

            Önemli sınıflandırma kuralları:
            1. LinkedIn, Indeed, Kariyer.net, İŞKUR, İşin Olsun, eleman arama, CV hazırlama gibi kariyer ve iş bulma uygulamalarını mutlaka "${LocalCategorizer.CATEGORY_CAREER}" kategorisine ata.
            2. Bankacılık, Papara, borsa, kripto para, döviz, ödeme uygulamalarını "${LocalCategorizer.CATEGORY_FINANCE}" kategorisine ata.
            3. E-ticaret, pazar yeri, market sipariş (Trendyol, Amazon, Getir, Sahibinden vb.) uygulamalarını "${LocalCategorizer.CATEGORY_SHOPPING}" kategorisine ata.
            4. Sosyal ağ ve mesajlaşma uygulamalarını "${LocalCategorizer.CATEGORY_SOCIAL}" kategorisine ata.
            5. Yanıtı SADECE ve SADECE aşağıdaki JSON formatında döndür, başka hiçbir açıklama veya markdown ekleme:
            {
              "com.example.app": "KategoriAdı"
            }

            Uygulama listesi:
            $appListString
        """.trimIndent()
    }

    private fun callGeminiApi(apiKey: String, promptText: String): String {
        val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
        val payload = """
            {
              "contents": [{
                "parts": [{ "text": ${Json.encodeToString(kotlinx.serialization.serializer(), promptText)} }]
              }],
              "generationConfig": {
                "temperature": 0.1,
                "responseMimeType": "application/json"
              }
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = payload.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Gemini API call failed (${response.code}): ${response.body?.string()}")
            }
            return response.body?.string() ?: throw IllegalStateException("Empty response body from Gemini")
        }
    }

    fun parseGeminiResponse(rawResponseBody: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val root = json.parseToJsonElement(rawResponseBody).jsonObject
            val candidates = root["candidates"]?.jsonArray ?: return emptyMap()
            if (candidates.isEmpty()) return emptyMap()

            val content = candidates[0].jsonObject["content"]?.jsonObject ?: return emptyMap()
            val parts = content["parts"]?.jsonArray ?: return emptyMap()
            if (parts.isEmpty()) return emptyMap()

            val text = parts[0].jsonObject["text"]?.jsonPrimitive?.content ?: return emptyMap()
            val cleanText = text.trim().removeSurrounding("```json", "```").removeSurrounding("```", "```").trim()

            val mapping = json.parseToJsonElement(cleanText).jsonObject
            for ((key, value) in mapping) {
                result[key] = value.jsonPrimitive.content
            }
        } catch (e: Exception) {
            // Silently fallback if parse error
        }
        return result
    }

    fun normalizeCategory(raw: String): String {
        val trimmed = raw.trim()
        val found = TAXONOMY.firstOrNull { it.equals(trimmed, ignoreCase = true) }
        return found ?: LocalCategorizer.CATEGORY_OTHER
    }
}
