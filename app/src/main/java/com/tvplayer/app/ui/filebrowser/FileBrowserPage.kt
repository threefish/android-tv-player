package com.tvplayer.app.ui.filebrowser

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.tvplayer.app.ui.component.AppIconButton
import com.tvplayer.app.ui.component.AppListRow
import com.tvplayer.app.ui.player.VideoPlayerActivity
import com.tvplayer.app.ui.theme.AppColors
import com.tvplayer.app.ui.theme.AppShapes
import com.tvplayer.app.ui.theme.Spacing
import com.tvplayer.core.model.FileInfo
import com.tvplayer.webdav.SyncService

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileBrowserPage(
    initialPath: String? = null,
    isActive: Boolean = true,
    refreshTick: Int = 0,
    viewModel: FileBrowserViewModel = hiltViewModel()
) {
    val currentPath by viewModel.currentPath.collectAsState()
    val files by viewModel.files.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val playCounts by viewModel.playCounts.collectAsState()
    val addToHomeResult by viewModel.addToHomeResult.collectAsState()

    var contextMenuFile by remember { mutableStateOf<FileInfo?>(null) }
    var focusedFile by remember { mutableStateOf<FileInfo?>(null) }
    var showSyncResult by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val itemFocusRequester = remember { FocusRequester() }
    var focusIndex by remember { mutableIntStateOf(-1) }
    // 页面内已经处理过焦点的目录，用于区分「进入新目录」和「同目录刷新」
    var focusedPath by remember { mutableStateOf(currentPath) }
    // 从播放器返回后需要重新聚焦的条目
    val resumeFocusPath = remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val syncing = syncState as? SyncService.SyncState.Syncing

    // 返回键先退回上级目录；停在根目录时交给 MainActivity 弹出退出确认。隐藏时不能劫持其它页的返回键
    BackHandler(enabled = isActive && currentPath != "/") {
        viewModel.navigateToParent()
    }

    LaunchedEffect(Unit) {
        viewModel.syncIfEmpty()
    }

    // 从首页进入指定目录；initialPath 被清空只代表离开页面，不重置本地目录
    LaunchedEffect(initialPath, refreshTick) {
        if (initialPath != null) {
            viewModel.loadFiles(initialPath)
        } else if (refreshTick > 0) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(currentPath) {
        focusedFile = null
        if (currentPath == focusedPath) return@LaunchedEffect
        focusedPath = currentPath

        val targetPath = viewModel.consumePendingFocusPath()
        if (files.isEmpty()) return@LaunchedEffect

        val targetIndex = targetPath?.let { path -> files.indexOfFirst { it.path == path } } ?: -1
        // 返回上级时让目标项上方保留两条，避免直接贴住顶部
        listState.scrollToItem(if (targetIndex >= 2) targetIndex - 2 else 0)
        focusIndex = if (targetIndex >= 0) targetIndex else 0
        // 列表按位置复用，等布局完成后再聚焦，避免停在与上一目录相同的索引上
        withFrameNanos { }
        withFrameNanos { }
        runCatching { itemFocusRequester.requestFocus() }
    }

    // 播放器返回时定位到刚播放的条目：优先用播放器回传的路径（可能已切集），否则用进入时的条目
    val playerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra("lastPlayedPath")?.let { resumeFocusPath.value = it }
        }
        val path = resumeFocusPath.value ?: return@rememberLauncherForActivityResult
        resumeFocusPath.value = null
        val index = viewModel.files.value.indexOfFirst { it.path == path }
        if (index < 0) return@rememberLauncherForActivityResult
        scope.launch {
            listState.scrollToItem((index - 2).coerceAtLeast(0))
            focusIndex = index
            withFrameNanos { }
            withFrameNanos { }
            runCatching { itemFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(syncState) {
        when (syncState) {
            is SyncService.SyncState.Completed, is SyncService.SyncState.Error -> {
                showSyncResult = true
            }
            else -> {}
        }
    }

    LaunchedEffect(addToHomeResult) {
        if (addToHomeResult != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearAddToHomeResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                // 遥控器无法触发触摸长按，用菜单键打开当前聚焦项的上下文菜单
                if (event.key == Key.Menu && event.type == KeyEventType.KeyDown && contextMenuFile == null) {
                    focusedFile?.let {
                        contextMenuFile = it
                        true
                    } ?: false
                } else {
                    false
                }
            }
    ) {
        // Page header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xxxl, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (currentPath != "/") {
                AppIconButton(onClick = { viewModel.navigateToParent() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = AppColors.listHeadline
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "视频",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.listHeadline
                )
                Text(
                    text = currentPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.listCaption
                )
            }

            // 同步中的路径提示与同步按钮放在同一容器里，避免底部浮层遮挡列表
            Row(
                modifier = Modifier.background(
                    if (syncing != null) AppColors.surfaceVariant else Color.Transparent,
                    AppShapes.medium,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (syncing != null) {
                    Text(
                        text = "同步中 · " + syncing.path.trimEnd('/').substringAfterLast('/')
                            .ifEmpty { "根目录" },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.listCaption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = 220.dp)
                            .padding(start = Spacing.md, end = Spacing.sm),
                    )
                }

                // 同步中保持可聚焦：禁用会让方向键把焦点甩到顶部 Tab 栏并误切页
                AppIconButton(onClick = { viewModel.sync() }) {
                    if (syncing != null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "同步",
                            tint = AppColors.listHeadline
                        )
                    }
                }
            }

            AppIconButton(onClick = { viewModel.refresh() }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "刷新",
                    tint = AppColors.listHeadline
                )
            }
        }

        // Content
        Box(
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = Spacing.xxxl, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                itemsIndexed(files) { index, file ->
                    FileItem(
                        file = file,
                        playCount = playCounts[file.path] ?: 0,
                        modifier = if (index == focusIndex) Modifier.focusRequester(itemFocusRequester) else Modifier,
                        menuExpanded = contextMenuFile?.path == file.path,
                        onClick = {
                            if (file.isDirectory) {
                                viewModel.loadFiles(file.path)
                            } else {
                                resumeFocusPath.value = file.path
                                val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                    putExtra("videoPath", file.path)
                                }
                                playerLauncher.launch(intent)
                            }
                        },
                        onLongClick = {
                            contextMenuFile = file
                        },
                        onFocused = {
                            focusedFile = file
                        },
                        onMenuDismiss = {
                            contextMenuFile = null
                        },
                        onBindToHome = {
                            contextMenuFile = null
                            viewModel.addToHomeGrid(file.path)
                        },
                        onDeleteFile = {
                            contextMenuFile = null
                            viewModel.deleteFile(file.path)
                        }
                    )
                }
            }

            // 同步结果短暂提示（进行中的状态显示在顶部同步按钮旁）
            when (val state = syncState) {
                is SyncService.SyncState.Completed -> {
                    if (showSyncResult) {
                        LaunchedEffect(state) {
                            kotlinx.coroutines.delay(3000)
                            showSyncResult = false
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(Spacing.md)
                                .background(AppColors.surfaceVariant, AppShapes.medium)
                        ) {
                            Text(
                                buildString {
                                    append("同步完成: ${state.fileCount} 个文件")
                                    if (state.removedCount > 0) append("，清理 ${state.removedCount} 项")
                                    if (state.failedCount > 0) append("，${state.failedCount} 项失败")
                                },
                                modifier = Modifier.padding(Spacing.md),
                                color = if (state.failedCount > 0) AppColors.error else AppColors.primary
                            )
                        }
                    }
                }
                is SyncService.SyncState.Error -> {
                    if (showSyncResult) {
                        LaunchedEffect(state) {
                            kotlinx.coroutines.delay(5000)
                            showSyncResult = false
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(Spacing.md)
                                .background(AppColors.surfaceVariant, AppShapes.medium)
                        ) {
                            Text(
                                "同步失败: ${state.message}",
                                modifier = Modifier.padding(Spacing.md),
                                color = AppColors.error
                            )
                        }
                    }
                }
                else -> {}
            }

            addToHomeResult?.let { msg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(Spacing.md),
                    containerColor = AppColors.surfaceVariant,
                    contentColor = AppColors.listHeadline,
                ) {
                    Text(msg)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileItem(
    file: FileInfo,
    playCount: Int,
    modifier: Modifier = Modifier,
    menuExpanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
    onMenuDismiss: () -> Unit,
    onBindToHome: () -> Unit,
    onDeleteFile: () -> Unit
) {
    val menuFocusRequester = remember { FocusRequester() }

    LaunchedEffect(menuExpanded) {
        if (menuExpanded) {
            withFrameNanos { }
            runCatching { menuFocusRequester.requestFocus() }
        }
    }

    AppListRow(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocused() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Icon(
                imageVector = when {
                    file.isDirectory -> Icons.Default.Folder
                    file.isAudio -> Icons.Default.MusicNote
                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = when {
                    file.isDirectory -> AppColors.primary
                    file.isAudio -> AppColors.primary
                    else -> AppColors.listCaption
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.listHeadline
                )
                if (!file.isDirectory) {
                    Text(
                        text = formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.listCaption
                    )
                }
            }
            if (!file.isDirectory && playCount > 0) {
                Text(
                    text = "播放 $playCount 次",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.listCaption
                )
            }
        }

        if (menuExpanded) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = onMenuDismiss
            ) {
                if (file.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("绑定到首页") },
                        onClick = onBindToHome,
                        modifier = Modifier.focusRequester(menuFocusRequester),
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("删除文件") },
                        onClick = onDeleteFile,
                        modifier = Modifier.focusRequester(menuFocusRequester),
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
