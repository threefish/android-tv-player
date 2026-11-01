package com.tvplayer.core.repository

import com.tvplayer.core.model.WebDavConfig
import kotlinx.coroutines.flow.Flow

interface WebDavConfigRepository {
    suspend fun getActiveConfig(): WebDavConfig?
    fun getActiveConfigFlow(): Flow<WebDavConfig?>
    suspend fun getAll(): List<WebDavConfig>
    suspend fun save(config: WebDavConfig): Long
    suspend fun setActive(id: Long)
    suspend fun delete(id: Long)
    suspend fun testConnection(config: WebDavConfig): Boolean
}
