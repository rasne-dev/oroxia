package com.oroxia.launcher.domain.categorizer

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.oroxia.launcher.data.local.dao.AppDao
import com.oroxia.launcher.data.local.dao.FolderDao
import com.oroxia.launcher.data.local.entity.AppEntity
import com.oroxia.launcher.data.local.entity.FolderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppScanner(
    private val context: Context,
    private val appDao: AppDao,
    private val folderDao: FolderDao,
    private val geminiCategorizer: GeminiCategorizer = GeminiCategorizer(),
    private val localCategorizer: LocalCategorizer = LocalCategorizer()
) {
    companion object {
        const val CACHE_VALIDITY_MS = 7 * 24 * 60 * 60 * 1000L // 7 days cache validity rule
    }

    suspend fun scanAndCategorizeInstalledApps(
        apiKey: String = "",
        forceRefresh: Boolean = false
    ): List<AppEntity> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        // Filter system apps, self package, and apps without a launchable activity
        val userApps = installed.filter { appInfo ->
            val isNotSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            val isNotSelf = appInfo.packageName != context.packageName
            val hasLaunchIntent = pm.getLaunchIntentForPackage(appInfo.packageName) != null
            isNotSystem && isNotSelf && hasLaunchIntent
        }

        val currentTime = System.currentTimeMillis()
        val cachedAppsMap = appDao.getAllAppsSync().associateBy { it.packageName }

        // Clean stale apps that are no longer installed on the device
        val installedPackageNames = userApps.map { it.packageName }.toSet()
        val stalePackages = cachedAppsMap.keys - installedPackageNames
        for (stalePkg in stalePackages) {
            appDao.deleteApp(stalePkg)
            com.oroxia.launcher.ui.common.IconCache.remove(stalePkg)
        }

        val appsToCategorize = mutableListOf<AppInfoForPrompt>()
        val finalAppEntities = mutableListOf<AppEntity>()
        val appsToUpdateInCache = mutableListOf<AppEntity>()

        for (appInfo in userApps) {
            val pkg = appInfo.packageName
            val label = pm.getApplicationLabel(appInfo).toString()
            val cached = cachedAppsMap[pkg]

            val isFresh = cached != null && (currentTime - cached.lastCategorizedAt) < CACHE_VALIDITY_MS

            if (!forceRefresh && isFresh) {
                var updatedCached = cached!!
                var changed = false

                // 1. Auto-heal deterministic category misclassifications (e.g., World Mobil in Tools)
                if (localCategorizer.isHighConfidenceFinance(label, pkg) &&
                    !updatedCached.category.equals(LocalCategorizer.CATEGORY_FINANCE, ignoreCase = true)
                ) {
                    updatedCached = updatedCached.copy(
                        category = LocalCategorizer.CATEGORY_FINANCE,
                        assignedFolderId = getFolderIdForCategory(LocalCategorizer.CATEGORY_FINANCE),
                        lastCategorizedAt = currentTime
                    )
                    changed = true
                } else if (localCategorizer.isHighConfidenceCareer(label, pkg) &&
                    !updatedCached.category.equals(LocalCategorizer.CATEGORY_CAREER, ignoreCase = true)
                ) {
                    updatedCached = updatedCached.copy(
                        category = LocalCategorizer.CATEGORY_CAREER,
                        assignedFolderId = getFolderIdForCategory(LocalCategorizer.CATEGORY_CAREER),
                        lastCategorizedAt = currentTime
                    )
                    changed = true
                }

                // 2. Ensure assignedFolderId is populated for any cached app that belongs to a category
                if (updatedCached.assignedFolderId == null &&
                    !updatedCached.category.equals(LocalCategorizer.CATEGORY_OTHER, ignoreCase = true)
                ) {
                    updatedCached = updatedCached.copy(
                        assignedFolderId = getFolderIdForCategory(updatedCached.category)
                    )
                    changed = true
                }

                if (changed) {
                    appsToUpdateInCache.add(updatedCached)
                }
                finalAppEntities.add(updatedCached)
            } else {
                appsToCategorize.add(AppInfoForPrompt(packageName = pkg, appName = label))
            }
        }

        if (appsToUpdateInCache.isNotEmpty()) {
            appDao.insertApps(appsToUpdateInCache)
        }

        if (appsToCategorize.isNotEmpty()) {
            val categories = if (apiKey.isNotBlank()) {
                geminiCategorizer.categorizeAppsBatch(apiKey, appsToCategorize)
            } else {
                appsToCategorize.associate { it.packageName to localCategorizer.categorizeApp(it.appName, it.packageName) }
            }

            val appsToSave = mutableListOf<AppEntity>()
            for (appInfo in appsToCategorize) {
                val category = categories[appInfo.packageName]
                    ?: localCategorizer.categorizeApp(appInfo.appName, appInfo.packageName)
                val existing = cachedAppsMap[appInfo.packageName]

                // Automatically assign app to its matching smart category folder (unless it's "Diğer")
                val categoryFolderId = if (!category.equals(LocalCategorizer.CATEGORY_OTHER, ignoreCase = true)) {
                    getFolderIdForCategory(category)
                } else {
                    null
                }

                val folderId = if (existing != null && existing.assignedFolderId != null && existing.category == category) {
                    existing.assignedFolderId
                } else {
                    categoryFolderId
                }

                val entity = AppEntity(
                    packageName = appInfo.packageName,
                    appName = appInfo.appName,
                    category = category,
                    isSystemApp = false,
                    installedAt = existing?.installedAt ?: currentTime,
                    lastCategorizedAt = currentTime,
                    assignedFolderId = folderId,
                    isPinnedToHome = existing?.isPinnedToHome ?: false
                )
                finalAppEntities.add(entity)
                appsToSave.add(entity)
            }
            appDao.insertApps(appsToSave)
        }

        // Synchronize default folders in Room if not exist
        ensureDefaultFoldersExist(finalAppEntities)

        return@withContext finalAppEntities
    }

    private suspend fun ensureDefaultFoldersExist(apps: List<AppEntity>) {
        val existingFolders = folderDao.getAllFoldersSync().associateBy { it.name }
        val categoriesInUse = apps.map { it.category }.distinct()

        var order = existingFolders.size
        for (category in categoriesInUse) {
            if (!category.equals(LocalCategorizer.CATEGORY_OTHER, ignoreCase = true) && !existingFolders.containsKey(category)) {
                val folder = FolderEntity(
                    id = getFolderIdForCategory(category),
                    name = category,
                    category = category,
                    orderIndex = order++,
                    isAutoCreated = true
                )
                folderDao.insertFolder(folder)
            }
        }
    }

    fun getFolderIdForCategory(category: String): String {
        val slug = category.lowercase()
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace("&", "ve")
            .replace(Regex("[^a-z0-9]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        return "folder_$slug"
    }
}
