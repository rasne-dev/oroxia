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
    private val categorizer: LocalCategorizer = LocalCategorizer()
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

        // Filter system apps per constraints
        val userApps = installed.filter { appInfo ->
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }

        val currentTime = System.currentTimeMillis()
        val cachedAppsMap = appDao.getAllAppsSync().associateBy { it.packageName }
        val finalAppEntities = mutableListOf<AppEntity>()
        val appsToSave = mutableListOf<AppEntity>()

        for (appInfo in userApps) {
            val pkg = appInfo.packageName
            val label = pm.getApplicationLabel(appInfo).toString()
            val cached = cachedAppsMap[pkg]

            val isFresh = cached != null && (currentTime - cached.lastCategorizedAt) < CACHE_VALIDITY_MS

            if (!forceRefresh && isFresh) {
                finalAppEntities.add(cached!!)
            } else {
                // Categorize locally, instantly and accurately
                val category = categorizer.categorizeApp(label, pkg, appInfo)
                val entity = AppEntity(
                    packageName = pkg,
                    appName = label,
                    category = category,
                    isSystemApp = false,
                    installedAt = cached?.installedAt ?: currentTime,
                    lastCategorizedAt = currentTime,
                    assignedFolderId = cached?.assignedFolderId,
                    isPinnedToHome = cached?.isPinnedToHome ?: false
                )
                finalAppEntities.add(entity)
                appsToSave.add(entity)
            }
        }

        if (appsToSave.isNotEmpty()) {
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
            if (!existingFolders.containsKey(category)) {
                val folder = FolderEntity(
                    id = "folder_${category.lowercase().replace(" ", "_").replace("&", "ve")}",
                    name = category,
                    category = category,
                    orderIndex = order++,
                    isAutoCreated = true
                )
                folderDao.insertFolder(folder)
            }
        }
    }
}
