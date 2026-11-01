package com.tvplayer.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioTrack(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val url: String,
    val duration: Long = 0
)

sealed class PlaybackState {
    object Idle : PlaybackState()
    object Loading : PlaybackState()
    data class Playing(val currentTrack: AudioTrack, val position: Long, val duration: Long) : PlaybackState()
    data class Paused(val currentTrack: AudioTrack, val position: Long, val duration: Long) : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}

class AudioPlayerService : Service() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _playlist = MutableStateFlow<List<AudioTrack>>(emptyList())
    val playlist: StateFlow<List<AudioTrack>> = _playlist.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val binder = AudioBinder()

    inner class AudioBinder : Binder() {
        fun getService(): AudioPlayerService = this@AudioPlayerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializePlayer()
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlaybackState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    _currentIndex.value = (_currentIndex.value + 1) % _playlist.value.size
                }
                updatePlaybackState()
            }
        })

        mediaSession = MediaSession.Builder(this, player!!)
            .build()
    }

    fun setPlaylist(tracks: List<AudioTrack>) {
        _playlist.value = tracks
        player?.clearMediaItems()
        tracks.forEach { track ->
            val mediaItem = MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.url)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                )
                .build()
            player?.addMediaItem(mediaItem)
        }
    }

    fun playTrack(index: Int) {
        if (index < 0 || index >= _playlist.value.size) return
        _currentIndex.value = index
        player?.seekToDefaultPosition(index)
        player?.playWhenReady = true
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun next() {
        if (_currentIndex.value < _playlist.value.size - 1) {
            playTrack(_currentIndex.value + 1)
        }
    }

    fun previous() {
        if (_currentIndex.value > 0) {
            playTrack(_currentIndex.value - 1)
        }
    }

    fun getCurrentPosition(): Long = player?.currentPosition ?: 0

    fun getDuration(): Long = player?.duration ?: 0

    private fun updatePlaybackState() {
        val currentPlayer = player ?: return
        
        when (currentPlayer.playbackState) {
            Player.STATE_IDLE -> {
                _playbackState.value = PlaybackState.Idle
            }
            Player.STATE_BUFFERING -> {
                _playbackState.value = PlaybackState.Loading
            }
            Player.STATE_READY -> {
                val currentTrack = _playlist.value.getOrNull(_currentIndex.value)
                if (currentTrack != null) {
                    if (currentPlayer.isPlaying) {
                        _playbackState.value = PlaybackState.Playing(
                            currentTrack = currentTrack,
                            position = currentPlayer.currentPosition,
                            duration = currentPlayer.duration
                        )
                    } else {
                        _playbackState.value = PlaybackState.Paused(
                            currentTrack = currentTrack,
                            position = currentPlayer.currentPosition,
                            duration = currentPlayer.duration
                        )
                    }
                }
            }
            Player.STATE_ENDED -> {
                // Auto-play next track
                if (_currentIndex.value < _playlist.value.size - 1) {
                    next()
                } else {
                    _playbackState.value = PlaybackState.Paused(
                        currentTrack = _playlist.value.lastOrNull() ?: return,
                        position = currentPlayer.duration,
                        duration = currentPlayer.duration
                    )
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "audio_playback",
                "音频播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "音频播放控制"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        player?.release()
        player = null
    }
}
