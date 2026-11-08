package com.tvplayer.app.ui.images

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvplayer.app.image.WebDavImageKey
import com.tvplayer.app.ui.component.AppButton
import com.tvplayer.app.ui.component.AppCard
import com.tvplayer.app.ui.component.AppIconButton
import com.tvplayer.app.ui.theme.AppColors
import com.tvplayer.app.ui.theme.AppShapes
import com.tvplayer.app.ui.theme.Spacing
import com.tvplayer.core.model.FileInfo
import com.tvplayer.core.util.ImageSortMode
import kotlinx.coroutines.launch

@Composable
fun ImagesPage(
    onBackToTabs: () -> Unit,
    isActive: Boolean = true,
    refreshTick: Int = 0,
    viewModel: ImagesViewModel = hiltViewModel(),
) {
    val images by viewModel.images.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val sourceId by viewModel.sourceId.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var sortMenuExpanded by remember { mutableStateOf(false) }
    // 预览期间固定图片列表，避免后台同步刷新导致左右切换跳到别的图
    var previewTarget by remember { mutableStateOf<Pair<List<FileInfo>, Int>?>(null) }
    var focusIndex by remember { mutableIntStateOf(-1) }

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val sortButtonFocusRequester = remember { FocusRequester() }
    val sortMenuItemFocusRequester = remember { FocusRequester() }
    val itemFocusRequester = remember { FocusRequester() }
    var pageHasFocus by remember { mutableStateOf(false) }

    // 宫格内按返回先回到顶部当前 Tab，焦点已在工具栏时交给外层弹退出确认。隐藏时不能劫持返回键
    BackHandler(enabled = isActive && pageHasFocus) {
        onBackToTabs()
    }

    // 每次切回图片 Tab 重新查一次本地库，新增同步的图片才能进入列表
    LaunchedEffect(refreshTick) {
        if (refreshTick > 0) viewModel.load()
    }

    LaunchedEffect(sortMenuExpanded) {
        if (sortMenuExpanded) {
            withFrameNanos { }
            runCatching { sortMenuItemFocusRequester.requestFocus() }
        }
    }

    val onSortSelected: (ImageSortMode) -> Unit = { mode ->
        viewModel.setSortMode(mode)
        sortMenuExpanded = false
        scope.launch {
            withFrameNanos { }
            runCatching { sortButtonFocusRequester.requestFocus() }
        }
    }

    val onPreviewClosed: (Int) -> Unit = { lastIndex ->
        previewTarget = null
        focusIndex = lastIndex
        scope.launch {
            // 左右切到远处的图片后，宫格需要先滚到目标项附近才能聚焦
            val visible = gridState.layoutInfo.visibleItemsInfo.any { it.index == lastIndex }
            if (!visible) {
                gridState.scrollToItem((lastIndex - 4).coerceAtLeast(0))
            }
            withFrameNanos { }
            withFrameNanos { }
            runCatching { itemFocusRequester.requestFocus() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onFocusChanged { pageHasFocus = it.hasFocus },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xxxl, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "图片",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.listHeadline,
                )
                Text(
                    text = "${images.size} 张",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.listCaption,
                )
            }

            Box {
                AppButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier.focusRequester(sortButtonFocusRequester),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(sortMode.label)
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                ) {
                    ImageSortMode.values().forEachIndexed { index, mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            onClick = { onSortSelected(mode) },
                            modifier = if (index == 0) {
                                Modifier.focusRequester(sortMenuItemFocusRequester)
                            } else {
                                Modifier
                            },
                            leadingIcon = {
                                if (mode == sortMode) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                } else {
                                    Spacer(modifier = Modifier.size(24.dp))
                                }
                            },
                        )
                    }
                }
            }

            // 同步中保持可聚焦：禁用会让方向键把焦点甩到顶部 Tab 栏并误切页
            AppIconButton(onClick = { viewModel.refresh() }) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.primary,
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "同步",
                        tint = AppColors.listHeadline,
                    )
                }
            }
        }

        if (images.isEmpty()) {
            EmptyImages(isSyncing = isSyncing)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                contentPadding = PaddingValues(horizontal = Spacing.xxxl, vertical = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                itemsIndexed(
                    items = images,
                    key = { _, file -> file.path },
                ) { index, file ->
                    ImageTile(
                        file = file,
                        sourceId = sourceId,
                        modifier = Modifier
                            .then(
                                if (index == focusIndex) {
                                    Modifier.focusRequester(itemFocusRequester)
                                } else {
                                    Modifier
                                }
                            )
                            // 首行按上键直接到排序按钮，避免误入顶部导航切换 tab
                            .then(
                                if (index < 4) {
                                    Modifier.focusProperties { up = sortButtonFocusRequester }
                                } else {
                                    Modifier
                                }
                            ),
                        onClick = { previewTarget = images to index },
                    )
                }
            }
        }
    }

    previewTarget?.let { (previewImages, startIndex) ->
        ImagePreviewDialog(
            images = previewImages,
            startIndex = startIndex,
            sourceId = sourceId,
            onClose = onPreviewClosed,
        )
    }
}

@Composable
private fun ImageTile(
    file: FileInfo,
    sourceId: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    val shape = AppShapes.medium

    val request = remember(file.path, file.modifiedTime, sourceId) {
        ImageRequest.Builder(context)
            .data(WebDavImageKey(sourceId, file.path, file.modifiedTime))
            .size(512, 384)
            .allowRgb565(true)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .crossfade(false)
            .build()
    }

    AppCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .onFocusChanged { focused = it.isFocused },
        shape = shape,
    ) {
        AsyncImage(
            model = request,
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.surfaceVariant),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)),
                    )
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = 6.dp),
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = parentPath(file.path),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.listCaption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (focused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(3.dp, AppColors.primary, shape),
            )
        }
    }
}

@Composable
private fun EmptyImages(isSyncing: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xxxl, vertical = Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp,
                    color = AppColors.primary,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = AppColors.listOverline,
                )
            }
            Spacer(modifier = Modifier.size(Spacing.md))
            Text(
                text = if (isSyncing) "正在同步 WebDAV..." else "暂无图片",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.listHeadline,
            )
            Text(
                text = "仅显示 jpg / jpeg / png / webp / bmp / gif",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.listCaption,
            )
        }
    }
}

private fun parentPath(path: String): String =
    path.substringBeforeLast('/', "/").ifEmpty { "/" }
