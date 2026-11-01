package com.tvplayer.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_grid")
data class HomeGridEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderPath: String,
    val displayName: String,
    val sortOrder: Int = 0,
    val createTime: Long = System.currentTimeMillis()
)
