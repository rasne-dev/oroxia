package com.oroxia.launcher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val orderIndex: Int = 0,
    val isAutoCreated: Boolean = true
)
