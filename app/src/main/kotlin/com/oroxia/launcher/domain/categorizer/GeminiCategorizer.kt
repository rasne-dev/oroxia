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
        val CANDIDATE_MODELS = listOf(
            "gemini-3.5-flash",
            "gemini-flash-latest",
            "gemini-3.6-flash",
            "gemini-2.5-flash",
            "gemini-2.0-flash"
        )
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

            KESİN VE ZORUNLU KATEGORİ KURALLARI:
            1. FINANS & BANKACILIK KURALI:
               - Tüm bankacılık (İşCep, Garanti BBVA, Akbank, Yapı Kredi, Ziraat Mobil, VakıfBank, Halkbank, QNB Mobil, DenizBank, Enpara, TEB, ING, Kuveyt Türk, Albaraka vb.)
               - Tüm dijital cüzdan ve ödeme (Papara, Tosla, Nays, Paycell, FastPay, Hadi, İninal, Pokus, Param, Sipay vb.)
               - Borsa, hisse, yatırım (Midas, Gedik, Oyak Yatırım vb.)
               - Kripto para (Binance, BtcTurk, Paribu vb.)
               - Döviz, altın, bütçe takibi, para yöneticisi, fatura ödeme ve vergi uygulamalarını MUTLAKA "${LocalCategorizer.CATEGORY_FINANCE}" kategorisine ata.
               - DİKKAT: Para veya bankacılık ile ilgili hiçbir uygulamayı KESİNLİKLE "${LocalCategorizer.CATEGORY_TOOLS}" veya "${LocalCategorizer.CATEGORY_PRODUCTIVITY}" kategorisine ATMA!

            2. KARİYER & İŞ KURALI:
               - LinkedIn, Indeed, Kariyer.net, İŞKUR, İşin Olsun, Eleman.net, CV hazırlama, iş arama uygulamalarını mutlaka "${LocalCategorizer.CATEGORY_CAREER}" kategorisine ata.

            3. DİĞER KATEGORİLER:
               - E-ticaret ve market (Trendyol, Amazon, Getir, Sahibinden, Yemeksepeti vb.): "${LocalCategorizer.CATEGORY_SHOPPING}"
               - Sosyal medya ve mesajlaşma (WhatsApp, Instagram, Telegram vb.): "${LocalCategorizer.CATEGORY_SOCIAL}"
               - Medya ve video (YouTube, Netflix, Spotify vb.): "${LocalCategorizer.CATEGORY_ENTERTAINMENT}"

            Yanıtı SADECE aşağıdaki JSON formatında döndür, başka hiçbir açıklama ekleme:
            {
              "com.example.app": "KategoriAdı"
            }

            Uygulama listesi:
            $appListString
        """.trimIndent()
    }

    private fun callGeminiApi(apiKey: String, promptText: String): String {
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

        var lastException: Exception? = null

        // Try candidate models in order of priority (fallback from active models)
        for (model in CANDIDATE_MODELS) {
            val url = "$BASE_URL/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string()
                        if (!bodyStr.isNullOrBlank()) {
                            return bodyStr
                        }
                    } else if (response.code != 404 && response.code != 503) {
                        // Permanent failure other than model not found or temporary unavailable
                        throw IllegalStateException("Gemini API call failed (${response.code}): ${response.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        throw lastException ?: IllegalStateException("All candidate Gemini models failed")
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

            val parsedJson = json.parseToJsonElement(cleanText).jsonObject

            for ((key, value) in parsedJson) {
                // Format A: { "com.example.app": "Kategori" }
                if (key.contains(".") && value is kotlinx.serialization.json.JsonPrimitive) {
                    result[key] = value.content
                }
                // Format B: { "Kategori": [ { "package_name": "com.example.app" } ] } or [ "com.example.app" ]
                else if (value is kotlinx.serialization.json.JsonArray) {
                    val categoryName = key
                    for (item in value) {
                        when (item) {
                            is kotlinx.serialization.json.JsonObject -> {
                                val pkg = item["package_name"]?.jsonPrimitive?.content
                                    ?: item["packageName"]?.jsonPrimitive?.content
                                    ?: item["pkg"]?.jsonPrimitive?.content
                                if (pkg != null) {
                                    result[pkg] = categoryName
                                }
                            }
                            is kotlinx.serialization.json.JsonPrimitive -> {
                                if (item.content.contains(".")) {
                                    result[item.content] = categoryName
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Silently fallback if parse error
        }
        return result
    }

    fun normalizeCategory(raw: String): String {
        val trimmed = raw.trim()
        val exactMatch = TAXONOMY.firstOrNull { it.equals(trimmed, ignoreCase = true) }
        if (exactMatch != null) return exactMatch

        val clean = trimmed.lowercase()
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c")

        return when {
            clean.contains("finans") || clean.contains("bank") || clean.contains("finance") -> LocalCategorizer.CATEGORY_FINANCE
            clean.contains("kariyer") || clean.contains("is") || clean.contains("job") || clean.contains("career") -> LocalCategorizer.CATEGORY_CAREER
            clean.contains("sosyal") || clean.contains("iletisim") || clean.contains("social") || clean.contains("chat") -> LocalCategorizer.CATEGORY_SOCIAL
            clean.contains("alisveris") || clean.contains("shop") || clean.contains("market") -> LocalCategorizer.CATEGORY_SHOPPING
            clean.contains("eglence") || clean.contains("medya") || clean.contains("entertain") || clean.contains("music") || clean.contains("video") -> LocalCategorizer.CATEGORY_ENTERTAINMENT
            clean.contains("uretken") || clean.contains("ofis") || clean.contains("productiv") || clean.contains("office") -> LocalCategorizer.CATEGORY_PRODUCTIVITY
            clean.contains("seyahat") || clean.contains("ulasim") || clean.contains("travel") || clean.contains("nav") -> LocalCategorizer.CATEGORY_TRAVEL
            clean.contains("egitim") || clean.contains("referans") || clean.contains("educat") -> LocalCategorizer.CATEGORY_EDUCATION
            clean.contains("saglik") || clean.contains("yasam") || clean.contains("health") || clean.contains("fit") -> LocalCategorizer.CATEGORY_HEALTH
            clean.contains("oyun") || clean.contains("game") -> LocalCategorizer.CATEGORY_GAMES
            clean.contains("arac") || clean.contains("sistem") || clean.contains("tool") || clean.contains("util") -> LocalCategorizer.CATEGORY_TOOLS
            else -> LocalCategorizer.CATEGORY_OTHER
        }
    }
}
