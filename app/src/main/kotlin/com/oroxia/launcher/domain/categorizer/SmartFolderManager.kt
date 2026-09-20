package com.oroxia.launcher.domain.categorizer

import com.oroxia.launcher.data.local.dao.AppDao
import com.oroxia.launcher.data.local.dao.FolderDao
import com.oroxia.launcher.data.local.entity.AppEntity
import com.oroxia.launcher.data.local.entity.FolderEntity
import com.oroxia.launcher.data.pref.PreferencesRepository
import kotlinx.coroutines.flow.first

data class FolderSuggestion(
    val app: AppEntity,
    val targetFolder: FolderEntity,
    val reason: String
)

class SmartFolderManager(
    private val appDao: AppDao,
    private val folderDao: FolderDao,
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun evaluateSuggestions(): List<FolderSuggestion> {
        val allApps = appDao.getAllAppsSync()
        val allFolders = folderDao.getAllFoldersSync()
        val dismissed = preferencesRepository.dismissedPackagesFlow.first()
        val autoPlace = preferencesRepository.autoFolderPlacementFlow.first()

        val suggestions = mutableListOf<FolderSuggestion>()

        for (app in allApps) {
            if (dismissed.contains(app.packageName)) continue
            // Do not generate suggestion cards for uncategorized/Other apps
            if (app.category.equals(LocalCategorizer.CATEGORY_OTHER, ignoreCase = true)) continue

            // If app is not yet assigned to a folder
            if (app.assignedFolderId == null) {
                val matchingFolder = allFolders.firstOrNull { it.category.equals(app.category, ignoreCase = true) }
                    ?: allFolders.firstOrNull { it.name.equals(app.category, ignoreCase = true) }

                if (matchingFolder != null) {
                    if (autoPlace) {
                        // Automatically assign if user enabled auto mode
                        appDao.assignFolder(app.packageName, matchingFolder.id)
                    } else {
                        // Generate smart suggestion
                        val reason = "Cihazınızda '${app.appName}' tespit edildi. '${matchingFolder.name}' klasörüne eklensin mi?"
                        suggestions.add(
                            FolderSuggestion(
                                app = app,
                                targetFolder = matchingFolder,
                                reason = reason
                            )
                        )
                    }
                }
            }
        }
        return suggestions
    }

    suspend fun acceptSuggestion(suggestion: FolderSuggestion) {
        appDao.assignFolder(suggestion.app.packageName, suggestion.targetFolder.id)
    }

    suspend fun dismissSuggestion(suggestion: FolderSuggestion) {
        preferencesRepository.dismissPackageSuggestion(suggestion.app.packageName)
    }

    suspend fun autoOrganizeAll() {
        val allApps = appDao.getAllAppsSync()
        val allFolders = folderDao.getAllFoldersSync()

        for (app in allApps) {
            // Keep uncategorized/Other apps in the individual apps section
            if (app.category.equals(LocalCategorizer.CATEGORY_OTHER, ignoreCase = true)) continue

            val matchingFolder = allFolders.firstOrNull { it.category.equals(app.category, ignoreCase = true) }
                ?: allFolders.firstOrNull { it.name.equals(app.category, ignoreCase = true) }

            if (matchingFolder != null) {
                appDao.assignFolder(app.packageName, matchingFolder.id)
            }
        }
    }
}
