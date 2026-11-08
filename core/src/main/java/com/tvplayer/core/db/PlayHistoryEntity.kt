package com.tvplayer.core.db

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "0")
    val playCount: Int = 0,
    val lastPlayedTime: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "1.0")
    val volume: Float = 1.0f,
    /** 是否真正调整过系统音量；旧版本存的音量是播放器增益，不能套用到系统音量上 */
    @ColumnInfo(defaultValue = "0")
    val volumeSet: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val resizeMode: Int = 0,
    /** null 表示该视频未单独调整过，沿用全局默认 */
    val playbackSpeed: Float? = null,
    val subtitleSize: Float? = null
)
