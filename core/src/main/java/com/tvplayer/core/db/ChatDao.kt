package com.tvplayer.core.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {
    
    @Query("SELECT * FROM chat_sessions ORDER BY updateTime DESC")
    suspend fun getAll(): List<ChatSessionEntity>
    
    @Query("SELECT * FROM chat_sessions ORDER BY updateTime DESC")
    fun getAllFlow(): Flow<List<ChatSessionEntity>>
    
    @Query("SELECT * FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun getBySessionId(sessionId: String): ChatSessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ChatSessionEntity): Long
    
    @Update
    suspend fun update(session: ChatSessionEntity)
    
    @Query("DELETE FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun delete(sessionId: String)
    
    @Query("UPDATE chat_sessions SET updateTime = :updateTime, title = :title WHERE sessionId = :sessionId")
    suspend fun updateTitle(sessionId: String, title: String, updateTime: Long = System.currentTimeMillis())
}

@Dao
interface ChatMessageDao {
    
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createTime ASC")
    suspend fun getBySessionId(sessionId: String): List<ChatMessageEntity>
    
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createTime ASC")
    fun getBySessionIdFlow(sessionId: String): Flow<List<ChatMessageEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)
    
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)
}
