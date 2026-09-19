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
    fun testNormalizeCategory() {
        assertEquals(LocalCategorizer.CATEGORY_CAREER, categorizer.normalizeCategory("kariyer & iş"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.normalizeCategory("finans & bankacılık"))
        assertEquals(LocalCategorizer.CATEGORY_OTHER, categorizer.normalizeCategory("bilinmeyen"))
    }
}
