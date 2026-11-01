package com.tvplayer.core.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {
    
    @Query("SELECT * FROM play_history ORDER BY lastPlayedTime DESC")
    suspend fun getAll(): List<PlayHistoryEntity>
    
    @Query("SELECT * FROM play_history ORDER BY lastPlayedTime DESC")
    fun getAllFlow(): Flow<List<PlayHistoryEntity>>
    
    @Query("SELECT * FROM play_history ORDER BY lastPlayedTime DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<PlayHistoryEntity>
    
    @Query("SELECT * FROM play_history WHERE filePath = :filePath")
    suspend fun getByPath(filePath: String): PlayHistoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: PlayHistoryEntity): Long
    
    @Update
    suspend fun update(history: PlayHistoryEntity)
    
    @Query("DELETE FROM play_history WHERE id = :id")
    suspend fun delete(id: Long)
    
    @Query("DELETE FROM play_history")
    suspend fun clearAll()

    @Query("SELECT * FROM play_history WHERE filePath LIKE :folderPrefix || '/%' ORDER BY lastPlayedTime DESC LIMIT 1")
    suspend fun getLatestInFolder(folderPrefix: String): PlayHistoryEntity?
}
