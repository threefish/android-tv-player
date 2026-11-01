package com.tvplayer.app.ui.home

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tvplayer.app.ui.component.AppCard
import com.tvplayer.app.ui.player.VideoPlayerActivity
import com.tvplayer.app.ui.theme.AppColors
import com.tvplayer.app.ui.theme.AppShapes
import com.tvplayer.app.ui.theme.Spacing
import com.tvplayer.core.model.HomeGridItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomePage(
    onOpenInBrowser: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val gridItems by viewModel.gridItems.collectAsState()
    var contextMenuItem by remember { mutableStateOf<HomeGridItem?>(null) }
    var focusedItem by remember { mutableStateOf<HomeGridItem?>(null) }
    var deleteTarget by remember { mutableStateOf<HomeGridItem?>(null) }
    val context = LocalContext.current

    val openFolder: (HomeGridItem) -> Unit = { item ->
        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
            putExtra("folderPath", item.folderPath)
        }
        context.startActivity(intent)
    }

    if (gridItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.xxxl, vertical = Spacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = AppColors.listOverline
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = "暂无收藏文件夹",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.listHeadline
                )
                Text(
                    text = "在文件浏览中按菜单键绑定文件夹到首页",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.listCaption
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { event ->
                    // 遥控器无法触发触摸长按，用菜单键打开当前聚焦项的上下文菜单
                    if (event.key == Key.Menu && event.type == KeyEventType.KeyDown && contextMenuItem == null) {
                        focusedItem?.let {
                            contextMenuItem = it
                            true
                        } ?: false
                    } else {
                        false
                    }
                },
            contentPadding = PaddingValues(horizontal = Spacing.xxxl, vertical = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            items(gridItems) { item ->
                FolderCard(
                    item = item,
                    menuExpanded = contextMenuItem?.id == item.id,
                    onClick = { contextMenuItem = item },
                    onLongClick = {
                        contextMenuItem = item
                    },
                    onFocused = {
                        focusedItem = item
                    },
                    onMenuDismiss = {
                        contextMenuItem = null
                    },
                    onPlay = {
                        contextMenuItem = null
                        openFolder(item)
                    },
                    onOpenInBrowser = {
                        contextMenuItem = null
                        onOpenInBrowser(item.folderPath)
                    },
                    onUnbind = {
                        contextMenuItem = null
                        deleteTarget = item
                    }
                )
            }
        }
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("解除绑定") },
            text = { Text("确定要将「${item.displayName}」从首页移除吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeItem(item.id)
                    deleteTarget = null
                }) {
                    Text("解除绑定")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderCard(
    item: HomeGridItem,
    menuExpanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
    onMenuDismiss: () -> Unit,
    onPlay: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onUnbind: () -> Unit
) {
    val menuFocusRequester = remember { FocusRequester() }

    LaunchedEffect(menuExpanded) {
        if (menuExpanded) {
            withFrameNanos { }
            runCatching { menuFocusRequester.requestFocus() }
        }
    }

    AppCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .onFocusChanged { if (it.isFocused) onFocused() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = AppColors.primary
            )
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.listHeadline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.folderPath,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.listCaption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (menuExpanded) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = onMenuDismiss
            ) {
                DropdownMenuItem(
                    text = { Text("继续播放") },
                    onClick = onPlay,
                    modifier = Modifier.focusRequester(menuFocusRequester),
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("进入目录") },
                    onClick = onOpenInBrowser,
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("解除") },
                    onClick = onUnbind,
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                )
            }
        }
    }
}
