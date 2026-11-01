package com.tvplayer.core.repository

import com.tvplayer.core.model.AiProviderConfig
import kotlinx.coroutines.flow.Flow

interface AiProviderRepository {
    suspend fun getAll(): List<AiProviderConfig>
    fun getAllFlow(): Flow<List<AiProviderConfig>>
    suspend fun getById(id: Long): AiProviderConfig?
    suspend fun getActive(): AiProviderConfig?
    fun getActiveFlow(): Flow<AiProviderConfig?>
    suspend fun save(config: AiProviderConfig): Long
    suspend fun update(config: AiProviderConfig)
    suspend fun delete(id: Long)
    suspend fun setActive(id: Long)
}
