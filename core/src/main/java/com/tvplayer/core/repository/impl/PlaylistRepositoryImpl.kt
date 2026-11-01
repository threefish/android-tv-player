package com.tvplayer.core.repository.impl

import com.tvplayer.core.db.PlaylistDao
import com.tvplayer.core.db.PlaylistEntity
import com.tvplayer.core.model.Playlist
import com.tvplayer.core.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override suspend fun getAll(): List<Playlist> {
        return playlistDao.getAll().map { it.toPlaylist() }
    }

    override fun getAllFlow(): Flow<List<Playlist>> {
        return playlistDao.getAllFlow().map { list -> list.map { it.toPlaylist() } }
    }

    override suspend fun getById(id: Long): Playlist? {
        return playlistDao.getById(id)?.toPlaylist()
    }

    override suspend fun create(playlist: Playlist): Long {
        val entity = playlist.toEntity()
        return playlistDao.insert(entity)
    }

    override suspend fun update(playlist: Playlist) {
        val entity = playlist.copy(updateTime = System.currentTimeMillis()).toEntity()
        playlistDao.update(entity)
    }

    override suspend fun delete(id: Long) {
        playlistDao.deleteById(id)
    }

    override suspend fun addTrackToPlaylist(playlistId: Long, trackPath: String) {
        val playlist = playlistDao.getById(playlistId) ?: return
        val updatedTracks = playlist.trackPaths + trackPath
        playlistDao.updateTrackPaths(playlistId, updatedTracks)
    }

    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackPath: String) {
        val playlist = playlistDao.getById(playlistId) ?: return
        val updatedTracks = playlist.trackPaths - trackPath
        playlistDao.updateTrackPaths(playlistId, updatedTracks)
    }

    private fun PlaylistEntity.toPlaylist() = Playlist(
        id = id,
        name = name,
        description = description,
        trackPaths = trackPaths,
        createTime = createTime,
        updateTime = updateTime
    )

    private fun Playlist.toEntity() = PlaylistEntity(
        id = id,
        name = name,
        description = description,
        trackPaths = trackPaths,
        createTime = createTime,
        updateTime = updateTime
    )
}
