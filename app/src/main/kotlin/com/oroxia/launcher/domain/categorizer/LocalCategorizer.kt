package com.oroxia.launcher.domain.categorizer

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

data class AppCategoryResult(
    val category: String,
    val confidence: Float
)

class LocalCategorizer {

    companion object {
        const val CATEGORY_CAREER = "Kariyer & İş"
        const val CATEGORY_FINANCE = "Finans & Bankacılık"
        const val CATEGORY_SOCIAL = "Sosyal & İletişim"
        const val CATEGORY_SHOPPING = "Alışveriş"
        const val CATEGORY_ENTERTAINMENT = "Eğlence & Medya"
        const val CATEGORY_PRODUCTIVITY = "Üretkenlik & Ofis"
        const val CATEGORY_TRAVEL = "Seyahat & Ulaşım"
        const val CATEGORY_EDUCATION = "Eğitim & Referans"
        const val CATEGORY_HEALTH = "Sağlık & Yaşam"
        const val CATEGORY_GAMES = "Oyunlar"
        const val CATEGORY_TOOLS = "Araçlar & Sistem"
        const val CATEGORY_OTHER = "Diğer"

        val TAXONOMY = listOf(
            CATEGORY_CAREER,
            CATEGORY_FINANCE,
            CATEGORY_SOCIAL,
            CATEGORY_SHOPPING,
            CATEGORY_ENTERTAINMENT,
            CATEGORY_PRODUCTIVITY,
            CATEGORY_TRAVEL,
            CATEGORY_EDUCATION,
            CATEGORY_HEALTH,
            CATEGORY_GAMES,
            CATEGORY_TOOLS,
            CATEGORY_OTHER
        )

        // Comprehensive keyword dictionaries for Turkish & global apps (normalized: i, g, u, s, o, c)
        private val CAREER_KEYWORDS = listOf(
            "kariyer", "isin olsun", "isinolsun", "linkedin", "indeed", "iskur",
            "eleman", "secretcv", "yenibiris", "upwork", "fiverr", "freelancer", "armut",
            "bionluk", "job", "career", "resume", "cv maker", "cv hazirla", "headhunter", "glassdoor", "monster"
        )

        private val FINANCE_KEYWORDS = listOf(
            // Turkish Banks & Official Apps
            "garanti", "bbva", "cepsubesi", "isbank", "iscep", "is bankasi", "maximum",
            "ziraat", "ziraat mobil", "ziraat katilim", "ziraat borsa",
            "akbank", "axess", "wings", "yapikredi", "yapi kredi", "ykb", "world",
            "vakif", "vakifbank", "vakif katilim", "vakif borsa",
            "halkbank", "paraf", "halk yatirim", "qnb", "finansbank", "enpara",
            "denizbank", "mobildeniz", "teb", "cepteteb", "ing bank", "ingbank", "ing mobil",
            "albaraka", "kuveytturk", "kuveyt turk", "turkiye finans", "emlak katilim",
            "anadolubank", "odeabank", "sekerbank", "burgan", "aktifbank", "nkolay", "n kolay",
            "fibabanka", "hayat finans", "tom bank",
            // Digital Wallets, FinTech & Payments
            "papara", "tosla", "nays", "paycell", "fastpay", "hadi", "ininal", "pokus",
            "param", "oldubil", "fups", "sipay", "pep", "iyzico", "paytr", "troy",
            "paypal", "revolut", "wise", "wallet", "cuzdan", "kredi kart", "banka kart",
            // Crypto, Stocks & Investments
            "midas", "gedik", "oyak yatirim", "ak yatirim", "is yatirim", "matriks", "tradingview",
            "borsa", "hisse", "portfoy", "yatirim fon", "tefas", "viop", "forex", "trading",
            "binance", "btcturk", "paribu", "crypto", "kripto", "bitcoin", "btc", "ethereum",
            "gateio", "okx", "mexc", "bybit", "bitlo", "coin",
            // Currency, Gold, Budget, Expense, Tax
            "doviz", "altin", "gumus", "canli kur", "doviz kur", "parite", "harem altin",
            "butce", "gelir", "gider", "para yonetim", "para yoneticisi", "monefy", "spendee",
            "1para", "butcem", "hesap defteri", "kasa",
            "fatura", "odeme", "sanal pos", "pos mobil", "yazar kasa",
            "gib", "gelir idaresi", "vergi", "e fatura", "gib mobile",
            // Generic Finance Terms
            "finance", "finans", "finansal", "banking", "banka", "bank"
        )

        private val SHOPPING_KEYWORDS = listOf(
            "trendyol", "hepsiburada", "amazon", "n11", "getir", "yemeksepeti", "migros",
            "sahibinden", "letgo", "dolap", "gardrops", "aliexpress", "temu", "shopee",
            "ebay", "zara", "lcw", "boyner", "morhipo", "ciceksepeti", "a101", "bim",
            "sok", "carrefour", "istegelsin", "banabi", "market", "shopping", "ecommerce"
        )

        private val SOCIAL_KEYWORDS = listOf(
            "whatsapp", "telegram", "instagram", "facebook", "twitter", "tiktok",
            "snapchat", "discord", "signal", "bip", "threads", "reddit", "pinterest",
            "clubhouse", "zoom", "skype", "teams", "meet", "tinder", "bumble", "okcupid",
            "messenger", "social", "chat"
        )

        private val ENTERTAINMENT_KEYWORDS = listOf(
            "youtube", "netflix", "spotify", "disney", "prime video", "blutv", "exxen",
            "gain", "mubi", "deezer", "apple music", "fizy", "twitch", "sinema", "movie",
            "music", "radio", "podcast", "stream", "video", "player", "tv", "vlc", "tabii"
        )

        private val PRODUCTIVITY_KEYWORDS = listOf(
            "drive", "docs", "sheets", "slides", "office", "word", "excel", "powerpoint",
            "notion", "trello", "asana", "slack", "evernote", "keep", "todoist", "onenote",
            "camscanner", "pdf", "scanner", "notes", "calendar", "task", "document", "cloud"
        )

        private val TRAVEL_KEYWORDS = listOf(
            "maps", "harita", "uber", "bitaksi", "marti", "binbin", "scooter", "yandex",
            "moovit", "enuygun", "obilet", "skyscanner", "booking", "airbnb", "trivago",
            "thy", "pegasus", "sunexpress", "navigasyon", "trip", "flight", "hotel"
        )

        private val EDUCATION_KEYWORDS = listOf(
            "duolingo", "udemy", "coursera", "khan", "ebba", "eba", "quizlet", "busuu",
            "memrise", "cambly", "sozluk", "dictionary", "wikipedia", "learn", "study",
            "course", "exam", "ogren", "ders", "kitap", "book"
        )

        private val HEALTH_KEYWORDS = listOf(
            "mhrs", "enabiz", "hastane", "fitness", "step", "adim", "diyet",
            "diet", "water", "workout", "gym", "strava", "nike run", "meditasyon",
            "health", "saglik", "nabiz", "kalori", "pharmacy", "eczane"
        )

        private val GAMES_KEYWORDS = listOf(
            "game", "oyun", "puzzle", "clash", "pubg", "candy", "chess", "satranc",
            "fifa", "roblox", "brawl", "minecraft", "sudoku", "runner", "race", "rpg",
            "action", "arcade", "casino", "poker", "tavla", "okey", "101"
        )

        private val TOOLS_KEYWORDS = listOf(
            "calculator", "hesap makinesi", "file manager", "dosya", "cleaner", "antivirus",
            "vpn", "flashlight", "fener", "clock", "saat", "alarm", "weather", "hava durumu",
            "settings", "ayarlar", "tools", "araclar", "qr okuyucu", "qr scanner", "barcode scanner", "speedtest"
        )
    }

    /**
     * Categorizes an app using Android OS metadata, package name, and label heuristics.
     * Works 100% offline on-device without any network or API keys.
     */
    fun categorizeApp(
        appName: String,
        packageName: String,
        appInfo: ApplicationInfo? = null
    ): String {
        val normalizedName = normalizeText(appName)
        val normalizedPkg = normalizeText(packageName)
        val combined = "$normalizedName $normalizedPkg"

        // 1. High Priority Semantic Matches (Career and Finance have top priority)
        if (combined.containsAny(CAREER_KEYWORDS)) return CATEGORY_CAREER
        if (combined.containsAny(FINANCE_KEYWORDS)) return CATEGORY_FINANCE
        if (combined.containsAny(SHOPPING_KEYWORDS)) return CATEGORY_SHOPPING
        if (combined.containsAny(SOCIAL_KEYWORDS)) return CATEGORY_SOCIAL
        if (combined.containsAny(ENTERTAINMENT_KEYWORDS)) return CATEGORY_ENTERTAINMENT
        if (combined.containsAny(TRAVEL_KEYWORDS)) return CATEGORY_TRAVEL
        if (combined.containsAny(HEALTH_KEYWORDS)) return CATEGORY_HEALTH
        if (combined.containsAny(EDUCATION_KEYWORDS)) return CATEGORY_EDUCATION
        if (combined.containsAny(PRODUCTIVITY_KEYWORDS)) return CATEGORY_PRODUCTIVITY
        if (combined.containsAny(GAMES_KEYWORDS)) return CATEGORY_GAMES
        if (combined.containsAny(TOOLS_KEYWORDS)) return CATEGORY_TOOLS

        // 2. Android OS Native Category Metadata (API 26+)
        if (appInfo != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val osCategory = mapAndroidOsCategory(appInfo.category)
            if (osCategory != null) {
                return osCategory
            }
        }

        // 3. Fallback to Tools or Other
        return if (normalizedPkg.contains("tool") || normalizedPkg.contains("util")) CATEGORY_TOOLS else CATEGORY_OTHER
    }

    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace(Regex("[^a-z0-9]"), " ")
    }

    private fun mapAndroidOsCategory(osCategory: Int): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return when (osCategory) {
                ApplicationInfo.CATEGORY_GAME -> CATEGORY_GAMES
                ApplicationInfo.CATEGORY_AUDIO -> CATEGORY_ENTERTAINMENT
                ApplicationInfo.CATEGORY_VIDEO -> CATEGORY_ENTERTAINMENT
                ApplicationInfo.CATEGORY_IMAGE -> CATEGORY_ENTERTAINMENT
                ApplicationInfo.CATEGORY_SOCIAL -> CATEGORY_SOCIAL
                ApplicationInfo.CATEGORY_NEWS -> CATEGORY_ENTERTAINMENT
                ApplicationInfo.CATEGORY_MAPS -> CATEGORY_TRAVEL
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> CATEGORY_PRODUCTIVITY
                ApplicationInfo.CATEGORY_ACCESSIBILITY -> CATEGORY_TOOLS
                else -> null
            }
        }
        return null
    }

    private fun String.containsAny(keywords: List<String>): Boolean {
        for (kw in keywords) {
            if (this.contains(kw)) return true
        }
        return false
    }
}
