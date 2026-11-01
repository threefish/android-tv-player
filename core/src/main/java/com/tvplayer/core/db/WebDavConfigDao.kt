package com.tvplayer.core.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WebDavConfigDao {
    
    @Query("SELECT * FROM webdav_config WHERE isEnabled = 1 ORDER BY id ASC LIMIT 1")
    suspend fun getActiveConfig(): WebDavConfigEntity?
    
    @Query("SELECT * FROM webdav_config WHERE isEnabled = 1 ORDER BY id ASC LIMIT 1")
    fun getActiveConfigFlow(): Flow<WebDavConfigEntity?>
    
    @Query("SELECT * FROM webdav_config WHERE id = :id")
    suspend fun getById(id: Long): WebDavConfigEntity?
    
    @Query("SELECT * FROM webdav_config ORDER BY id ASC")
    suspend fun getAll(): List<WebDavConfigEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: WebDavConfigEntity): Long
    
    @Update
    suspend fun update(config: WebDavConfigEntity)
    
    @Query("DELETE FROM webdav_config WHERE id = :id")
    suspend fun delete(id: Long)
    
    // 单条 SQL 完成切换，保证任何时刻最多只有一个激活配置
    @Query("UPDATE webdav_config SET isEnabled = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setActive(id: Long)
    
    @Query("UPDATE webdav_config SET lastSyncTime = :syncTime WHERE id = :id")
    suspend fun updateLastSyncTime(id: Long, syncTime: Long)
}
