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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.tvplayer.app.ui.component.AppButton
import com.tvplayer.app.ui.component.AppButtonDefaults
import com.tvplayer.app.ui.component.AppListRow
import com.tvplayer.app.ui.component.DrawerSectionTitle
import com.tvplayer.app.ui.component.FloatingIconButton
import com.tvplayer.app.ui.component.SideDrawer
import com.tvplayer.app.ui.component.floatingCorner
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
    var showPageDrawer by remember { mutableStateOf(false) }
    // 打开抽屉时快照当前选中项，抽屉弹出后焦点变化不影响已展示的操作
    var drawerFile by remember { mutableStateOf<FileInfo?>(null) }
    val drawerFirstFocus = remember { FocusRequester() }
    val openPageDrawer: () -> Unit = {
        drawerFile = focusedFile
        showPageDrawer = true
    }
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

    // 结果条短暂展示后自动收起
    LaunchedEffect(syncState, showSyncResult) {
        if (!showSyncResult) return@LaunchedEffect
        when (syncState) {
            is SyncService.SyncState.Completed -> {
                kotlinx.coroutines.delay(3000)
                showSyncResult = false
            }
            is SyncService.SyncState.Error -> {
                kotlinx.coroutines.delay(5000)
                showSyncResult = false
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
                // 菜单键打开页面抽屉，目录操作与当前选中项操作都在这里
                if (event.key == Key.Menu && event.type == KeyEventType.KeyDown) {
                    openPageDrawer()
                    true
                } else {
                    false
                }
            }
    ) {
        // Content
        Box(
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    start = 56.dp,
                    end = 56.dp,
                    top = 12.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        onBlur = {
                            if (focusedFile?.path == file.path) focusedFile = null
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

            FloatingIconButton(
                onClick = openPageDrawer,
                contentDescription = "页面菜单",
                loading = syncing != null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .floatingCorner(),
            )
        }

        // 同步状态条参与布局，不遮挡文件列表
        val syncBar = when (val state = syncState) {
            is SyncService.SyncState.Syncing -> SyncBarState(
                text = buildString {
                    append("同步中")
                    val folder = state.path.trimEnd('/').substringAfterLast('/')
                    if (folder.isNotEmpty() && folder != "准备同步") append(" · $folder")
                    if (state.total > 0) append("  ${state.current}/${state.total}")
                },
                isError = false,
                inProgress = true,
            )
            is SyncService.SyncState.Completed -> if (showSyncResult) SyncBarState(
                text = buildString {
                    append("同步完成: ${state.fileCount} 个文件")
                    if (state.removedCount > 0) append("，清理 ${state.removedCount} 项")
                    if (state.failedCount > 0) append("，${state.failedCount} 项失败")
                },
                isError = state.failedCount > 0,
                inProgress = false,
            ) else null
            is SyncService.SyncState.Error -> if (showSyncResult) SyncBarState(
                text = "同步失败: ${state.message}",
                isError = true,
                inProgress = false,
            ) else null
            else -> null
        }
        syncBar?.let { bar ->
            SyncStatusBar(
                text = bar.text,
                isError = bar.isError,
                inProgress = bar.inProgress,
                modifier = Modifier.padding(start = 56.dp, end = 56.dp, bottom = 12.dp),
            )
        }
    }

    if (showPageDrawer) {
        SideDrawer(
            title = "视频",
            subtitle = currentPath,
            initialFocus = drawerFirstFocus,
            onDismiss = { showPageDrawer = false },
        ) {
            DrawerSectionTitle("目录操作")
            AppButton(
                onClick = {
                    showPageDrawer = false
                    viewModel.sync()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(drawerFirstFocus),
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(if (syncing != null) "同步中…" else "同步")
            }
            Spacer(Modifier.height(10.dp))
            AppButton(
                onClick = {
                    showPageDrawer = false
                    viewModel.refresh()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("刷新")
            }
            if (currentPath != "/") {
                Spacer(Modifier.height(10.dp))
                AppButton(
                    onClick = {
                        showPageDrawer = false
                        viewModel.navigateToParent()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text("返回上级")
                }
            }

            drawerFile?.let { file ->
                Spacer(Modifier.height(20.dp))
                DrawerSectionTitle("当前选中")
                Text(
                    text = file.name,
                    fontSize = 15.sp,
                    color = AppColors.listHeadline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                if (file.isDirectory) {
                    AppButton(
                        onClick = {
                            showPageDrawer = false
                            viewModel.addToHomeGrid(file.path)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text("绑定到首页")
                    }
                } else {
                    AppButton(
                        onClick = {
                            showPageDrawer = false
                            viewModel.deleteFile(file.path)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = AppButtonDefaults.dangerColors(),
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text("删除文件")
                    }
                }
            }
        }
    }
}

private data class SyncBarState(
    val text: String,
    val isError: Boolean,
    val inProgress: Boolean,
)

@Composable
private fun SyncStatusBar(
    text: String,
    isError: Boolean,
    inProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.surfaceVariant, AppShapes.medium)
            .padding(horizontal = Spacing.md, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            inProgress -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = AppColors.primary,
            )
            isError -> Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = AppColors.error,
                modifier = Modifier.size(18.dp),
            )
            else -> Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AppColors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = if (isError) AppColors.error else AppColors.listHeadline,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    onBlur: () -> Unit,
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
            .onFocusChanged { if (it.isFocused) onFocused() else onBlur() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = when {
                    file.isDirectory -> Icons.Default.Folder
                    file.isAudio -> Icons.Default.MusicNote
                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                },
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = when {
                    file.isDirectory -> AppColors.primary
                    file.isAudio -> AppColors.primary
                    else -> AppColors.listCaption
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.listHeadline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!file.isDirectory) {
                    Text(
                        text = formatFileSize(file.size),
                        fontSize = 14.sp,
                        color = AppColors.listCaption,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (!file.isDirectory && playCount > 0) {
                Text(
                    text = "播放 $playCount 次",
                    fontSize = 14.sp,
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
