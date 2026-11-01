package com.tvplayer.app.ui.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvplayer.core.model.FileInfo
import com.tvplayer.core.repository.FileRepository
import com.tvplayer.core.repository.WebDavConfigRepository
import com.tvplayer.core.util.ImageSortMode
import com.tvplayer.core.util.ImageSorter
import com.tvplayer.webdav.SyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ImagesViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val syncService: SyncService,
    private val webDavConfigRepository: WebDavConfigRepository,
) : ViewModel() {

    private val _images = MutableStateFlow<List<FileInfo>>(emptyList())
    val images: StateFlow<List<FileInfo>> = _images.asStateFlow()

    private val _sortMode = MutableStateFlow(ImageSortMode.DIRECTORY)
    val sortMode: StateFlow<ImageSortMode> = _sortMode.asStateFlow()

    /** 当前 WebDAV 配置 id，作为图片缓存键的一部分 */
    private val _sourceId = MutableStateFlow(0L)
    val sourceId: StateFlow<Long> = _sourceId.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun load() {
        viewModelScope.launch { reload() }
    }

    fun setSortMode(mode: ImageSortMode) {
        if (_sortMode.value == mode) return
        _sortMode.value = mode
        val current = _images.value
        viewModelScope.launch {
            // 中文排序走 ICU，放主线程会阻塞遥控器输入甚至 ANR
            _images.value = withContext(Dispatchers.Default) { ImageSorter.sort(current, mode) }
        }
    }

    fun refresh() {
        if (_isSyncing.value) return
        // 其它页面已触发同步时不再排队跑第二遍
        if (syncService.syncState.value is SyncService.SyncState.Syncing) return
        _isSyncing.value = true
        viewModelScope.launch {
            try {
                syncService.fullSync()
                reload()
            } catch (e: Exception) {
                // 错误由 SyncService 的 syncState 暴露
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun reload() {
        _sourceId.value = webDavConfigRepository.getActiveConfig()?.id ?: 0L
        val sorted = withContext(Dispatchers.Default) {
            ImageSorter.sort(fileRepository.getImages(), _sortMode.value)
        }
        _images.value = sorted
    }
}
