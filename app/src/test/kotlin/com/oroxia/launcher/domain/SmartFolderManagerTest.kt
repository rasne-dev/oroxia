package com.oroxia.launcher.domain

import com.oroxia.launcher.data.local.dao.AppDao
import com.oroxia.launcher.data.local.dao.FolderDao
import com.oroxia.launcher.data.local.entity.AppEntity
import com.oroxia.launcher.data.local.entity.FolderEntity
import com.oroxia.launcher.data.pref.PreferencesRepository
import com.oroxia.launcher.domain.categorizer.SmartFolderManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmartFolderManagerTest {

    private class FakeAppDao : AppDao {
        val apps = mutableListOf<AppEntity>()

        override fun getAllApps(): Flow<List<AppEntity>> = flowOf(apps)
        override suspend fun getAllAppsSync(): List<AppEntity> = apps.toList()
        override fun getAppsByFolder(folderId: String): Flow<List<AppEntity>> =
            flowOf(apps.filter { it.assignedFolderId == folderId })
        override fun getAppsByCategory(category: String): Flow<List<AppEntity>> =
            flowOf(apps.filter { it.category == category })
        override fun getPinnedApps(): Flow<List<AppEntity>> =
            flowOf(apps.filter { it.isPinnedToHome })
        override suspend fun getApp(packageName: String): AppEntity? =
            apps.firstOrNull { it.packageName == packageName }
        override suspend fun insertApps(newApps: List<AppEntity>) {
            apps.removeAll { a -> newApps.any { it.packageName == a.packageName } }
            apps.addAll(newApps)
        }
        override suspend fun insertApp(app: AppEntity) {
            apps.removeAll { it.packageName == app.packageName }
            apps.add(app)
        }
        override suspend fun updateApp(app: AppEntity) {
            insertApp(app)
        }
        override suspend fun assignFolder(packageName: String, folderId: String?) {
            val idx = apps.indexOfFirst { it.packageName == packageName }
            if (idx != -1) {
                apps[idx] = apps[idx].copy(assignedFolderId = folderId)
            }
        }
        override suspend fun deleteApp(packageName: String) {
            apps.removeAll { it.packageName == packageName }
        }
    }

    private class FakeFolderDao : FolderDao {
        val folders = mutableListOf<FolderEntity>()

        override fun getAllFolders(): Flow<List<FolderEntity>> = flowOf(folders)
        override suspend fun getAllFoldersSync(): List<FolderEntity> = folders.toList()
        override suspend fun getFolderById(id: String): FolderEntity? =
            folders.firstOrNull { it.id == id }
        override suspend fun getFolderByCategory(category: String): FolderEntity? =
            folders.firstOrNull { it.category == category }
        override suspend fun insertFolders(newFolders: List<FolderEntity>) {
            folders.addAll(newFolders)
        }
        override suspend fun insertFolder(folder: FolderEntity) {
            folders.add(folder)
        }
        override suspend fun updateFolder(folder: FolderEntity) {}
        override suspend fun deleteFolder(id: String) {
            folders.removeAll { it.id == id }
        }
    }

    private class FakePreferencesRepository(
        initialDismissed: Set<String> = emptySet(),
        initialAutoPlace: Boolean = false
    ) : PreferencesRepository {
        val dismissed = MutableStateFlow(initialDismissed)
        val autoPlace = MutableStateFlow(initialAutoPlace)
        val apiKey = MutableStateFlow("test_key")
        val autoCategorize = MutableStateFlow(true)
        val lastScan = MutableStateFlow(0L)

        override val geminiApiKeyFlow: Flow<String> = apiKey
        override val autoCategorizeEnabledFlow: Flow<Boolean> = autoCategorize
        override val autoFolderPlacementFlow: Flow<Boolean> = autoPlace
        override val lastScanTimestampFlow: Flow<Long> = lastScan
        override val dismissedPackagesFlow: Flow<Set<String>> = dismissed

        override suspend fun setGeminiApiKey(key: String) { apiKey.value = key }
        override suspend fun setAutoCategorizeEnabled(enabled: Boolean) { autoCategorize.value = enabled }
        override suspend fun setAutoFolderPlacement(auto: Boolean) { autoPlace.value = auto }
        override suspend fun updateLastScanTimestamp(timestamp: Long) { lastScan.value = timestamp }
        override suspend fun dismissPackageSuggestion(packageName: String) {
            dismissed.value = dismissed.value + packageName
        }
    }

    private lateinit var appDao: FakeAppDao
    private lateinit var folderDao: FakeFolderDao
    private lateinit var prefsRepo: FakePreferencesRepository
    private lateinit var smartFolderManager: SmartFolderManager

    @Before
    fun setUp() {
        appDao = FakeAppDao()
        folderDao = FakeFolderDao()
        prefsRepo = FakePreferencesRepository()
        smartFolderManager = SmartFolderManager(appDao, folderDao, prefsRepo)
    }

    @Test
    fun testEvaluateSuggestions_SuggestsCareerFolderForCareerApp() = runBlocking {
        // Setup existing folder: "Kariyer" (e.g. from LinkedIn)
        folderDao.insertFolder(FolderEntity(id = "folder_kariyer", name = "Kariyer", category = "Kariyer"))

        // Add a new app: Kariyer.net with category "Kariyer" but no assignedFolderId
        appDao.insertApp(AppEntity(packageName = "net.kariyer.android", appName = "Kariyer.net", category = "Kariyer"))

        val suggestions = smartFolderManager.evaluateSuggestions()
        assertEquals(1, suggestions.size)
        assertEquals("net.kariyer.android", suggestions[0].app.packageName)
        assertEquals("folder_kariyer", suggestions[0].targetFolder.id)
        assertTrue(suggestions[0].reason.contains("Kariyer.net"))
        assertTrue(suggestions[0].reason.contains("Kariyer"))
    }

    @Test
    fun testAcceptSuggestion_AssignsFolder() = runBlocking {
        val folder = FolderEntity(id = "folder_kariyer", name = "Kariyer", category = "Kariyer")
        folderDao.insertFolder(folder)

        val app = AppEntity(packageName = "net.kariyer.android", appName = "Kariyer.net", category = "Kariyer")
        appDao.insertApp(app)

        val suggestions = smartFolderManager.evaluateSuggestions()
        smartFolderManager.acceptSuggestion(suggestions[0])

        val updated = appDao.getApp("net.kariyer.android")
        assertEquals("folder_kariyer", updated?.assignedFolderId)
    }

    @Test
    fun testAutoOrganizeAll_MatchesCategoryToFolder() = runBlocking {
        folderDao.insertFolder(FolderEntity(id = "folder_kariyer", name = "Kariyer", category = "Kariyer"))
        folderDao.insertFolder(FolderEntity(id = "folder_finans", name = "Finans", category = "Finans"))

        appDao.insertApp(AppEntity(packageName = "com.linkedin.android", appName = "LinkedIn", category = "Kariyer"))
        appDao.insertApp(AppEntity(packageName = "com.garanti.mobile", appName = "Garanti", category = "Finans"))

        smartFolderManager.autoOrganizeAll()

        val updatedApps = appDao.getAllAppsSync()
        assertEquals("folder_kariyer", updatedApps.first { it.packageName == "com.linkedin.android" }.assignedFolderId)
        assertEquals("folder_finans", updatedApps.first { it.packageName == "com.garanti.mobile" }.assignedFolderId)
    }
}
