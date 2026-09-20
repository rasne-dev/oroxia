package com.oroxia.launcher.ui.home

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oroxia.launcher.OroxiaApplication
import com.oroxia.launcher.data.local.entity.AppEntity
import com.oroxia.launcher.data.local.entity.FolderEntity
import com.oroxia.launcher.domain.categorizer.AppScanner
import com.oroxia.launcher.domain.categorizer.FolderSuggestion
import com.oroxia.launcher.domain.categorizer.SmartFolderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class FolderWithApps(
    val folder: FolderEntity,
    val apps: List<AppEntity>
)

data class HomeUiState(
    val foldersWithApps: List<FolderWithApps> = emptyList(),
    val uncategorizedApps: List<AppEntity> = emptyList(),
    val suggestions: List<FolderSuggestion> = emptyList(),
    val isScanning: Boolean = false,
    val totalAppsCount: Int = 0,
    val message: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as OroxiaApplication
    private val db = app.database
    private val prefs = app.preferencesRepository

    private val appScanner = AppScanner(application, db.appDao(), db.folderDao())
    private val smartFolderManager = SmartFolderManager(db.appDao(), db.folderDao(), prefs)
    private val scanMutex = Mutex()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _suggestions = MutableStateFlow<List<FolderSuggestion>>(emptyList())
    val suggestions: StateFlow<List<FolderSuggestion>> = _suggestions.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        db.folderDao().getAllFolders(),
        db.appDao().getAllApps(),
        _suggestions,
        _isScanning,
        _userMessage
    ) { folders, apps, currentSuggestions, scanning, msg ->
        val appsByFolderId = apps.groupBy { it.assignedFolderId }
        val foldersWithApps = folders.map { folder ->
            FolderWithApps(
                folder = folder,
                apps = appsByFolderId[folder.id] ?: emptyList()
            )
        }.filter { it.apps.isNotEmpty() }

        val uncategorized = apps.filter { it.assignedFolderId == null }

        HomeUiState(
            foldersWithApps = foldersWithApps,
            uncategorizedApps = uncategorized,
            suggestions = currentSuggestions,
            isScanning = scanning,
            totalAppsCount = apps.size,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        refreshApps(force = false)
    }

    fun refreshApps(force: Boolean = false) {
        viewModelScope.launch {
            if (_isScanning.value) return@launch
            scanMutex.withLock {
                _isScanning.value = true
                try {
                    val apiKey = prefs.geminiApiKeyFlow.first()
                    appScanner.scanAndCategorizeInstalledApps(apiKey, forceRefresh = force)
                    prefs.updateLastScanTimestamp()
                    val autoPlace = prefs.autoFolderPlacementFlow.first()
                    if (autoPlace || force) {
                        smartFolderManager.autoOrganizeAll()
                    }
                    refreshSuggestions()
                } catch (e: Exception) {
                    _userMessage.value = "Tarama sırasında bir hata oluştu: ${e.localizedMessage}"
                } finally {
                    _isScanning.value = false
                }
            }
        }
    }

    fun refreshSuggestions() {
        viewModelScope.launch {
            try {
                val evaluated = smartFolderManager.evaluateSuggestions()
                _suggestions.value = evaluated
            } catch (e: Exception) {
                // Ignore suggestion refresh error
            }
        }
    }

    fun acceptSuggestion(suggestion: FolderSuggestion) {
        viewModelScope.launch {
            try {
                smartFolderManager.acceptSuggestion(suggestion)
                _suggestions.value = _suggestions.value.filter { it.app.packageName != suggestion.app.packageName }
                _userMessage.value = "'${suggestion.app.appName}', '${suggestion.targetFolder.name}' klasörüne eklendi."
            } catch (e: Exception) {
                _userMessage.value = "Öneri onaylanırken bir hata oluştu."
            }
        }
    }

    fun dismissSuggestion(suggestion: FolderSuggestion) {
        viewModelScope.launch {
            try {
                smartFolderManager.dismissSuggestion(suggestion)
                _suggestions.value = _suggestions.value.filter { it.app.packageName != suggestion.app.packageName }
            } catch (e: Exception) {
                // Ignore dismiss error
            }
        }
    }

    fun autoOrganizeAll() {
        viewModelScope.launch {
            if (_isScanning.value) return@launch
            scanMutex.withLock {
                _isScanning.value = true
                try {
                    smartFolderManager.autoOrganizeAll()
                    refreshSuggestions()
                    _userMessage.value = "Tüm uygulamalar otomatik klasörlendirildi!"
                } catch (e: Exception) {
                    _userMessage.value = "Klasörleme sırasında hata oluştu: ${e.localizedMessage}"
                } finally {
                    _isScanning.value = false
                }
            }
        }
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun launchApp(context: Context, packageName: String) {
        try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                _userMessage.value = "Uygulama başlatılamadı: Cihazda bulunamadı."
            }
        } catch (e: SecurityException) {
            _userMessage.value = "Güvenlik kısıtlaması nedeniyle uygulama açılamadı."
        } catch (e: Exception) {
            _userMessage.value = "Uygulama başlatılırken hata oluştu: ${e.localizedMessage}"
        }
    }
}
