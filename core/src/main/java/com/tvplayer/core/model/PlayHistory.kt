package com.tvplayer.core.model

data class PlayHistory(
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val position: Long,
    val duration: Long,
    val playCount: Int = 0,
    val lastPlayedTime: Long = System.currentTimeMillis(),
    val volume: Float = 1.0f,
    val volumeSet: Boolean = false,
    val resizeMode: Int = 0,
    /** null 表示该视频未单独调整过，沿用全局默认 */
    val playbackSpeed: Float? = null,
    val subtitleSize: Float? = null
)
