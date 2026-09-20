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
    fun testAiCategory_MatchesLeadingAiApps() {
        assertEquals(LocalCategorizer.CATEGORY_AI, categorizer.categorizeApp("ChatGPT", "com.openai.chatgpt"))
        assertEquals(LocalCategorizer.CATEGORY_AI, categorizer.categorizeApp("Gemini", "com.google.android.apps.bard"))
        assertEquals(LocalCategorizer.CATEGORY_AI, categorizer.categorizeApp("Microsoft Copilot", "com.microsoft.copilot"))
        assertEquals(LocalCategorizer.CATEGORY_AI, categorizer.categorizeApp("Claude", "com.anthropic.claude"))
        assertEquals(LocalCategorizer.CATEGORY_AI, categorizer.categorizeApp("DeepSeek", "com.deepseek.chat"))
        assertEquals(LocalCategorizer.CATEGORY_AI, categorizer.categorizeApp("Perplexity", "ai.perplexity.app.android"))
    }

    @Test
    fun testWeatherCategory_MatchesWeatherAppsAndExcludesAirlines() {
        assertEquals(LocalCategorizer.CATEGORY_WEATHER, categorizer.categorizeApp("Hava Durumu", "tr.gov.mgm.meteorolojihavadurumu"))
        assertEquals(LocalCategorizer.CATEGORY_WEATHER, categorizer.categorizeApp("AccuWeather", "com.accuweather.android"))
        assertEquals(LocalCategorizer.CATEGORY_WEATHER, categorizer.categorizeApp("Windy.com", "com.windyty.android"))
        assertEquals(LocalCategorizer.CATEGORY_WEATHER, categorizer.categorizeApp("The Weather Channel", "com.weather.Weather"))

        // Airlines should go to Travel, not Weather
        assertEquals(LocalCategorizer.CATEGORY_TRAVEL, categorizer.categorizeApp("Türk Hava Yolları", "com.turkishairlines.mobile"))
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
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("İşCep", "com.isbank.iscep"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Yapı Kredi", "com.ykb.android"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("World Mobil", "com.ykb.avm"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Bonus Flaş", "com.garanti.bonusflas"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Maximum Mobil", "com.isbank.maximummobil"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Juzdan", "com.akbank.axess"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Paraf Mobil", "com.halkbank.mvpos"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("MobilDeniz", "com.tmob.denizbank"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("QNB Mobil", "com.finansbank.mobile.cepsube"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Kuveyt Türk", "com.kuveytturk.mobil"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Papara", "com.mobillium.papara"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Tosla", "com.akbank.tosla"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Nays", "com.dgpays.nays"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Binance", "com.binance.dev"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Midas", "com.getmidas.app"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Para Yöneticisi", "com.realbyteapps.moneymanager"))
        assertEquals(LocalCategorizer.CATEGORY_FINANCE, categorizer.categorizeApp("Canlı Döviz", "com.doviz.app"))
    }

    @Test
    fun testGamesCategory_DoesNotFalsePositiveOnWorldKeywords() {
        assertEquals(LocalCategorizer.CATEGORY_GAMES, categorizer.categorizeApp("World of Tanks Blitz", "com.wargaming.wot.blitz"))
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
