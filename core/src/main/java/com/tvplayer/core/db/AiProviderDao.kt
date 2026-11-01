package com.tvplayer.core.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AiProviderDao {
    @Query("SELECT * FROM ai_providers ORDER BY updateTime DESC")
    suspend fun getAll(): List<AiProviderEntity>

    @Query("SELECT * FROM ai_providers ORDER BY updateTime DESC")
    fun getAllFlow(): Flow<List<AiProviderEntity>>

    @Query("SELECT * FROM ai_providers WHERE id = :id")
    suspend fun getById(id: Long): AiProviderEntity?

    @Query("SELECT * FROM ai_providers WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): AiProviderEntity?

    @Query("SELECT * FROM ai_providers WHERE isActive = 1 LIMIT 1")
    fun getActiveFlow(): Flow<AiProviderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(provider: AiProviderEntity): Long

    @Update
    suspend fun update(provider: AiProviderEntity)

    @Query("DELETE FROM ai_providers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE ai_providers SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE ai_providers SET isActive = 1, updateTime = :updateTime WHERE id = :id")
    suspend fun activate(id: Long, updateTime: Long = System.currentTimeMillis())
}
