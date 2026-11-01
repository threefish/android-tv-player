package com.tvplayer.core.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "files",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["parentId"]),
        Index(value = ["modifiedTime"]),
        Index(value = ["isDeleted"])
    ]
)
data class FileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val modifiedTime: Long,
    val parentId: Long? = null,
    val isDeleted: Boolean = false,
    val deletedTime: Long? = null,
    val mimeType: String? = null,
    val duration: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val syncTime: Long = System.currentTimeMillis()
)
