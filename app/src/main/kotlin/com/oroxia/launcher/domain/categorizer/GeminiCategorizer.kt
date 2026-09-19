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
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val MODEL_NAME = "gemini-2.0-flash"
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        val TAXONOMY = listOf(
            "Kariyer",      // LinkedIn, Indeed, Kariyer.net, İŞKUR, İşin Olsun vb.
            "Finans",       // Bankalar, Papara, borsa, kripto, e-cüzdan
            "Sosyal",       // WhatsApp, Telegram, Instagram, X, TikTok, Discord
            "Alışveriş",    // Trendyol, Hepsiburada, Amazon, Getir, Sahibinden
            "Üretkenlik",   // Notlar, takvim, Todoist, Drive, ofis araçları
            "Eğlence",      // YouTube, Spotify, Netflix, müzik/video
            "Oyun",         // Tüm mobil oyunlar
            "Araçlar",      // Dosya yöneticisi, hesap makinesi, tarayıcı, VPN
            "Eğitim",       // Duolingo, Udemy, sözlük, yabancı dil
            "Sağlık",       // Adımsayar, fitness, diyet, MHRS
            "Diğer"
        )
    }

    suspend fun categorizeAppsBatch(
        apiKey: String,
        apps: List<AppInfoForPrompt>
    ): Map<String, String> = withContext(Dispatchers.IO) {
        if (apps.isEmpty()) return@withContext emptyMap()
        if (apiKey.isBlank() || apiKey == "your_gemini_api_key_here") {
            // Offline fallback heuristics if no API key is set
            return@withContext apps.associate { it.packageName to fallbackCategorize(it.appName, it.packageName) }
        }

        val result = mutableMapOf<String, String>()
        val chunkSize = 40 // Batch up to 40 apps per request to respect free tier limits

        for (chunk in apps.chunked(chunkSize)) {
            try {
                val prompt = buildBatchPrompt(chunk)
                val responseJson = callGeminiApi(apiKey, prompt)
                val parsed = parseGeminiResponse(responseJson)
                for (app in chunk) {
                    val cat = parsed[app.packageName] ?: fallbackCategorize(app.appName, app.packageName)
                    result[app.packageName] = normalizeCategory(cat)
                }
            } catch (e: Exception) {
                // In case of network error or rate limit, fall back safely
                for (app in chunk) {
                    result[app.packageName] = fallbackCategorize(app.appName, app.packageName)
                }
            }
        }
        result
    }

    private fun buildBatchPrompt(apps: List<AppInfoForPrompt>): String {
        val appListString = apps.joinToString(separator = "\n") { "- ${it.packageName}: ${it.appName}" }
        return """
            Sen akıllı bir Android uygulama kategorilendirme asistanısın.
            Aşağıda paket adları ve isimleri verilen Android uygulamalarını analiz et ve her birini ŞU KATEGORİLERDEN YALNIZCA BİRİNE ata:
            ${TAXONOMY.joinToString(", ")}

            Önemli kurallar:
            1. LinkedIn, Indeed, Kariyer.net, İŞKUR, İşin Olsun, CV, eleman arama gibi iş ve kariyerle ilgili tüm uygulamalar mutlaka "Kariyer" kategorisine atanmalıdır.
            2. Bankacılık, borsa, kripto para, Papara, ödeme uygulamaları "Finans" kategorisine atanmalıdır.
            3. Yanıtı SADECE ve SADECE aşağıdaki JSON formatında ver, başka hiçbir açıklama veya markdown ekleme:
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
                throw IllegalStateException("Gemini API call failed with code ${response.code}: ${response.body?.string()}")
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
        return found ?: "Diğer"
    }

    fun fallbackCategorize(appName: String, packageName: String): String {
        val lower = "${appName.lowercase()} ${packageName.lowercase()}"
        return when {
            lower.containsAny("kariyer", "işin olsun", "isinolsun", "linkedin", "indeed", "işkur", "iskur", "job", "career", "eleman", "cv") -> "Kariyer"
            lower.containsAny("bank", "finans", "garanti", "isbank", "ziraat", "akbank", "papara", "yapikredi", "vakif", "crypto", "binance", "borsa", "wallet") -> "Finans"
            lower.containsAny("whatsapp", "telegram", "instagram", "facebook", "twitter", "tiktok", "discord", "messenger", "social", "reddit") -> "Sosyal"
            lower.containsAny("trendyol", "hepsiburada", "amazon", "getir", "sahibinden", "yemeksepeti", "migros", "shopping", "n11") -> "Alışveriş"
            lower.containsAny("youtube", "netflix", "spotify", "music", "video", "prime video", "disney", "twitch") -> "Eğlence"
            lower.containsAny("office", "word", "excel", "drive", "notion", "trello", "notes", "keep", "tasks", "calendar") -> "Üretkenlik"
            lower.containsAny("game", "oyun", "puzzle", "clash", "pubg", "candy", "chess") -> "Oyun"
            lower.containsAny("duolingo", "udemy", "course", "dictionary", "sözlük", "learn", "study") -> "Eğitim"
            lower.containsAny("health", "fitness", "sağlık", "mhrs", "enabiz", "step", "diet") -> "Sağlık"
            else -> "Araçlar"
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
