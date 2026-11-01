package com.tvplayer.core.model

data class PlayHistory(
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val position: Long,
    val duration: Long,
    val lastPlayedTime: Long = System.currentTimeMillis()
)
