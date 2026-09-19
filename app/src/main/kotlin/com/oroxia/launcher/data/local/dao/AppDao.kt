package com.oroxia.launcher.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.oroxia.launcher.data.local.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY appName ASC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps ORDER BY appName ASC")
    suspend fun getAllAppsSync(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE assignedFolderId = :folderId ORDER BY appName ASC")
    fun getAppsByFolder(folderId: String): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE category = :category ORDER BY appName ASC")
    fun getAppsByCategory(category: String): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE isPinnedToHome = 1 ORDER BY appName ASC")
    fun getPinnedApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): AppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity)

    @Update
    suspend fun updateApp(app: AppEntity)

    @Query("UPDATE apps SET assignedFolderId = :folderId WHERE packageName = :packageName")
    suspend fun assignFolder(packageName: String, folderId: String?)

    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)
}
