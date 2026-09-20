package com.oroxia.launcher.domain

import com.oroxia.launcher.domain.categorizer.GeminiCategorizer
import com.oroxia.launcher.domain.categorizer.LocalCategorizer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GeminiCategorizerTest {

    private lateinit var categorizer: GeminiCategorizer

    @Before
    fun setUp() {
        categorizer = GeminiCategorizer()
    }

    @Test
    fun testParseGeminiResponse_ValidJson() {
        val mockResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\n  \"net.kariyer.android\": \"Kariyer & İş\",\n  \"com.garanti.mobile\": \"Finans & Bankacılık\"\n}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = categorizer.parseGeminiResponse(mockResponse)
        assertEquals("Kariyer & İş", parsed["net.kariyer.android"])
        assertEquals("Finans & Bankacılık", parsed["com.garanti.mobile"])
    }

    @Test
    fun testParseGeminiResponse_GroupedFormat() {
        val mockResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\n  \"Finans & Bankacilik\": [\n    {\"package_name\": \"com.isbank.iscep\"},\n    {\"package_name\": \"com.ykb.android\"}\n  ]\n}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = categorizer.parseGeminiResponse(mockResponse)
        assertEquals("Finans & Bankacilik", parsed["com.isbank.iscep"])
        assertEquals("Finans & Bankacilik", parsed["com.ykb.android"])
    }

    @Test
    fun testNormalizeCategory() {
        assertEquals(LocalCategorizer.CATEGORY_AI, categorizer.normalizeCategory("yapay zeka"))
        assertEquals(LocalCategorizer.CATEGORY_AI, categorizer.normalizeCategory("Artificial Intelligence"))
        assertEquals(LocalCategorizer.CATEGORY_AI, categorizer.normalizeCategory("AI"))
        assertEquals(LocalCategorizer.CATEGORY_WEATHER, categorizer.normalizeCategory("hava durumu"))
        assertEquals(LocalCategorizer.CATEGORY_WEATHER, categorizer.normalizeCategory("Weather Forecast"))
        assertEquals(LocalCategorizer.CATEGORY_CAREER, categorizer.normalizeCategory("kariyer & iş"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.normalizeCategory("finans & bankacılık"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.normalizeCategory("Finans & Bankacilik"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.normalizeCategory("Finans"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.normalizeCategory("Finance"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.normalizeCategory("Banka"))
        assertEquals(LocalCategorizer.CATEGORY_CAREER, categorizer.normalizeCategory("Career"))
        assertEquals(LocalCategorizer.CATEGORY_OTHER, categorizer.normalizeCategory("bilinmeyen"))
    }

    @Test
    fun testSafetyGuard_OverridesGeminiHallucinationsForFinanceAndCareer() {
        val worldMobil = com.oroxia.launcher.domain.categorizer.AppInfoForPrompt(
            packageName = "com.ykb.avm",
            appName = "World Mobil"
        )
        // Even if Gemini hallucinates and predicts "Araçlar & Sistem" or "Alışveriş"
        val resolved = categorizer.resolveCategoryWithSafetyGuard(worldMobil, LocalCategorizer.CATEGORY_TOOLS)
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, resolved)

        val bonusFlas = com.oroxia.launcher.domain.categorizer.AppInfoForPrompt(
            packageName = "com.garanti.bonusflas",
            appName = "Bonus Flaş"
        )
        val resolvedBonus = categorizer.resolveCategoryWithSafetyGuard(bonusFlas, LocalCategorizer.CATEGORY_SHOPPING)
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, resolvedBonus)

        val chatGpt = com.oroxia.launcher.domain.categorizer.AppInfoForPrompt(
            packageName = "com.openai.chatgpt",
            appName = "ChatGPT"
        )
        val resolvedAi = categorizer.resolveCategoryWithSafetyGuard(chatGpt, LocalCategorizer.CATEGORY_PRODUCTIVITY)
        assertEquals(LocalCategorizer.CATEGORY_AI, resolvedAi)

        val accuWeather = com.oroxia.launcher.domain.categorizer.AppInfoForPrompt(
            packageName = "com.accuweather.android",
            appName = "AccuWeather"
        )
        val resolvedWeather = categorizer.resolveCategoryWithSafetyGuard(accuWeather, LocalCategorizer.CATEGORY_TOOLS)
        assertEquals(LocalCategorizer.CATEGORY_WEATHER, resolvedWeather)

        val spotify = com.oroxia.launcher.domain.categorizer.AppInfoForPrompt(
            packageName = "com.spotify.music",
            appName = "Spotify"
        )
        val resolvedSpotify = categorizer.resolveCategoryWithSafetyGuard(spotify, LocalCategorizer.CATEGORY_ENTERTAINMENT)
        assertEquals(LocalCategorizer.CATEGORY_ENTERTAINMENT, resolvedSpotify)
    }
}
