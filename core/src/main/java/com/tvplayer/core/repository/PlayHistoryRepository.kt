package com.tvplayer.core.repository

import com.tvplayer.core.model.PlayHistory
import kotlinx.coroutines.flow.Flow

interface PlayHistoryRepository {
    suspend fun getAll(): List<PlayHistory>
    fun getAllFlow(): Flow<List<PlayHistory>>
    suspend fun getRecent(limit: Int): List<PlayHistory>
    suspend fun getByPath(filePath: String): PlayHistory?
    suspend fun save(history: PlayHistory)
    /** 记录一次起播：已存在则次数 +1，不存在则新建；不覆盖已保存的进度 */
    suspend fun recordPlayStart(filePath: String, fileName: String)
    /** 保存单个视频的播放设置；null 字段表示本次不修改该项 */
    suspend fun savePlaybackSettings(
        filePath: String,
        fileName: String,
        volume: Float? = null,
        resizeMode: Int? = null,
        playbackSpeed: Float? = null,
        subtitleSize: Float? = null
    )
    suspend fun delete(id: Long)
    suspend fun clearAll()
    suspend fun getLatestInFolder(folderPath: String): PlayHistory?
}
