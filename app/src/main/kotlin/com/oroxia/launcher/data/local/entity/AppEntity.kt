package com.oroxia.launcher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val category: String,
    val isSystemApp: Boolean = false,
    val installedAt: Long = System.currentTimeMillis(),
    val lastCategorizedAt: Long = System.currentTimeMillis(),
    val assignedFolderId: String? = null,
    val isPinnedToHome: Boolean = false
)
