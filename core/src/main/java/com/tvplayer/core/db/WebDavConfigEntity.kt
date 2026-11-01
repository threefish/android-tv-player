package com.tvplayer.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "webdav_config")
data class WebDavConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val url: String,
    val username: String,
    val password: String,
    val rootPath: String = "/",
    val isEnabled: Boolean = true,
    val lastSyncTime: Long? = null
)
