package com.oroxia.launcher.domain

import com.oroxia.launcher.domain.categorizer.GeminiCategorizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeminiCategorizerTest {

    private lateinit var categorizer: GeminiCategorizer

    @Before
    fun setUp() {
        categorizer = GeminiCategorizer()
    }

    @Test
    fun testFallbackCategorize_CareerApps() {
        assertEquals("Kariyer", categorizer.fallbackCategorize("LinkedIn", "com.linkedin.android"))
        assertEquals("Kariyer", categorizer.fallbackCategorize("Indeed Job Search", "com.indeed.android.jobsearch"))
        assertEquals("Kariyer", categorizer.fallbackCategorize("Kariyer.net", "net.kariyer.android"))
        assertEquals("Kariyer", categorizer.fallbackCategorize("İŞKUR", "tr.gov.iskur.mobil"))
        assertEquals("Kariyer", categorizer.fallbackCategorize("İşin Olsun", "com.isinolsun.android"))
    }

    @Test
    fun testFallbackCategorize_OtherCategories() {
        assertEquals("Finans", categorizer.fallbackCategorize("Garanti BBVA", "com.garanti.mobile"))
        assertEquals("Sosyal", categorizer.fallbackCategorize("Instagram", "com.instagram.android"))
        assertEquals("Alışveriş", categorizer.fallbackCategorize("Trendyol", "com.trendyol.online"))
        assertEquals("Eğlence", categorizer.fallbackCategorize("Spotify", "com.spotify.music"))
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
                        "text": "{\n  \"net.kariyer.android\": \"Kariyer\",\n  \"com.garanti.mobile\": \"Finans\"\n}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = categorizer.parseGeminiResponse(mockResponse)
        assertEquals("Kariyer", parsed["net.kariyer.android"])
        assertEquals("Finans", parsed["com.garanti.mobile"])
    }

    @Test
    fun testNormalizeCategory() {
        assertEquals("Kariyer", categorizer.normalizeCategory("kariyer"))
        assertEquals("Finans", categorizer.normalizeCategory("FINANS"))
        assertEquals("Diğer", categorizer.normalizeCategory("BilinmeyenKategori123"))
    }
}
