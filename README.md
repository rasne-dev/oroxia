# Oroxia

> 🚀 AI-powered Android Smart Launcher & Home Screen Organizer  
> Yapay zeka destekli akıllı Android başlatıcı ve ana ekran düzenleyici

Oroxia is a modern Android Custom Launcher built with Jetpack Compose that uses the **Gemini 2.0 Flash API** to automatically categorize your installed applications, group them into smart folders on your home screen, and provide proactive recommendations when new apps are installed (e.g. suggesting adding *Kariyer.net* to an existing *Kariyer* folder alongside *LinkedIn* and *Indeed*).

---

## 🌟 Key Features / Öne Çıkan Özellikler

- **🤖 AI-Powered Categorization (Gemini 2.0 Flash):**
  - Analyzes installed applications in batch prompts to stay strictly within rate limits.
  - Understands local and global apps (e.g., *LinkedIn, Indeed, Kariyer.net, İŞKUR, İşin Olsun* are accurately grouped under **Kariyer & İş**).
  - Fully functional offline after initial categorization thanks to local Room DB caching (< 7 days cache freshness rule).

- **📁 Smart Folders (Akıllı Klasörler):**
  - Automatically creates and manages category-based folders on your home screen (Career, Finance, Social, Shopping, Entertainment, Productivity, Games, Tools, etc.).
  - 2x2 live icon previews for folders and expandable bottom sheets to launch apps instantly.

- **💡 Proactive Suggestions (Akıllı Öneriler):**
  - When a new app is installed (e.g., *Kariyer.net*), Oroxia detects it via background `ACTION_PACKAGE_ADDED` receiver and prompts:
    > *"Cihazınızda Kariyer.net tespit edildi. 'Kariyer' klasörüne eklensin mi?"*
  - Choose between **Interactive Suggestion Mode** (ask with 1-tap accept/dismiss) or **Full Auto Mode** (organize silently).

- **🔕 Zero Spam Background Worker:**
  - Uses Android WorkManager for weekly maintenance scans and new app events. No persistent foreground services draining your battery.

- **🔒 Privacy First & Open Source:**
  - No trackers, no ads. API keys stored strictly in `local.properties` or encrypted locally via DataStore.

---

## 🏗️ Architecture & Tech Stack

| Component | Technology | Purpose |
| :--- | :--- | :--- |
| **UI** | Jetpack Compose + Material 3 | Modern declarative UI, fluid animations, custom launcher interface |
| **AI Engine** | Gemini 2.0 Flash API | Batch app categorization and semantic clustering |
| **Local Cache** | Room Database | Offline-first app and folder persistence |
| **Preferences** | Jetpack DataStore | User folder placement and AI settings |
| **Background** | WorkManager | Periodic weekly scans and new app broadcast processing |
| **Language** | Kotlin 100% | Modern, coroutines & Flow-based architecture |

---

## 📂 Project Structure

```
oroxia/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Launcher intent filters & permissions
│   │   └── kotlin/com/oroxia/launcher/
│   │       ├── OroxiaApplication.kt     # App entrypoint & WorkManager init
│   │       ├── data/
│   │       │   ├── local/               # Room entities, DAOs, and database
│   │       │   └── pref/                # Jetpack DataStore preferences
│   │       ├── domain/categorizer/      # Gemini 2.0 Flash API, AppScanner & SmartFolderManager
│   │       ├── receiver/                # AppInstallReceiver (ACTION_PACKAGE_ADDED)
│   │       ├── worker/                  # ScanWorker (WorkManager weekly/on-demand scan)
│   │       └── ui/
│   │           ├── MainActivity.kt      # Launcher main activity
│   │           ├── home/                # Home screen, folder sheet, suggestion card
│   │           ├── drawer/              # App drawer with search and category filter chips
│   │           ├── settings/            # API key & folder mode configurations
│   │           └── theme/               # Material 3 Dark theme & styling
```

---

## 🚀 Getting Started / Kurulum

1. **Clone the repository:**
   ```bash
   git clone https://github.com/rasne-dev/oroxia.git
   cd oroxia
   ```

2. **Configure your Gemini API Key:**
   Add your Gemini 2.0 Flash API key to `local.properties`:
   ```properties
   GEMINI_API_KEY=AIzaSy...your_gemini_api_key...
   ```
   *(Alternatively, you can configure it inside the in-app Settings screen at runtime).*

3. **Build and Validate:**
   ```bash
   ./gradlew lint
   ./gradlew test
   ./gradlew assembleDebug
   ```

4. **Run on Device / Emulator:**
   - Install the generated APK on your Android device (Android 8.0+ / API 26+).
   - Press the **Home** button on your device.
   - Select **Oroxia** and choose **Always** (Her Zaman) to set it as your default launcher.

---

## 📄 License

GPL-3.0 © [rasne-dev](https://github.com/rasne-dev)
