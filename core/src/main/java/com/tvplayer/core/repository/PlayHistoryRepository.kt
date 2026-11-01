package com.tvplayer.core.repository

import com.tvplayer.core.model.PlayHistory
import kotlinx.coroutines.flow.Flow

interface PlayHistoryRepository {
    suspend fun getAll(): List<PlayHistory>
    fun getAllFlow(): Flow<List<PlayHistory>>
    suspend fun getRecent(limit: Int): List<PlayHistory>
    suspend fun getByPath(filePath: String): PlayHistory?
    suspend fun save(history: PlayHistory)
    suspend fun delete(id: Long)
    suspend fun clearAll()
    suspend fun getLatestInFolder(folderPath: String): PlayHistory?
}
