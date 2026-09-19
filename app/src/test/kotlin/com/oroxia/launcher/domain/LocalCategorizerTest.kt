package com.oroxia.launcher.domain

import com.oroxia.launcher.domain.categorizer.LocalCategorizer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LocalCategorizerTest {

    private lateinit var categorizer: LocalCategorizer

    @Before
    fun setUp() {
        categorizer = LocalCategorizer()
    }

    @Test
    fun testCareerCategory_MatchesTurkishAndGlobalJobApps() {
        assertEquals(LocalCategorizer.CATEGORY_CAREER, categorizer.categorizeApp("LinkedIn", "com.linkedin.android"))
        assertEquals(LocalCategorizer.CATEGORY_CAREER, categorizer.categorizeApp("Indeed", "com.indeed.android.jobsearch"))
        assertEquals(LocalCategorizer.CATEGORY_CAREER, categorizer.categorizeApp("Kariyer.net", "net.kariyer.android"))
        assertEquals(LocalCategorizer.CATEGORY_CAREER, categorizer.categorizeApp("İŞKUR", "tr.gov.iskur.mobil"))
        assertEquals(LocalCategorizer.CATEGORY_CAREER, categorizer.categorizeApp("İşin Olsun", "com.isinolsun.android"))
        assertEquals(LocalCategorizer.CATEGORY_CAREER, categorizer.categorizeApp("Eleman.net", "com.eleman.android"))
        assertEquals(LocalCategorizer.CATEGORY_CAREER, categorizer.categorizeApp("Upwork", "com.upwork.android.apps.main"))
    }

    @Test
    fun testFinanceCategory_MatchesBankingAndCrypto() {
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Garanti BBVA", "com.garanti.mobile"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Ziraat Mobil", "com.ziraat.ziraatmobil"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Papara", "com.mobillium.papara"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Binance", "com.binance.dev"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Midas", "com.getmidas.app"))
    }

    @Test
    fun testShoppingCategory_MatchesMarketplaces() {
        assertEquals(LocalCategorizer.CATEGORY_SHOPPING, categorizer.categorizeApp("Trendyol", "com.trendyol.online"))
        assertEquals(LocalCategorizer.CATEGORY_SHOPPING, categorizer.categorizeApp("Hepsiburada", "com.pozitron.hepsiburada"))
        assertEquals(LocalCategorizer.CATEGORY_SHOPPING, categorizer.categorizeApp("Amazon", "com.amazon.mShop.android.shopping"))
        assertEquals(LocalCategorizer.CATEGORY_SHOPPING, categorizer.categorizeApp("Getir", "com.getir"))
        assertEquals(LocalCategorizer.CATEGORY_SHOPPING, categorizer.categorizeApp("Sahibinden", "com.sahibinden"))
    }

    @Test
    fun testSocialCategory_MatchesChatAndSocialMedia() {
        assertEquals(LocalCategorizer.CATEGORY_SOCIAL, categorizer.categorizeApp("WhatsApp", "com.whatsapp"))
        assertEquals(LocalCategorizer.CATEGORY_SOCIAL, categorizer.categorizeApp("Instagram", "com.instagram.android"))
        assertEquals(LocalCategorizer.CATEGORY_SOCIAL, categorizer.categorizeApp("Telegram", "org.telegram.messenger"))
        assertEquals(LocalCategorizer.CATEGORY_SOCIAL, categorizer.categorizeApp("Discord", "com.discord"))
    }

    @Test
    fun testEntertainmentCategory_MatchesStreaming() {
        assertEquals(LocalCategorizer.CATEGORY_ENTERTAINMENT, categorizer.categorizeApp("Spotify", "com.spotify.music"))
        assertEquals(LocalCategorizer.CATEGORY_ENTERTAINMENT, categorizer.categorizeApp("Netflix", "com.netflix.mediaclient"))
        assertEquals(LocalCategorizer.CATEGORY_ENTERTAINMENT, categorizer.categorizeApp("YouTube", "com.google.android.youtube"))
    }

    @Test
    fun testHealthCategory_MatchesHealthApps() {
        assertEquals(LocalCategorizer.CATEGORY_HEALTH, categorizer.categorizeApp("MHRS", "tr.gov.saglik.mhrs"))
        assertEquals(LocalCategorizer.CATEGORY_HEALTH, categorizer.categorizeApp("e-Nabız", "tr.gov.saglik.enabiz"))
    }
}
