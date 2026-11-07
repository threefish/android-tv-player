package com.tvplayer.app.ui.filebrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvplayer.core.model.FileInfo
import com.tvplayer.core.repository.FileRepository
import com.tvplayer.core.repository.HomeGridRepository
import com.tvplayer.core.repository.PlayHistoryRepository
import com.tvplayer.core.repository.WebDavConfigRepository
import com.tvplayer.webdav.SyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val syncService: SyncService,
    private val webDavConfigRepository: WebDavConfigRepository,
    private val homeGridRepository: HomeGridRepository,
    private val playHistoryRepository: PlayHistoryRepository
) : ViewModel() {

    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _files = MutableStateFlow<List<FileInfo>>(emptyList())
    val files: StateFlow<List<FileInfo>> = _files.asStateFlow()

    val syncState = syncService.syncState
    val lastSyncTime = syncService.lastSyncTime

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _addToHomeResult = MutableStateFlow<String?>(null)
    val addToHomeResult: StateFlow<String?> = _addToHomeResult.asStateFlow()

    /** filePath → 播放次数，供列表行展示 */
    private val _playCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val playCounts: StateFlow<Map<String, Int>> = _playCounts.asStateFlow()

    init {
        // 起播计数、同步清理都会改动历史表，用 Flow 保持列表显示同步
        viewModelScope.launch {
            playHistoryRepository.getAllFlow().collect { histories ->
                _playCounts.value = histories
                    .filter { it.playCount > 0 }
                    .associate { it.filePath to it.playCount }
            }
        }
    }

    /** 返回上级时需要聚焦的目录 path，由页面在列表加载完成后消费 */
    private var pendingFocusPath: String? = null

    fun loadFiles(path: String) {
        viewModelScope.launch {
            // 先更新列表再切换路径，进入目录时页面拿到的就是新目录数据
            _files.value = fileRepository.getChildren(path)
            _currentPath.value = path
        }
    }

    fun navigateToParent() {
        val current = _currentPath.value
        if (current != "/") {
            pendingFocusPath = current
            val parent = current.substringBeforeLast("/", "/")
            loadFiles(if (parent.isEmpty()) "/" else parent)
        }
    }

    fun consumePendingFocusPath(): String? {
        val path = pendingFocusPath
        pendingFocusPath = null
        return path
    }

    fun refresh() {
        viewModelScope.launch {
            _files.value = fileRepository.getChildren(_currentPath.value)
        }
    }

    fun sync() {
        if (_isSyncing.value) return
        // 其它页面已触发同步时不再排队跑第二遍
        if (syncService.syncState.value is SyncService.SyncState.Syncing) return
        _isSyncing.value = true
        viewModelScope.launch {
            try {
                syncService.fullSync()
                refresh()
            } catch (e: Exception) {
                // Error is handled by SyncService state
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /** 本地还没有任何同步数据时才自动同步一次；已同步过的目录只由同步按钮触发 */
    fun syncIfEmpty() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        viewModelScope.launch {
            try {
                if (syncService.syncIfEmpty() != null) refresh()
            } catch (e: Exception) {
                // Error is handled by SyncService state
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun addToHomeGrid(path: String) {
        viewModelScope.launch {
            val displayName = path.trimEnd('/').substringAfterLast("/").ifEmpty { "根目录" }
            val existing = homeGridRepository.getByPath(path)
            if (existing != null) {
                _addToHomeResult.value = "已在首页中"
            } else {
                homeGridRepository.add(path, displayName)
                _addToHomeResult.value = "已添加到首页"
            }
        }
    }

    fun clearAddToHomeResult() {
        _addToHomeResult.value = null
    }

    fun deleteFile(path: String) {
        viewModelScope.launch {
            fileRepository.markAsDeleted(path)
            _addToHomeResult.value = "已删除"
            refresh()
        }
    }
}
