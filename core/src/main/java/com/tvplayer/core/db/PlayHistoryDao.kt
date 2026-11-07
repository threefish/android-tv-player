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

    /** 删除指定路径及其子孙的历史记录，返回删除行数（用精确前缀，避免 LIKE 把 _ % 当通配符） */
    @Query("DELETE FROM play_history WHERE filePath = :path OR substr(filePath, 1, length(:path) + 1) = :path || '/'")
    suspend fun deleteByPathPrefix(path: String): Int
}
