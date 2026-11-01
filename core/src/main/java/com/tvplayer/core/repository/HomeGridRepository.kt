package com.tvplayer.core.repository

import com.tvplayer.core.model.HomeGridItem
import kotlinx.coroutines.flow.Flow

interface HomeGridRepository {
    suspend fun getAll(): List<HomeGridItem>
    fun getAllFlow(): Flow<List<HomeGridItem>>
    suspend fun getByPath(folderPath: String): HomeGridItem?
    suspend fun add(folderPath: String, displayName: String): Long
    suspend fun remove(id: Long)
}
