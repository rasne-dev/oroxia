# AGENTS.md

## Must-follow constraints

- Target SDK: Android 8.0+ (API 26+)
- Language: Kotlin only — no Java
- UI: Jetpack Compose only — no XML layouts
- AI backend: Gemini 2.0 Flash API only — no other LLM integrations
- No persistent background services — use WorkManager for scheduled tasks only
- Gemini API key must never be hardcoded — read from `local.properties` or environment variable
- App must function offline after initial categorization (cache results locally with Room)

## Validation before finishing

```
./gradlew lint
./gradlew test
./gradlew assembleDebug
```

All three must pass before committing.

## Repo-specific conventions

- Categorization logic lives in `domain/categorizer/` — do not scatter it across ViewModels
- App scan results are cached in Room DB — never re-fetch from system if cache is fresh (< 7 days)
- Notification scheduling is done via WorkManager in `worker/ScanWorker.kt`
- User folder preferences stored in DataStore, not SharedPreferences

## Important locations

- `data/local/` — Room DB entities and DAOs
- `domain/categorizer/` — Gemini API call + category mapping logic
- `worker/` — WorkManager tasks (weekly scan, new app detection)
- `local.properties` — API key storage (gitignored)

## Change safety rules

- `local.properties` must remain in `.gitignore` — never commit API keys
- Changing category taxonomy affects existing user data — migrate Room DB accordingly
- WorkManager constraints must not require network for local operations

## Known gotchas

- `PackageManager.getInstalledApplications()` returns system apps — always filter with `ApplicationInfo.FLAG_SYSTEM`
- Gemini free tier has rate limits — batch app list in single prompt, never one request per app
- New app detection via `ACTION_PACKAGE_ADDED` broadcast requires receiver registered in manifest with `RECEIVE_BOOT_COMPLETED` permission
