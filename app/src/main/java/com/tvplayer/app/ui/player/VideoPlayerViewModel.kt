package com.tvplayer.app.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.tvplayer.core.model.FileInfo
import com.tvplayer.core.model.PlayHistory
import com.tvplayer.core.model.WebDavConfig
import com.tvplayer.core.repository.FileRepository
import com.tvplayer.core.repository.PlayHistoryRepository
import com.tvplayer.core.repository.WebDavConfigRepository
import com.tvplayer.player.PlayerService
import com.tvplayer.player.SubtitleFinder
import com.tvplayer.player.SubtitleInfo
import com.tvplayer.player.SubtitleSource
import com.tvplayer.player.TrackInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaybackTransition(
    val title: String,
    val index: Int,
    val total: Int,
    val isAuto: Boolean,
    val isAudio: Boolean
)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playerService: PlayerService,
    private val playHistoryRepository: PlayHistoryRepository,
    private val subtitleFinder: SubtitleFinder,
    private val fileRepository: FileRepository,
    private val webDavConfigRepository: WebDavConfigRepository
) : ViewModel() {

    private val videoPath: String = savedStateHandle.get<String>("videoPath") ?: ""
    private val folderPath: String? = savedStateHandle.get<String>("folderPath")

    // 退出时 ViewModel scope 已取消，最后一次历史写入需要独立 scope
    private val historyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _subtitles = MutableStateFlow<List<SubtitleInfo>>(emptyList())
    val subtitles: StateFlow<List<SubtitleInfo>> = _subtitles.asStateFlow()

    private val _isAudio = MutableStateFlow(false)
    val isAudio: StateFlow<Boolean> = _isAudio.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<TrackInfo>> = _audioTracks.asStateFlow()

    private val _textTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val textTracks: StateFlow<List<TrackInfo>> = _textTracks.asStateFlow()

    private val _savedPosition = MutableStateFlow(0L)
    val savedPosition: StateFlow<Long> = _savedPosition.asStateFlow()

    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    private val _previousName = MutableStateFlow("")
    val previousName: StateFlow<String> = _previousName.asStateFlow()

    private val _nextName = MutableStateFlow("")
    val nextName: StateFlow<String> = _nextName.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _transition = MutableStateFlow<PlaybackTransition?>(null)
    val transition: StateFlow<PlaybackTransition?> = _transition.asStateFlow()

    /** 整个目录的条目都播完后为 true，用于显示播放停止页 */
    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private var playlist: List<FileInfo> = emptyList()
    private var currentIndex: Int = -1

    val playbackState = playerService.playbackState
    val isPlaying = playerService.isPlaying
    val currentPosition = playerService.currentPosition
    val duration = playerService.duration
    val bufferSpeed = playerService.bufferSpeed
    val isSingleLoop = playerService.isSingleLoop
    val playbackSpeed = playerService.playbackSpeed
    val resizeMode = playerService.resizeMode
    val subtitleSize = playerService.subtitleSize
    val engine = playerService.engine
    val selectedSubtitleName = playerService.selectedSubtitleName

    fun getPlayer() = playerService.getPlayer()

    fun attachVlcView(layout: org.videolan.libvlc.util.VLCVideoLayout) {
        playerService.attachVlcView(layout)
    }

    fun detachVlcView() {
        playerService.detachVlcView()
    }

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                savePosition()
            }
        }
        // 历史必须在开播前加载完，否则会先用 0 开播、弹窗随后才出现
        viewModelScope.launch {
            if (folderPath != null) {
                if (loadFolderPlaylist()) _isReady.value = true
            } else {
                _currentTitle.value = videoPath.substringAfterLast("/")
                loadPlaylist()
                loadSubtitles()
                loadSavedPosition()
                _isReady.value = true
            }
        }
        viewModelScope.launch {
            playerService.playbackState.collect { state ->
                if (state is PlayerService.PlaybackState.Ended) {
                    onPlaybackEnded()
                }
            }
        }
    }

    private fun onPlaybackEnded() {
        if (playerService.isSingleLoop.value) return
        if (currentIndex in playlist.indices && currentIndex < playlist.size - 1) {
            playFile(playlist[currentIndex + 1], isAuto = true)
        } else {
            // 目录里的条目已全部播完，交给播放页展示停止页
            _finished.value = true
        }
    }

    private suspend fun loadFolderPlaylist(): Boolean {
        val folder = folderPath ?: return false
        val children = fileRepository.getChildren(folder).filter { !it.isDirectory }
        if (children.isEmpty()) return false

        val latest = playHistoryRepository.getLatestInFolder(folder)
        val latestFile = latest?.let { history -> children.firstOrNull { it.path == history.filePath } }
        // 优先续播历史记录，其次优先视频；只有音频的目录（如专辑）也能直接播放
        val startFile = latestFile ?: children.firstOrNull { it.isVideo } ?: children.first()
        playlist = children.filter { it.isSameKind(startFile) }
        currentIndex = playlist.indexOfFirst { it.path == startFile.path }.coerceAtLeast(0)
        val file = playlist[currentIndex]
        _currentTitle.value = file.name
        _isAudio.value = file.isAudio
        Log.d("VideoPlayerVM", "loadFolderPlaylist folder=$folder latest=${latest?.filePath} latestPos=${latest?.position} startIndex=$currentIndex startFile=${file.path}")

        if (latest != null && latest.filePath == file.path) {
            _savedPosition.value = latest.position
        }

        _subtitles.value = if (file.isAudio) emptyList() else subtitleFinder.findSubtitles(file.path)
        updateNeighborNames()
        return true
    }

    private suspend fun loadPlaylist() {
        val parentPath = videoPath.substringBeforeLast("/").ifEmpty { "/" }
        val current = fileRepository.getByPath(videoPath)
        val siblings = fileRepository.getChildren(parentPath).filter { !it.isDirectory }
        playlist = if (current != null) {
            siblings.filter { it.isSameKind(current) }
        } else {
            siblings.filter { !it.isAudio }
        }
        currentIndex = playlist.indexOfFirst { it.path == videoPath }
        _isAudio.value = current?.isAudio == true
        updateNeighborNames()
    }

    /** 视频与音频各自成播放列表，避免放完视频自动播到同目录的歌曲 */
    private fun FileInfo.isSameKind(other: FileInfo): Boolean =
        if (other.isAudio) isAudio else isVideo

    private fun updateNeighborNames() {
        if (playlist.isEmpty() || currentIndex < 0) {
            _previousName.value = ""
            _nextName.value = ""
            return
        }
        val prevIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
        val nextIndex = (currentIndex + 1) % playlist.size
        _previousName.value = playlist[prevIndex].name
        _nextName.value = playlist[nextIndex].name
    }

    private suspend fun loadSubtitles() {
        _subtitles.value = if (_isAudio.value) emptyList() else subtitleFinder.findSubtitles(videoPath)
    }

    private suspend fun loadSavedPosition() {
        val history = playHistoryRepository.getByPath(videoPath)
        if (history != null) {
            _savedPosition.value = history.position
        }
    }

    fun startPlayback(startPosition: Long? = null) {
        viewModelScope.launch {
            val config = webDavConfigRepository.getActiveConfig() ?: return@launch
            _finished.value = false
            val path = getCurrentFilePath()
            val position = startPosition ?: _savedPosition.value
            val isAudio = (playlist.getOrNull(currentIndex) ?: fileRepository.getByPath(path))?.isAudio == true
            _isAudio.value = isAudio
            val subtitles = if (isAudio) emptyList() else subtitleFinder.findSubtitles(path)
            _subtitles.value = subtitles
            Log.d("VideoPlayerVM", "startPlayback path=$path position=$position isAudio=$isAudio subtitles=${subtitles.size}")
            playerService.playVideo(
                videoUrl = buildVideoUrl(config, path),
                requestHeaders = authHeaders(config),
                startPosition = position,
                subtitles = buildSubtitleSources(config, subtitles)
            )
        }
    }

    private fun playFile(file: FileInfo, isAuto: Boolean = false) {
        savePosition()
        viewModelScope.launch {
            val config = webDavConfigRepository.getActiveConfig() ?: return@launch
            _finished.value = false
            currentIndex = playlist.indexOfFirst { it.path == file.path }
            _currentTitle.value = file.name
            _savedPosition.value = 0
            _isAudio.value = file.isAudio
            val subtitles = if (file.isAudio) emptyList() else subtitleFinder.findSubtitles(file.path)
            _subtitles.value = subtitles
            updateNeighborNames()
            _transition.value = PlaybackTransition(
                title = file.name,
                index = (currentIndex + 1).coerceAtLeast(0),
                total = playlist.size,
                isAuto = isAuto,
                isAudio = file.isAudio
            )
            playerService.playVideo(
                videoUrl = buildVideoUrl(config, file.path),
                requestHeaders = authHeaders(config),
                subtitles = buildSubtitleSources(config, subtitles)
            )
        }
    }

    fun clearTransition() {
        _transition.value = null
    }

    // 路径里的中文、空格和 + 必须编码，否则 ExoPlayer 拼出的 URL 会打到错误位置
    private fun buildVideoUrl(config: WebDavConfig, path: String): String =
        config.url.trimEnd('/') + "/" + Uri.encode(path.trimStart('/'), "/")

    private fun buildSubtitleSources(
        config: WebDavConfig,
        subtitles: List<SubtitleInfo>
    ): List<SubtitleSource> = subtitles.map { subtitle ->
        SubtitleSource(
            url = buildVideoUrl(config, subtitle.path),
            name = subtitle.name,
            mimeType = subtitle.mimeType
        )
    }

    private fun authHeaders(config: WebDavConfig): Map<String, String> {
        val raw = "${config.username}:${config.password}"
        val encoded = Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return mapOf("Authorization" to "Basic $encoded")
    }

    fun playNext() {
        if (playlist.isEmpty() || currentIndex < 0) return
        val nextIndex = (currentIndex + 1) % playlist.size
        playFile(playlist[nextIndex])
    }

    fun playPrevious() {
        if (playlist.isEmpty() || currentIndex < 0) return
        val prevIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
        playFile(playlist[prevIndex])
    }

    fun replay() {
        _finished.value = false
        playerService.seekTo(0)
        playerService.resume()
    }

    /** 停止页的重播：重新起播当前条目，不依赖 Ended 状态下的 seek */
    fun replayCurrent() {
        val file = playlist.getOrNull(currentIndex)
        if (file != null) playFile(file) else replay()
    }

    fun toggleRepeatMode() {
        playerService.toggleRepeatMode()
    }

    fun cyclePlaybackSpeed() {
        playerService.cyclePlaybackSpeed()
    }

    fun cycleResizeMode() {
        playerService.cycleResizeMode()
    }

    fun cycleSubtitleSize() {
        playerService.cycleSubtitleSize()
    }

    fun seekTo(position: Long) {
        playerService.seekTo(position)
    }

    fun setVolume(volume: Float) {
        playerService.setVolume(volume)
    }

    fun pause() {
        playerService.pause()
    }

    fun resume() {
        playerService.resume()
    }

    fun seekForward(ms: Long = 10_000L) {
        playerService.seekForward(ms)
    }

    fun seekBackward(ms: Long = 10_000L) {
        playerService.seekBackward(ms)
    }

    fun selectSubtitle(index: Int) {
        val subtitle = _subtitles.value.getOrNull(index) ?: return
        playerService.selectExternalSubtitle(subtitle.name)
    }

    fun disableSubtitles() {
        playerService.disableSubtitles()
    }

    fun selectAudioTrack(index: Int) {
        playerService.selectAudioTrack(index)
    }

    fun refreshTracks() {
        _audioTracks.value = playerService.getAvailableAudioTracks()
        _textTracks.value = playerService.getAvailableTextTracks()
    }

    private fun getCurrentFilePath(): String {
        return if (currentIndex in playlist.indices) playlist[currentIndex].path else videoPath
    }

    /** 返回列表时用于定位最后播放的条目 */
    fun lastPlayedPath(): String = getCurrentFilePath()

    fun savePosition() {
        val position = playerService.getCurrentPosition()
        val duration = playerService.getDuration()
        if (position <= 0 || duration <= 0) return
        val path = getCurrentFilePath()
        if (path.isBlank()) return
        Log.d("VideoPlayerVM", "savePosition path=$path position=$position duration=$duration")
        historyScope.launch {
            playHistoryRepository.save(PlayHistory(
                filePath = path,
                fileName = path.substringAfterLast("/"),
                position = position,
                duration = duration
            ))
        }
    }

    override fun onCleared() {
        super.onCleared()
        savePosition()
        playerService.release()
    }
}
