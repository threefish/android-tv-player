@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.tvplayer.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

enum class PlayerEngine { EXOPLAYER, VLC }

@Singleton
class PlayerService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var player: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var tickerStarted = false

    private var vlcLib: LibVLC? = null
    private var vlcPlayer: MediaPlayer? = null
    private var vlcVideoLayout: VLCVideoLayout? = null
    private var vlcPendingSeek = 0L
    private var vlcEnded = false
    private var vlcAudioTracks: Array<MediaPlayer.TrackDescription> = emptyArray()
    private var vlcSpuTracks: Array<MediaPlayer.TrackDescription> = emptyArray()

    private val _volume = MutableStateFlow(0f)
    /** 0~1 的系统媒体音量占比，与遥控器音量键操作的同一路音量 */
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1) == AudioManager.STREAM_MUSIC) {
                _volume.value = readSystemVolume()
            }
        }
    }

    init {
        _volume.value = readSystemVolume()
        val filter = IntentFilter(VOLUME_CHANGED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(volumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(volumeReceiver, filter)
        }
    }
    private var currentVideoUrl = ""
    private var currentRequestHeaders: Map<String, String> = emptyMap()
    private var vlcFallbackAttempted = false
    private var currentSubtitleSources: List<SubtitleSource> = emptyList()
    private var pendingSubtitleName: String? = null
    private var vlcSubtitleAutoPending = false

    private val _selectedSubtitleName = MutableStateFlow<String?>(null)
    val selectedSubtitleName: StateFlow<String?> = _selectedSubtitleName.asStateFlow()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _engine = MutableStateFlow(PlayerEngine.EXOPLAYER)
    val engine: StateFlow<PlayerEngine> = _engine.asStateFlow()

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _bufferSpeed = MutableStateFlow(0L)
    val bufferSpeed: StateFlow<Long> = _bufferSpeed.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isSingleLoop = MutableStateFlow(false)
    val isSingleLoop: StateFlow<Boolean> = _isSingleLoop.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _resizeMode = MutableStateFlow(0) // 0=FIT, 1=FILL, 2=ZOOM
    val resizeMode: StateFlow<Int> = _resizeMode.asStateFlow()

    private val _subtitleSize = MutableStateFlow(1.0f) // multiplier
    val subtitleSize: StateFlow<Float> = _subtitleSize.asStateFlow()

    private var lastBufferedPosition = 0L

    // WebDAV 需要 Basic 认证，请求头随每次播放设置
    @Volatile
    private var requestHeaders: Map<String, String> = emptyMap()

    sealed class PlaybackState {
        object Idle : PlaybackState()
        object Buffering : PlaybackState()
        object Ready : PlaybackState()
        object Ended : PlaybackState()
        data class Error(val message: String) : PlaybackState()
    }

    private val isVlc: Boolean
        get() = _engine.value == PlayerEngine.VLC

    fun initialize() {
        if (player == null) {
            trackSelector = DefaultTrackSelector(context)
            // 认证头在每次请求时解析，播放器本身只创建一次
            val resolvingFactory = ResolvingDataSource.Factory(
                DefaultHttpDataSource.Factory()
            ) { dataSpec ->
                if (requestHeaders.isEmpty()) dataSpec else dataSpec.withRequestHeaders(requestHeaders)
            }
            player = ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector!!)
                .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingFactory))
                .build()
            player?.repeatMode = if (_isSingleLoop.value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

            player?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    _playbackState.value = when (state) {
                        Player.STATE_IDLE -> PlaybackState.Idle
                        Player.STATE_BUFFERING -> PlaybackState.Buffering
                        Player.STATE_READY -> PlaybackState.Ready
                        Player.STATE_ENDED -> PlaybackState.Ended
                        else -> PlaybackState.Idle
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    updatePosition()
                }

                override fun onTracksChanged(tracks: Tracks) {
                    applyPendingExoSubtitle(tracks)
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("PlayerService", "播放失败: ${error.errorCodeName} ${error.message}", error)
                    // ExoPlayer 不支持该容器/编码时自动切到 libVLC 再试一次
                    if (!vlcFallbackAttempted && currentVideoUrl.isNotEmpty()) {
                        vlcFallbackAttempted = true
                        val position = player?.currentPosition ?: 0
                        Log.w("PlayerService", "ExoPlayer 无法播放，切换 libVLC: ${error.errorCodeName}")
                        pendingSubtitleName = currentSubtitleSources.firstOrNull()?.name
                        vlcSubtitleAutoPending = pendingSubtitleName != null
                        _selectedSubtitleName.value = pendingSubtitleName
                        playWithVlc(currentVideoUrl, currentRequestHeaders, position)
                        return
                    }
                    _playbackState.value = PlaybackState.Error(
                        "${error.errorCodeName}: ${error.message ?: "未知错误"}"
                    )
                }
            })

            if (!tickerStarted) {
                tickerStarted = true
                serviceScope.launch {
                    while (true) {
                        if (isVlc) {
                            vlcPlayer?.let { mp ->
                                if (_isPlaying.value) {
                                    val position = mp.time
                                    if (position >= 0) _currentPosition.value = position
                                    val length = mp.length
                                    if (length > 0) _duration.value = length
                                }
                            }
                        } else {
                            player?.let { p ->
                                if (p.isPlaying) {
                                    updatePosition()
                                }
                                if (p.playbackState == Player.STATE_BUFFERING) {
                                    val buffered = p.bufferedPosition
                                    val speed = (buffered - lastBufferedPosition) * 2
                                    _bufferSpeed.value = if (speed > 0) speed else 0
                                    lastBufferedPosition = buffered
                                } else {
                                    if (_bufferSpeed.value != 0L) {
                                        _bufferSpeed.value = 0
                                        lastBufferedPosition = 0
                                    }
                                }
                            }
                        }
                        delay(500)
                    }
                }
            }
        }
    }

    fun getPlayer(): ExoPlayer? {
        initialize()
        return player
    }

    fun playVideo(
        videoUrl: String,
        requestHeaders: Map<String, String> = emptyMap(),
        startPosition: Long = 0,
        subtitles: List<SubtitleSource> = emptyList()
    ) {
        initialize()
        currentVideoUrl = videoUrl
        currentRequestHeaders = requestHeaders
        this.requestHeaders = requestHeaders
        currentSubtitleSources = subtitles
        pendingSubtitleName = subtitles.firstOrNull()?.name
        vlcSubtitleAutoPending = pendingSubtitleName != null
        _selectedSubtitleName.value = pendingSubtitleName
        vlcFallbackAttempted = false
        if (isLegacyContainer(videoUrl)) {
            vlcFallbackAttempted = true
            playWithVlc(videoUrl, requestHeaders, startPosition)
        } else {
            playWithExo(videoUrl, startPosition)
        }
    }

    private fun playWithExo(videoUrl: String, startPosition: Long) {
        stopVlc()
        _engine.value = PlayerEngine.EXOPLAYER
        _bufferSpeed.value = 0
        lastBufferedPosition = 0
        player?.let { exoPlayer ->
            val mediaItem = MediaItem.Builder()
                .setUri(videoUrl)
                .apply {
                    if (currentSubtitleSources.isNotEmpty()) {
                        setSubtitleConfigurations(
                            currentSubtitleSources.map { subtitle ->
                                MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitle.url))
                                    .setMimeType(subtitle.mimeType)
                                    .setLabel(subtitle.name)
                                    .setId(subtitle.name)
                                    .build()
                            }
                        )
                    }
                }
                .build()
            // 上一部视频可能手动关过字幕，换片时恢复文本轨自动选择
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.seekTo(startPosition)
            // 播放器实例跨视频复用，起播时显式恢复用户设置的倍速
            exoPlayer.setPlaybackSpeed(_playbackSpeed.value)
            exoPlayer.playWhenReady = true
        }
    }

    private fun playWithVlc(videoUrl: String, headers: Map<String, String>, startPosition: Long) {
        val lib = try {
            ensureVlc()
        } catch (t: Throwable) {
            Log.e("PlayerService", "libVLC 初始化失败", t)
            val reason = t.message?.takeIf { it.isNotBlank() } ?: t.javaClass.simpleName
            _playbackState.value = PlaybackState.Error("解码引擎初始化失败: $reason")
            return
        }
        val mp = vlcPlayer ?: return

        player?.pause()
        _engine.value = PlayerEngine.VLC
        vlcEnded = false
        vlcPendingSeek = startPosition
        _bufferSpeed.value = 0
        _isPlaying.value = false
        _playbackState.value = PlaybackState.Buffering
        _currentPosition.value = startPosition.coerceAtLeast(0)
        _duration.value = 0

        // libVLC 3.x 的 HTTP 模块不读取 :http-user/:http-pwd 媒体选项，只认 URL 内嵌凭据
        val media = Media(lib, Uri.parse(urlWithCredentials(videoUrl, headers))).apply {
            setHWDecoderEnabled(true, false)
            addOption(":network-caching=2000")
            // 用 slave 而不是重复 :sub-file，多字幕才能同时加载并随时切换
            currentSubtitleSources.forEach { subtitle ->
                addSlave(
                    IMedia.Slave(
                        IMedia.Slave.Type.Subtitle,
                        SUBTITLE_SLAVE_PRIORITY,
                        urlWithCredentials(subtitle.url, headers)
                    )
                )
            }
        }
        mp.setMedia(media)
        media.release()
        mp.rate = _playbackSpeed.value
        mp.play()
        if (startPosition > 0) mp.time = startPosition
    }

    private fun ensureVlc(): LibVLC {
        vlcLib?.let { return it }
        val lib = LibVLC(
            context,
            arrayListOf(
                "--no-drop-late-frames",
                "--no-skip-frames",
                "--network-caching=2000"
            )
        )
        val mp = MediaPlayer(lib)
        mp.setEventListener(MediaPlayer.EventListener { event -> onVlcEvent(event) })
        vlcLib = lib
        vlcPlayer = mp
        return lib
    }

    private fun onVlcEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.Opening -> {
                _playbackState.value = PlaybackState.Buffering
            }
            MediaPlayer.Event.Playing -> {
                vlcEnded = false
                _playbackState.value = PlaybackState.Ready
                _isPlaying.value = true
                vlcPlayer?.let { mp ->
                    if (vlcPendingSeek > 0) {
                        mp.time = vlcPendingSeek
                        vlcPendingSeek = 0
                    }
                    val length = mp.length
                    if (length > 0) _duration.value = length
                }
                refreshVlcTracks()
                scheduleVlcSubtitleFallback()
            }
            MediaPlayer.Event.Paused -> {
                _isPlaying.value = false
                _playbackState.value = PlaybackState.Ready
            }
            MediaPlayer.Event.EndReached -> {
                vlcEnded = true
                _isPlaying.value = false
                if (_isSingleLoop.value) {
                    vlcPendingSeek = 0
                    vlcPlayer?.let { mp ->
                        mp.time = 0
                        mp.play()
                    }
                } else {
                    _playbackState.value = PlaybackState.Ended
                }
            }
            MediaPlayer.Event.Stopped -> {
                _isPlaying.value = false
                if (!vlcEnded) _playbackState.value = PlaybackState.Idle
            }
            MediaPlayer.Event.EncounteredError -> {
                _isPlaying.value = false
                _playbackState.value = PlaybackState.Error("该视频无法解码，可能是编码格式不受支持")
            }
            MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESDeleted, MediaPlayer.Event.Vout -> {
                refreshVlcTracks()
            }
            else -> {}
        }
    }

    private fun stopVlc() {
        vlcEnded = true
        vlcPlayer?.let { mp ->
            if (mp.isPlaying) mp.stop()
        }
    }

    fun attachVlcView(layout: VLCVideoLayout) {
        initialize()
        if (vlcVideoLayout === layout) return
        try {
            ensureVlc()
        } catch (t: Throwable) {
            Log.e("PlayerService", "libVLC 初始化失败", t)
            return
        }
        val mp = vlcPlayer ?: return
        runCatching { mp.detachViews() }
        vlcVideoLayout = layout
        // 使用 TextureView，避免 SurfaceView 在部分电视上与 Compose 叠层/截图冲突
        mp.attachViews(layout, null, false, true)
        mp.setVideoScale(vlcScaleType())
    }

    fun detachVlcView() {
        runCatching { vlcPlayer?.detachViews() }
        vlcVideoLayout = null
    }

    private fun refreshVlcTracks() {
        vlcPlayer?.let { mp ->
            vlcAudioTracks = runCatching { mp.audioTracks ?: emptyArray() }.getOrDefault(emptyArray())
            vlcSpuTracks = runCatching { mp.spuTracks ?: emptyArray() }.getOrDefault(emptyArray())
        }
        applyPendingVlcSubtitle()
    }

    fun playAudio(audioUrl: String, startPosition: Long = 0) {
        initialize()
        player?.let { exoPlayer ->
            val mediaItem = MediaItem.fromUri(audioUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.seekTo(startPosition)
            exoPlayer.playWhenReady = true
        }
    }

    fun pause() {
        if (isVlc) vlcPlayer?.pause() else player?.pause()
    }

    fun resume() {
        if (isVlc) vlcPlayer?.play() else player?.play()
    }

    /** 直接调整系统媒体音量，遥控器音量键与状态栏看到的是同一个值 */
    fun setVolume(volume: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val index = (volume.coerceIn(0f, 1f) * max).roundToInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
        _volume.value = readSystemVolume()
    }

    private fun readSystemVolume(): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max)
            .coerceIn(0f, 1f)
    }

    fun seekTo(position: Long) {
        if (isVlc) vlcPlayer?.time = position else player?.seekTo(position)
    }

    fun getCurrentPosition(): Long {
        return if (isVlc) (vlcPlayer?.time ?: 0L).coerceAtLeast(0) else (player?.currentPosition ?: 0)
    }

    fun getDuration(): Long {
        return if (isVlc) (vlcPlayer?.length ?: 0L).coerceAtLeast(0) else (player?.duration ?: 0)
    }

    fun selectTextTrack(index: Int) {
        if (isVlc) {
            vlcSpuTracks.getOrNull(index)?.let { vlcPlayer?.setSpuTrack(it.id) }
            return
        }
        player?.let { exoPlayer ->
            val trackGroups = exoPlayer.currentTracks.groups
            var textTrackCount = 0
            for (group in trackGroups) {
                if (group.type == androidx.media3.common.C.TRACK_TYPE_TEXT) {
                    if (textTrackCount == index) {
                        exoPlayer.setTrackSelectionParameters(
                            exoPlayer.trackSelectionParameters.buildUpon()
                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                                .build()
                        )
                        break
                    }
                    textTrackCount++
                }
            }
        }
    }

    private fun applyPendingExoSubtitle(tracks: Tracks) {
        val name = pendingSubtitleName ?: return
        val group = findExoTextGroup(tracks, name) ?: return
        selectExoTextGroup(group)
        _selectedSubtitleName.value = name
        pendingSubtitleName = null
    }

    private fun findExoTextGroup(tracks: Tracks, name: String): Tracks.Group? =
        tracks.groups.firstOrNull { group ->
            group.type == C.TRACK_TYPE_TEXT && (0 until group.length).any { index ->
                val format = group.getTrackFormat(index)
                format.label == name || format.id == name
            }
        }

    private fun selectExoTextGroup(group: Tracks.Group) {
        player?.let { exoPlayer ->
            exoPlayer.setTrackSelectionParameters(
                exoPlayer.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                    .build()
            )
        }
    }

    /** 按文件名选外部字幕；轨道尚未解析出来时先记下，等 onTracksChanged / ESAdded 再应用 */
    fun selectExternalSubtitle(name: String) {
        pendingSubtitleName = name
        vlcSubtitleAutoPending = false
        _selectedSubtitleName.value = name
        if (isVlc) {
            findVlcSubtitleTrack(name)?.let { track ->
                vlcPlayer?.setSpuTrack(track.id)
                pendingSubtitleName = null
            }
        } else {
            player?.currentTracks?.let { tracks ->
                findExoTextGroup(tracks, name)?.let { group ->
                    selectExoTextGroup(group)
                    pendingSubtitleName = null
                }
            }
        }
    }

    fun disableSubtitles() {
        pendingSubtitleName = null
        vlcSubtitleAutoPending = false
        _selectedSubtitleName.value = null
        if (isVlc) {
            vlcPlayer?.setSpuTrack(-1)
        } else {
            player?.let { exoPlayer ->
                exoPlayer.setTrackSelectionParameters(
                    exoPlayer.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                )
            }
        }
    }

    private fun applyPendingVlcSubtitle() {
        val name = pendingSubtitleName ?: return
        val track = findVlcSubtitleTrack(name) ?: return
        vlcPlayer?.setSpuTrack(track.id)
        pendingSubtitleName = null
        vlcSubtitleAutoPending = false
    }

    private fun findVlcSubtitleTrack(name: String): MediaPlayer.TrackDescription? {
        val target = name.substringAfterLast('/').substringBeforeLast('.').lowercase()
        return vlcSpuTracks.firstOrNull { track ->
            val trackName = track.name?.substringAfterLast('/')?.substringBeforeLast('.')?.lowercase()
            track.id >= 0 && trackName == target
        }
    }

    /** VLC 的轨道名对不上字幕文件名时，等轨道枚举完后退回第一条字幕轨 */
    private fun scheduleVlcSubtitleFallback() {
        if (!vlcSubtitleAutoPending || pendingSubtitleName == null) return
        serviceScope.launch {
            repeat(6) {
                delay(500)
                val name = pendingSubtitleName ?: return@launch
                if (!vlcSubtitleAutoPending) return@launch
                val track = findVlcSubtitleTrack(name) ?: vlcSpuTracks.firstOrNull { it.id >= 0 }
                if (track != null) {
                    vlcPlayer?.setSpuTrack(track.id)
                    pendingSubtitleName = null
                    vlcSubtitleAutoPending = false
                    return@launch
                }
            }
        }
    }

    fun selectAudioTrack(index: Int) {
        if (isVlc) {
            vlcAudioTracks.getOrNull(index)?.let { vlcPlayer?.setAudioTrack(it.id) }
            return
        }
        player?.let { exoPlayer ->
            val trackGroups = exoPlayer.currentTracks.groups
            var audioTrackCount = 0
            for (group in trackGroups) {
                if (group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                    if (audioTrackCount == index) {
                        exoPlayer.setTrackSelectionParameters(
                            exoPlayer.trackSelectionParameters.buildUpon()
                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                                .build()
                        )
                        break
                    }
                    audioTrackCount++
                }
            }
        }
    }

    fun getAvailableTextTracks(): List<TrackInfo> {
        if (isVlc) {
            return vlcSpuTracks.mapIndexed { index, track ->
                TrackInfo(index = index, label = track.name ?: "字幕 ${index + 1}", language = null)
            }
        }
        val tracks = mutableListOf<TrackInfo>()
        player?.let { exoPlayer ->
            val trackGroups = exoPlayer.currentTracks.groups
            var index = 0
            for (group in trackGroups) {
                if (group.type == androidx.media3.common.C.TRACK_TYPE_TEXT) {
                    val format = group.getTrackFormat(0)
                    tracks.add(
                        TrackInfo(
                            index = index,
                            label = format.label ?: "字幕 ${index + 1}",
                            language = format.language
                        )
                    )
                    index++
                }
            }
        }
        return tracks
    }

    fun getAvailableAudioTracks(): List<TrackInfo> {
        if (isVlc) {
            return vlcAudioTracks.mapIndexed { index, track ->
                TrackInfo(index = index, label = track.name ?: "音轨 ${index + 1}", language = null)
            }
        }
        val tracks = mutableListOf<TrackInfo>()
        player?.let { exoPlayer ->
            val trackGroups = exoPlayer.currentTracks.groups
            var index = 0
            for (group in trackGroups) {
                if (group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                    val format = group.getTrackFormat(0)
                    tracks.add(
                        TrackInfo(
                            index = index,
                            label = format.label ?: "音轨 ${index + 1}",
                            language = format.language
                        )
                    )
                    index++
                }
            }
        }
        return tracks
    }

    fun toggleRepeatMode() {
        val newLoop = !_isSingleLoop.value
        _isSingleLoop.value = newLoop
        player?.repeatMode = if (newLoop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    private val speedSteps = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player?.setPlaybackSpeed(speed)
        vlcPlayer?.rate = speed
    }

    fun cyclePlaybackSpeed() {
        val currentIdx = speedSteps.indexOf(_playbackSpeed.value)
        val nextIdx = (currentIdx + 1) % speedSteps.size
        setPlaybackSpeed(speedSteps[nextIdx])
    }

    private val resizeModeSteps = listOf(0, 1, 2) // FIT, FILL, ZOOM

    fun setResizeMode(mode: Int) {
        if (mode !in resizeModeSteps) return
        _resizeMode.value = mode
        if (isVlc) vlcPlayer?.setVideoScale(vlcScaleType())
    }

    fun cycleResizeMode() {
        val currentIdx = resizeModeSteps.indexOf(_resizeMode.value)
        val nextIdx = (currentIdx + 1) % resizeModeSteps.size
        setResizeMode(resizeModeSteps[nextIdx])
    }

    private fun vlcScaleType(): MediaPlayer.ScaleType = when (_resizeMode.value) {
        1 -> MediaPlayer.ScaleType.SURFACE_FILL
        2 -> MediaPlayer.ScaleType.SURFACE_FIT_SCREEN
        else -> MediaPlayer.ScaleType.SURFACE_BEST_FIT
    }

    fun seekForward(ms: Long = 10_000L) {
        if (isVlc) {
            val target = (getCurrentPosition() + ms).coerceAtMost(getDuration().coerceAtLeast(0))
            vlcPlayer?.time = target
            return
        }
        player?.let { p ->
            val newPos = (p.currentPosition + ms).coerceAtMost(p.duration.coerceAtLeast(0))
            p.seekTo(newPos)
        }
    }

    fun seekBackward(ms: Long = 10_000L) {
        if (isVlc) {
            val target = (getCurrentPosition() - ms).coerceAtLeast(0)
            vlcPlayer?.time = target
            return
        }
        player?.let { p ->
            val newPos = (p.currentPosition - ms).coerceAtLeast(0)
            p.seekTo(newPos)
        }
    }

    private val subtitleSizeSteps = listOf(0.8f, 1.0f, 1.2f, 1.5f, 2.0f)

    fun setSubtitleSize(size: Float) {
        _subtitleSize.value = size.coerceIn(0.5f, 3f)
    }

    fun cycleSubtitleSize() {
        val currentIdx = subtitleSizeSteps.indexOf(_subtitleSize.value)
        val nextIdx = if (currentIdx < 0) 1 else (currentIdx + 1) % subtitleSizeSteps.size
        setSubtitleSize(subtitleSizeSteps[nextIdx])
    }

    private fun updatePosition() {
        _currentPosition.value = player?.currentPosition ?: 0
        _duration.value = player?.duration ?: 0
    }

    fun release() {
        _isPlaying.value = false
        // 单例跨播放页复用：不清零会让下一页误判 Ready/旧进度，休息后 resume 空转
        _playbackState.value = PlaybackState.Idle
        _currentPosition.value = 0L
        _duration.value = 0L
        player?.release()
        player = null
        runCatching { vlcPlayer?.detachViews() }
        vlcPlayer?.let { mp ->
            runCatching { mp.stop() }
            mp.release()
        }
        vlcPlayer = null
        vlcLib?.release()
        vlcLib = null
        vlcVideoLayout = null
    }

    private fun isLegacyContainer(url: String): Boolean {
        val name = url.substringBefore('?').substringAfterLast('/')
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in VLC_CONTAINERS
    }

    /** 把 Basic 凭据嵌入 URL，libVLC 的 HTTP 输入会从 userinfo 里取账号密码 */
    private fun urlWithCredentials(url: String, headers: Map<String, String>): String {
        val (user, password) = authCredentials(headers) ?: return url
        val uri = Uri.parse(url)
        if (uri.scheme != "http" && uri.scheme != "https") return url
        val authority = uri.encodedAuthority ?: return url
        if (authority.contains('@')) return url
        val encodedAuthority = "${Uri.encode(user)}:${Uri.encode(password)}@$authority"
        return uri.buildUpon().encodedAuthority(encodedAuthority).build().toString()
    }

    /** 从 Basic 认证头里取出账号密码 */
    private fun authCredentials(headers: Map<String, String>): Pair<String, String>? {
        val auth = headers.entries.firstOrNull { it.key.equals("Authorization", true) }?.value ?: return null
        if (!auth.startsWith("Basic ", ignoreCase = true)) return null
        val raw = runCatching {
            String(Base64.decode(auth.substring(6).trim(), Base64.DEFAULT), Charsets.UTF_8)
        }.getOrNull() ?: return null
        val user = raw.substringBefore(':')
        val password = raw.substringAfter(':', "")
        return user to password
    }

    companion object {
        // 系统音量变化广播（AudioManager 里的同名常量是 @hide）
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"

        // libVLC slave 优先级：0 最低、4 最高
        private const val SUBTITLE_SLAVE_PRIORITY = 4

        // ExoPlayer 不支持的旧容器/音频格式交给 libVLC
        private val VLC_CONTAINERS = setOf(
            "rm", "rmvb", "avi", "wmv", "asf", "divx", "xvid",
            "mpg", "mpeg", "vob", "ogv", "m2v",
            "wma", "ape", "wv"
        )
    }
}

data class TrackInfo(
    val index: Int,
    val label: String,
    val language: String?
)

data class SubtitleSource(
    val url: String,
    val name: String,
    val mimeType: String
)
