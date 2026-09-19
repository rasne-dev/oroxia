package com.oroxia.launcher.data.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.oroxia.launcher.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "oroxia_user_prefs")

interface PreferencesRepository {
    val geminiApiKeyFlow: Flow<String>
    val autoCategorizeEnabledFlow: Flow<Boolean>
    val autoFolderPlacementFlow: Flow<Boolean>
    val lastScanTimestampFlow: Flow<Long>
    val dismissedPackagesFlow: Flow<Set<String>>

    suspend fun setGeminiApiKey(key: String)
    suspend fun setAutoCategorizeEnabled(enabled: Boolean)
    suspend fun setAutoFolderPlacement(auto: Boolean)
    suspend fun updateLastScanTimestamp(timestamp: Long = System.currentTimeMillis())
    suspend fun dismissPackageSuggestion(packageName: String)
}

open class UserPreferencesRepository(private val context: Context) : PreferencesRepository {

    private object Keys {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val AUTO_CATEGORIZE_ENABLED = booleanPreferencesKey("auto_categorize_enabled")
        val AUTO_FOLDER_PLACEMENT = booleanPreferencesKey("auto_folder_placement")
        val LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
        val DISMISSED_PACKAGES = stringSetPreferencesKey("dismissed_packages")
    }

    override val geminiApiKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val saved = prefs[Keys.GEMINI_API_KEY]
        if (!saved.isNullOrBlank()) saved else BuildConfig.GEMINI_API_KEY
    }

    override val autoCategorizeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_CATEGORIZE_ENABLED] ?: true
    }

    override val autoFolderPlacementFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_FOLDER_PLACEMENT] ?: false // Default: Suggestion mode
    }

    override val lastScanTimestampFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_SCAN_TIMESTAMP] ?: 0L
    }

    override val dismissedPackagesFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.DISMISSED_PACKAGES] ?: emptySet()
    }

    override suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GEMINI_API_KEY] = key
        }
    }

    override suspend fun setAutoCategorizeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CATEGORIZE_ENABLED] = enabled
        }
    }

    override suspend fun setAutoFolderPlacement(auto: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_FOLDER_PLACEMENT] = auto
        }
    }

    override suspend fun updateLastScanTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SCAN_TIMESTAMP] = timestamp
        }
    }

    override suspend fun dismissPackageSuggestion(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.DISMISSED_PACKAGES] ?: emptySet()
            prefs[Keys.DISMISSED_PACKAGES] = current + packageName
        }
    }
}
