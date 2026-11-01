package com.tvplayer.app.ui.audio

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvplayer.core.model.Playlist
import com.tvplayer.core.repository.PlaylistRepository
import com.tvplayer.player.AudioPlayerService
import com.tvplayer.player.AudioTrack
import com.tvplayer.player.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private var audioService: AudioPlayerService? = null
    private var bound = false

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPlaylist = MutableStateFlow<List<AudioTrack>>(emptyList())
    val currentPlaylist: StateFlow<List<AudioTrack>> = _currentPlaylist.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioPlayerService.AudioBinder
            audioService = binder.getService()
            bound = true
            
            // Collect service state
            viewModelScope.launch {
                audioService?.playbackState?.collect { state ->
                    _playbackState.value = state
                }
            }
            viewModelScope.launch {
                audioService?.currentIndex?.collect { index ->
                    _currentIndex.value = index
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            audioService = null
        }
    }

    init {
        bindService()
        loadPlaylists()
    }

    private fun bindService() {
        val intent = Intent(context, AudioPlayerService::class.java)
        context.startService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun setPlaylist(tracks: List<AudioTrack>) {
        _currentPlaylist.value = tracks
        audioService?.setPlaylist(tracks)
    }

    fun playTrack(index: Int) {
        audioService?.playTrack(index)
    }

    fun togglePlayPause() {
        when (_playbackState.value) {
            is PlaybackState.Playing -> audioService?.pause()
            is PlaybackState.Paused -> audioService?.play()
            else -> {}
        }
    }

    fun seekTo(position: Long) {
        audioService?.seekTo(position)
    }

    fun next() {
        audioService?.next()
    }

    fun previous() {
        audioService?.previous()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            _playlists.value = playlistRepository.getAll()
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            val playlist = Playlist(name = name, description = description)
            playlistRepository.create(playlist)
            loadPlaylists()
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            playlistRepository.delete(id)
            loadPlaylists()
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackPath: String) {
        viewModelScope.launch {
            playlistRepository.addTrackToPlaylist(playlistId, trackPath)
            loadPlaylists()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (bound) {
            context.unbindService(serviceConnection)
            bound = false
        }
    }
}
