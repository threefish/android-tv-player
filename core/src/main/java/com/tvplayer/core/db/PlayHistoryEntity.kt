package com.tvplayer.core.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "play_history",
    indices = [
        Index(value = ["filePath"]),
        Index(value = ["lastPlayedTime"])
    ]
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val position: Long,
    val duration: Long,
    val lastPlayedTime: Long = System.currentTimeMillis()
)
