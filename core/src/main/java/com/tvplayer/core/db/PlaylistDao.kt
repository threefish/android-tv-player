package com.tvplayer.core.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updateTime DESC")
    suspend fun getAll(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists ORDER BY updateTime DESC")
    fun getAllFlow(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE playlists SET trackPaths = :trackPaths, updateTime = :updateTime WHERE id = :playlistId")
    suspend fun updateTrackPaths(playlistId: Long, trackPaths: List<String>, updateTime: Long = System.currentTimeMillis())
}
