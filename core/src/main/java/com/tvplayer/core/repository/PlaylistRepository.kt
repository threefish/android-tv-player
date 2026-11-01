package com.tvplayer.core.repository

import com.tvplayer.core.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    suspend fun getAll(): List<Playlist>
    fun getAllFlow(): Flow<List<Playlist>>
    suspend fun getById(id: Long): Playlist?
    suspend fun create(playlist: Playlist): Long
    suspend fun update(playlist: Playlist)
    suspend fun delete(id: Long)
    suspend fun addTrackToPlaylist(playlistId: Long, trackPath: String)
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackPath: String)
}
