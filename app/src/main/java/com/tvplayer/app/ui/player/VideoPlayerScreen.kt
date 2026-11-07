package com.tvplayer.app.ui.player

import android.graphics.Color as AndroidColor
import android.util.TypedValue
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.tvplayer.app.ui.theme.AppColors
import com.tvplayer.app.ui.theme.Spacing
import com.tvplayer.player.PlayerEngine
import com.tvplayer.player.PlayerService
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val subtitles by viewModel.subtitles.collectAsState()
    val savedPosition by viewModel.savedPosition.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val currentTitle by viewModel.currentTitle.collectAsState()
    val bufferSpeed by viewModel.bufferSpeed.collectAsState()
    val isSingleLoop by viewModel.isSingleLoop.collectAsState()
    val previousName by viewModel.previousName.collectAsState()
    val nextName by viewModel.nextName.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val resizeMode by viewModel.resizeMode.collectAsState()
    val subtitleSize by viewModel.subtitleSize.collectAsState()
    val isReady by viewModel.isReady.collectAsState()
    val transition by viewModel.transition.collectAsState()
    val engine by viewModel.engine.collectAsState()
    val selectedSubtitleName by viewModel.selectedSubtitleName.collectAsState()
    val isAudio by viewModel.isAudio.collectAsState()
    val finished by viewModel.finished.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val restReminder by viewModel.restReminder.collectAsState()
    val playlistEntries by viewModel.playlistEntries.collectAsState()
    val currentPlaylistIndex by viewModel.currentPlaylistIndex.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(true) }
    var seekIndicator by remember { mutableStateOf<String?>(null) }
    var seekIndicatorTick by remember { mutableIntStateOf(0) }
    var exitArmed by remember { mutableStateOf(false) }
    var showExitHint by remember { mutableStateOf(false) }
    var volume by remember { mutableStateOf(1.0f) }
    var rootFocused by remember { mutableStateOf(false) }
    var menuKeyHeld by remember { mutableStateOf(false) }
    var centerKeyHeld by remember { mutableStateOf(false) }
    var backKeyHeld by remember { mutableStateOf(false) }
    var seekRepeat by remember { mutableIntStateOf(0) }
    var lastTransition by remember { mutableStateOf<PlaybackTransition?>(null) }
    var focusControlsTick by remember { mutableIntStateOf(0) }
    var focusPauseButtonOnPause by remember { mutableStateOf(false) }
    var focusedSubtitleRow by remember { mutableIntStateOf(-1) }
    var finishedBackFocused by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var restMessage by remember { mutableStateOf<String?>(null) }
    var showPlaylist by remember { mutableStateOf(false) }
    var focusedPlaylistRow by remember { mutableIntStateOf(-1) }
    val playlistListState = rememberLazyListState()
    val playlistItemFocusRequester = remember { FocusRequester() }
    val drawerItems = remember(playlistEntries) { buildPlaylistDrawerItems(playlistEntries) }

    val resumeDialogVisible = showResumeDialog && savedPosition > 10_000
    var resumeContinueFocused by remember { mutableStateOf(false) }
    // 只有用户把焦点移到“从头播放”才停止抢占，短暂获焦后被抢走不算
    var resumeStartOverFocused by remember { mutableStateOf(false) }
    val rootFocusRequester = remember { FocusRequester() }
    val menuFirstFocusRequester = remember { FocusRequester() }
    val menuLastFocusRequester = remember { FocusRequester() }
    // 菜单首项按上循环聚焦到最后一项「退出播放」
    val onMenuTopUp: (KeyEvent) -> Boolean = { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
            runCatching { menuLastFocusRequester.requestFocus() }
            true
        } else {
            false
        }
    }
    // 菜单末项按下循环聚焦到第一项
    val onMenuBottomDown: (KeyEvent) -> Boolean = { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
            runCatching { menuFirstFocusRequester.requestFocus() }
            true
        } else {
            false
        }
    }
    val playPauseFocusRequester = remember { FocusRequester() }
    val subtitleDialogFocusRequester = remember { FocusRequester() }
    val resumeFocusRequester = remember { FocusRequester() }
    val resumeStartOverFocusRequester = remember { FocusRequester() }
    val finishedBackFocusRequester = remember { FocusRequester() }
    val errorBackFocusRequester = remember { FocusRequester() }

    // 目录没有可播放条目时展示错误页，焦点落到返回按钮
    LaunchedEffect(loadError) {
        if (loadError == null) return@LaunchedEffect
        showControls = false
        delay(400)
        repeat(20) {
            withFrameNanos { }
            if (runCatching { errorBackFocusRequester.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(80)
        }
    }

    // 目录播完后收起操作栏，并把焦点交给停止页的返回按钮。
    // 单次请求可能落在节点挂载之前，重试若干帧直到按钮真正获焦
    LaunchedEffect(finished) {
        if (!finished) {
            finishedBackFocused = false
            return@LaunchedEffect
        }
        showControls = false
        finishedBackFocused = false
        // 操作栏退场动画期间焦点会被并回根节点，等它退完再抢，否则会立刻被抢走
        delay(400)
        repeat(20) {
            if (finishedBackFocused) return@LaunchedEffect
            withFrameNanos { }
            runCatching { finishedBackFocusRequester.requestFocus() }
            delay(80)
        }
    }

    // 弹窗关闭后把焦点还给画面，保证遥控器继续操作播放
    LaunchedEffect(resumeDialogVisible) {
        if (!resumeDialogVisible) {
            resumeContinueFocused = false
            resumeStartOverFocused = false
            withFrameNanos { }
            runCatching { rootFocusRequester.requestFocus() }
            return@LaunchedEffect
        }
        repeat(20) {
            if (resumeContinueFocused) return@LaunchedEffect
            if (resumeStartOverFocused) return@LaunchedEffect
            withFrameNanos { }
            runCatching { resumeFocusRequester.requestFocus() }
            delay(80)
        }
    }

    LaunchedEffect(isReady) {
        if (isReady) viewModel.startPlayback()
    }

    LaunchedEffect(showMenu) {
        withFrameNanos { }
        if (showMenu) {
            runCatching { menuFirstFocusRequester.requestFocus() }
        } else if (!resumeDialogVisible && !showSubtitleDialog && !showPlaylist) {
            runCatching { rootFocusRequester.requestFocus() }
        }
    }

    // 焦点到达首条目时滚到列表绝对顶部，否则首个组头会被最小滚动留在视口外
    LaunchedEffect(focusedPlaylistRow) {
        if (showPlaylist && focusedPlaylistRow == 0) {
            playlistListState.scrollToItem(0)
        }
    }

    // 抽屉打开时滚动到当前集附近并聚焦当前行；关闭后焦点归还画面
    LaunchedEffect(showPlaylist) {
        if (showPlaylist) {
            focusedPlaylistRow = currentPlaylistIndex
            val adapterIndex = drawerItems.indexOfFirst {
                it is PlaylistDrawerItem.Entry && it.entry.index == currentPlaylistIndex
            }.coerceAtLeast(0)
            playlistListState.scrollToItem((adapterIndex - 4).coerceAtLeast(0))
            repeat(20) {
                withFrameNanos { }
                if (runCatching { playlistItemFocusRequester.requestFocus() }.isSuccess) return@LaunchedEffect
                delay(80)
            }
        } else if (!resumeDialogVisible && !showMenu && !showSubtitleDialog) {
            withFrameNanos { }
            runCatching { rootFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(showControls, finished, loadError) {
        // 停止页/错误页展示期间焦点归页面按钮，不要被根节点抢走
        if (!showControls && !finished && loadError == null) {
            withFrameNanos { }
            runCatching { rootFocusRequester.requestFocus() }
        }
    }

    // 全屏画面本身是焦点目标，方向键无法自动进入底部操作栏，按下键时显式移入焦点
    LaunchedEffect(focusControlsTick) {
        if (focusControlsTick > 0 && showControls && !showMenu && !showSubtitleDialog && !showPlaylist) {
            withFrameNanos { }
            runCatching { playPauseFocusRequester.requestFocus() }
        }
    }

    // OK 键暂停后把焦点落到播放/暂停按钮；必须等状态落定，避免按键抬起被按钮当成点击
    LaunchedEffect(isPlaying, focusPauseButtonOnPause) {
        if (focusPauseButtonOnPause && !isPlaying && showControls && !showMenu && !showSubtitleDialog && !showPlaylist) {
            withFrameNanos { }
            runCatching { playPauseFocusRequester.requestFocus() }
            focusPauseButtonOnPause = false
        }
    }

    // 无遥控操作 5 秒后隐藏工具栏（播放与暂停都计时）；弹窗/抽屉/停止页/错误页/休息屏打开时不隐藏
    LaunchedEffect(showControls, interactionTick, showMenu, showSubtitleDialog, showPlaylist, resumeDialogVisible, finished, loadError, restMessage) {
        if (showControls && !showMenu && !showSubtitleDialog && !showPlaylist && !resumeDialogVisible &&
            !finished && loadError == null && restMessage == null
        ) {
            delay(5000)
            showControls = false
        }
    }

    // 播放提醒触发：自动暂停已由 ViewModel 完成，这里只展示休息屏
    LaunchedEffect(restReminder) {
        if (restReminder) {
            restMessage = "休息一下"
            showControls = false
            showPlaylist = false
            showMenu = false
            viewModel.acknowledgeRestReminder()
        }
    }

    // 暂停 30 秒后进入休息屏；恢复播放或打开弹窗都会取消计时
    LaunchedEffect(isPlaying, playbackState, isReady, finished, resumeDialogVisible, loadError, restMessage, showMenu, showSubtitleDialog, showPlaylist) {
        if (isReady && !isPlaying && playbackState is PlayerService.PlaybackState.Ready &&
            !finished && !resumeDialogVisible && loadError == null && restMessage == null &&
            !showMenu && !showSubtitleDialog && !showPlaylist
        ) {
            delay(30_000)
            restMessage = "休息中"
            showControls = false
        }
    }

    LaunchedEffect(showSubtitleDialog) {
        if (showSubtitleDialog) {
            focusedSubtitleRow = -1
            withFrameNanos { }
            runCatching { subtitleDialogFocusRequester.requestFocus() }
        } else if (!resumeDialogVisible && !showMenu && !showPlaylist) {
            withFrameNanos { }
            runCatching { rootFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(seekIndicatorTick) {
        if (seekIndicatorTick > 0) {
            delay(1500)
            seekIndicator = null
        }
    }

    LaunchedEffect(transition) {
        if (transition != null) {
            lastTransition = transition
            delay(2600)
            viewModel.clearTransition()
        }
    }

    LaunchedEffect(exitArmed) {
        if (exitArmed) {
            delay(3000)
            exitArmed = false
            showExitHint = false
        }
    }

    fun seekBy(ms: Long) {
        if (ms > 0) viewModel.seekForward(ms) else viewModel.seekBackward(-ms)
        seekIndicator = if (ms > 0) ">> ${ms / 1000}s" else "<< ${-ms / 1000}s"
        seekIndicatorTick++
    }

    fun requestExit() {
        if (exitArmed) {
            onBack()
            return
        }
        exitArmed = true
        showExitHint = true
        showControls = true
    }

    BackHandler {
        when {
            // 错误页与停止页上返回键一次退出，不再要求二次确认
            loadError != null -> onBack()
            finished -> onBack()
            showSubtitleDialog -> showSubtitleDialog = false
            resumeDialogVisible -> showResumeDialog = false
            showPlaylist -> showPlaylist = false
            showMenu -> showMenu = false
            else -> requestExit()
        }
    }

    val exoPlayer = remember { viewModel.getPlayer() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .onFocusChanged { rootFocused = it.isFocused }
            // 预览阶段先拿到所有按键（含焦点在控件按钮上时），用于刷新无操作计时
            .onPreviewKeyEvent {
                interactionTick++
                false
            }
            .onKeyEvent { event ->
                val isDown = event.type == KeyEventType.KeyDown
                val isUp = event.type == KeyEventType.KeyUp
                when {
                    restMessage != null -> {
                        if (isDown) {
                            when (event.key) {
                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                    restMessage = null
                                    viewModel.resume()
                                    showControls = true
                                }
                                Key.Back -> onBack()
                            }
                        }
                        true
                    }
                    loadError != null -> {
                        if (isDown && event.key == Key.Back) onBack()
                        true
                    }
                    event.key == Key.Menu -> {
                        if (isDown && !menuKeyHeld) {
                            if (showPlaylist) {
                                showPlaylist = false
                            } else if (!finished && !resumeDialogVisible && !showSubtitleDialog) {
                                showMenu = !showMenu
                                if (showMenu) showControls = true
                            }
                        }
                        if (isDown) menuKeyHeld = true else if (isUp) menuKeyHeld = false
                        true
                    }
                    event.key == Key.Back -> {
                        if (finished) {
                            // 停止页显示时交给 BackHandler 一次退出
                            false
                        } else {
                            if (isDown && !backKeyHeld) {
                                when {
                                    showSubtitleDialog -> showSubtitleDialog = false
                                    resumeDialogVisible -> showResumeDialog = false
                                    showPlaylist -> showPlaylist = false
                                    showMenu -> showMenu = false
                                    // 焦点在操作栏内时先收起操作栏，回到画面
                                    !rootFocused -> showControls = false
                                    else -> requestExit()
                                }
                            }
                            if (isDown) backKeyHeld = true else if (isUp) backKeyHeld = false
                            true
                        }
                    }
                    !rootFocused -> false
                    event.key == Key.DirectionDown -> {
                        // 停止页展示时不再唤出操作栏，按键留给停止页按钮
                        if (isDown && !finished && !showMenu && !resumeDialogVisible && !showSubtitleDialog && !showPlaylist) {
                            showControls = true
                            focusControlsTick++
                        }
                        true
                    }
                    event.key == Key.DirectionLeft || event.key == Key.DirectionRight -> {
                        if (isDown && !finished && !showMenu && !resumeDialogVisible && !showSubtitleDialog && !showPlaylist) {
                            val step = if (seekRepeat > 2) 60_000L else 10_000L
                            seekRepeat++
                            seekBy(if (event.key == Key.DirectionRight) step else -step)
                        }
                        if (isUp) seekRepeat = 0
                        true
                    }
                    event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                        if (isDown && !centerKeyHeld && !finished &&
                            !showMenu && !resumeDialogVisible && !showSubtitleDialog && !showPlaylist
                        ) {
                            if (isPlaying) {
                                viewModel.pause()
                                focusPauseButtonOnPause = true
                            } else {
                                viewModel.resume()
                            }
                            showControls = true
                        }
                        if (isDown) centerKeyHeld = true else if (isUp) centerKeyHeld = false
                        true
                    }
                    else -> false
                }
            }
            .focusable()
    ) {
        if (isAudio) {
            AudioBackdrop(title = currentTitle)
        } else {
            key(engine) {
                if (engine == PlayerEngine.VLC) {
                    AndroidView(
                        factory = { ctx ->
                            org.videolan.libvlc.util.VLCVideoLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(AndroidColor.BLACK)
                                isFocusable = false
                                isFocusableInTouchMode = false
                                isClickable = false
                                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                                viewModel.attachVlcView(this)
                            }
                        },
                        onRelease = { viewModel.detachVlcView() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                useController = false
                                isFocusable = false
                                isFocusableInTouchMode = false
                                isClickable = false
                                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                            }
                        },
                        update = { playerView ->
                            playerView.resizeMode = when (resizeMode) {
                                1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                            playerView.subtitleView?.let { subtitleView ->
                                val baseSizeSp = 20f
                                subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, baseSizeSp * subtitleSize)
                                subtitleView.setStyle(
                                    CaptionStyleCompat(
                                        AndroidColor.WHITE,
                                        AndroidColor.argb(128, 0, 0, 0),
                                        AndroidColor.TRANSPARENT,
                                        CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                        AndroidColor.BLACK,
                                        null
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showControls && !showMenu,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top gradient overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xCC000000), Color.Transparent),
                                startY = 0f,
                                endY = 120f
                            )
                        )
                )

                // Bottom gradient overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC000000)),
                                startY = 0f,
                                endY = 180f
                            )
                        )
                )

                // Top bar: back + title + secondary icons
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                                runCatching { rootFocusRequester.requestFocus() }
                                true
                            } else {
                                false
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlayerIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            iconSize = 24.dp,
                            onClick = { requestExit() }
                        )
                        Text(
                            text = currentTitle,
                            color = Color.White,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Bottom controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    // Progress bar with times
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                                    runCatching { rootFocusRequester.requestFocus() }
                                    true
                                } else {
                                    false
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )

                        PlayerSlider(
                            value = if (duration > 0) currentPosition.toFloat() else 0f,
                            onValueChange = { viewModel.seekTo(it.toLong()) },
                            valueRange = 0f..if (duration > 0) duration.toFloat() else 1f,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = formatTime(duration),
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Media controls row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: media buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PlayerIconButton(
                                icon = Icons.Default.SkipPrevious,
                                contentDescription = "上一个",
                                iconSize = 28.dp,
                                onClick = { viewModel.playPrevious() }
                            )
                            PlayerIconButton(
                                icon = Icons.Default.FastRewind,
                                contentDescription = "快退",
                                iconSize = 28.dp,
                                onClick = { viewModel.seekBackward(10000) }
                            )

                            PlayerIconButton(
                                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "暂停" else "播放",
                                iconSize = 32.dp,
                                size = 44.dp,
                                focusRequester = playPauseFocusRequester,
                                onClick = {
                                    if (isPlaying) viewModel.pause() else viewModel.resume()
                                }
                            )

                            PlayerIconButton(
                                icon = Icons.Default.FastForward,
                                contentDescription = "快进",
                                iconSize = 28.dp,
                                onClick = { viewModel.seekForward(10000) }
                            )
                            PlayerIconButton(
                                icon = Icons.Default.SkipNext,
                                contentDescription = "下一个",
                                iconSize = 28.dp,
                                onClick = { viewModel.playNext() }
                            )
                        }

                        // Right: volume + settings + fullscreen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PlayerIconButton(
                                    icon = if (volume > 0f) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "音量",
                                    iconSize = 22.dp,
                                    onClick = {
                                        volume = if (volume > 0f) 0f else 1f
                                        viewModel.setVolume(volume)
                                    }
                                )
                                PlayerSlider(
                                    value = volume,
                                    onValueChange = {
                                        volume = it
                                        viewModel.setVolume(it)
                                    },
                                    modifier = Modifier.width(80.dp)
                                )
                            }

                            if (!isAudio) {
                                PlayerIconButton(
                                    icon = Icons.Default.Subtitles,
                                    contentDescription = "字幕",
                                    iconSize = 22.dp,
                                    onClick = { showSubtitleDialog = true }
                                )
                            }
                            PlayerIconButton(
                                icon = Icons.Default.Settings,
                                contentDescription = "设置",
                                iconSize = 22.dp,
                                onClick = { showMenu = true }
                            )
                            if (!isAudio) {
                                PlayerIconButton(
                                    icon = Icons.Default.Fullscreen,
                                    contentDescription = "画面缩放",
                                    iconSize = 22.dp,
                                    onClick = { viewModel.cycleResizeMode() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Seek indicator
        seekIndicator?.let { text ->
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.playerScrim)
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }

        // 切换视频时的过场提示：放在底部，避免标题挡住画面中心
        AnimatedVisibility(
            visible = transition != null,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.96f),
            exit = fadeOut(tween(400)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp)
        ) {
            lastTransition?.let { item ->
                Card(
                    modifier = Modifier
                        .widthIn(min = 260.dp, max = 460.dp)
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.playerScrim),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (item.isAuto) "自动播放下一集" else "正在切换",
                            color = AppColors.primary,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = item.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "第 ${item.index} / ${item.total} 个${if (item.isAudio) "音频" else "视频"}",
                                color = Color(0xFFAAAAAA),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (playbackState is PlayerService.PlaybackState.Buffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Menu overlay
        if (showMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000))
            )
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(360.dp)
                    .heightIn(max = 460.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.playerScrim)
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .focusGroup()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "播放控制",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    HorizontalDivider(color = Color.Gray)
                    if (playlistEntries.size > 1) {
                        MenuItem(
                            icon = Icons.Default.VideoLibrary,
                            label = "播放列表",
                            trailingText = "${currentPlaylistIndex + 1}/${playlistEntries.size}",
                            onClick = { showMenu = false; showPlaylist = true },
                            focusRequester = menuFirstFocusRequester,
                            onKeyEvent = onMenuTopUp
                        )
                    }
                    MenuItem(
                        icon = Icons.Default.SkipPrevious,
                        label = "上一个",
                        trailingText = previousName,
                        onClick = { viewModel.playPrevious(); showMenu = false },
                        focusRequester = if (playlistEntries.size <= 1) menuFirstFocusRequester else null,
                        onKeyEvent = if (playlistEntries.size <= 1) onMenuTopUp else null
                    )
                    MenuItem(
                        icon = Icons.Default.Replay,
                        label = "重播",
                        onClick = { viewModel.replay(); showMenu = false }
                    )
                    MenuItem(
                        icon = Icons.Default.SkipNext,
                        label = "下一个",
                        trailingText = nextName,
                        onClick = { viewModel.playNext(); showMenu = false }
                    )
                    MenuItem(
                        icon = if (isSingleLoop) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        label = if (isSingleLoop) "取消循环" else "单个循环",
                        onClick = { viewModel.toggleRepeatMode(); showMenu = false }
                    )
                    HorizontalDivider(color = Color.Gray)
                    MenuItem(
                        icon = Icons.Default.Speed,
                        label = "倍速",
                        trailingText = "${playbackSpeed}x",
                        onClick = { viewModel.cyclePlaybackSpeed() }
                    )
                    if (!isAudio) {
                        MenuItem(
                            icon = Icons.Default.AspectRatio,
                            label = "画面缩放",
                            trailingText = when (resizeMode) {
                                1 -> "拉伸填充"
                                2 -> "放大裁剪"
                                else -> "适应屏幕"
                            },
                            onClick = { viewModel.cycleResizeMode() }
                        )
                        MenuItem(
                            icon = Icons.Default.FormatSize,
                            label = "字幕大小",
                            trailingText = "${(subtitleSize * 100).toInt()}%",
                            onClick = { viewModel.cycleSubtitleSize() }
                        )
                    }
                    if (!isAudio && subtitles.isNotEmpty()) {
                        HorizontalDivider(color = Color.Gray)
                        MenuItem(
                            icon = Icons.Default.Subtitles,
                            label = "字幕",
                            onClick = { showSubtitleDialog = true; showMenu = false }
                        )
                    }
                    HorizontalDivider(color = Color.Gray)
                    MenuItem(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = "退出播放",
                        onClick = { showMenu = false; onBack() },
                        focusRequester = menuLastFocusRequester,
                        onKeyEvent = onMenuBottomDown
                    )
                }
            }
        }

        // 播放列表抽屉：右侧滑出，左右键与边界上下键在抽屉内消费，防止焦点逃逸到底层控件
        if (showPlaylist) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(22f)
                    .background(Color(0x99000000))
                    .pointerInput(Unit) { detectTapGestures { } }
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (event.key) {
                            Key.DirectionLeft, Key.DirectionRight -> true
                            Key.DirectionUp -> focusedPlaylistRow <= 0
                            Key.DirectionDown -> focusedPlaylistRow >= playlistEntries.size - 1
                            else -> false
                        }
                    }
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(440.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.playerScrim),
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "播放列表",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${currentPlaylistIndex + 1} / ${playlistEntries.size}",
                            color = Color(0xFFAAAAAA),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(
                            state = playlistListState,
                            modifier = Modifier
                                .weight(1f)
                                .focusGroup()
                        ) {
                            itemsIndexed(drawerItems, key = { index, _ -> index }) { _, item ->
                                when (item) {
                                    is PlaylistDrawerItem.Header -> PlaylistGroupHeader(
                                        label = item.label,
                                        count = item.count
                                    )
                                    is PlaylistDrawerItem.Entry -> PlaylistRow(
                                        entry = item.entry,
                                        isCurrent = item.entry.index == currentPlaylistIndex,
                                        focusRequester = if (item.entry.index == currentPlaylistIndex) playlistItemFocusRequester else null,
                                        onFocused = { focusedPlaylistRow = item.entry.index },
                                        onClick = {
                                            viewModel.playAt(item.entry.index)
                                            showPlaylist = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showExitHint,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.playerScrim)) {
                Text(
                    text = "再按一次返回退出",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }

        // Loading indicator
        if (playbackState is PlayerService.PlaybackState.Buffering && transition == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = formatSpeed(bufferSpeed),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        (playbackState as? PlayerService.PlaybackState.Error)?.let { state ->
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(Spacing.xxxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "播放失败",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.message,
                    color = Color(0xFFFFB4AB),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // 休息屏：暂停 30 秒或播放提醒触发时全屏黑底，OK 继续播放
        if (restMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(25f)
                    .background(Color.Black)
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = restMessage ?: "",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "按 OK 键继续播放",
                        color = Color(0xFF999999),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // 目录全部播完后的停止页
        if (finished) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(18f)
                    .background(Color(0xCC000000))
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.width(560.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.playerScrim),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "播放结束",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "本目录的${if (isAudio) "音频" else "视频"}已全部播放完毕",
                            color = Color(0xFFCCCCCC),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.align(Alignment.End),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ResumeDialogButton(
                                text = "重播本集",
                                primary = false,
                                onClick = { viewModel.replayCurrent() }
                            )
                            ResumeDialogButton(
                                text = "返回列表",
                                primary = true,
                                focusRequester = finishedBackFocusRequester,
                                onFocusChange = { finishedBackFocused = it },
                                onClick = onBack
                            )
                        }
                    }
                }
            }
        }

        // 目录没有可播放条目时的错误页
        if (loadError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(30f)
                    .background(Color(0xCC000000))
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.width(560.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.playerScrim),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "无法播放",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = loadError ?: "",
                            color = Color(0xFFCCCCCC),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(modifier = Modifier.align(Alignment.End)) {
                            ResumeDialogButton(
                                text = "返回",
                                primary = true,
                                focusRequester = errorBackFocusRequester,
                                onClick = onBack
                            )
                        }
                    }
                }
            }
        }

        // 续播弹窗与播放页同窗口，避免独立弹窗延迟获焦
        if (resumeDialogVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .background(Color(0x99000000))
                    .pointerInput(Unit) { detectTapGestures { } }
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (event.key) {
                            Key.DirectionLeft -> {
                                runCatching { resumeFocusRequester.requestFocus() }
                                true
                            }
                            Key.DirectionRight -> {
                                runCatching { resumeStartOverFocusRequester.requestFocus() }
                                true
                            }
                            Key.DirectionUp, Key.DirectionDown -> true
                            Key.Back -> {
                                showResumeDialog = false
                                true
                            }
                            else -> false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.width(560.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.playerScrim),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "继续播放",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "上次播放到 ${formatTime(savedPosition)}，是否从上次位置继续？",
                            color = Color(0xFFCCCCCC),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.align(Alignment.End),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ResumeDialogButton(
                                text = "继续播放",
                                primary = true,
                                focusRequester = resumeFocusRequester,
                                onFocusChange = {
                                    resumeContinueFocused = it
                                },
                                onClick = {
                                    viewModel.startPlayback(savedPosition)
                                    showResumeDialog = false
                                }
                            )
                            ResumeDialogButton(
                                text = "从头播放",
                                primary = false,
                                focusRequester = resumeStartOverFocusRequester,
                                onFocusChange = { resumeStartOverFocused = it },
                                onClick = {
                                    viewModel.startPlayback(0)
                                    showResumeDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
        // 字幕选择弹窗与播放页同窗口，保证遥控器焦点可控
        if (showSubtitleDialog) {
            val selectedSubtitleIndex = subtitles.indexOfFirst { it.name == selectedSubtitleName }
            val initialSubtitleRow = when {
                selectedSubtitleIndex >= 0 -> selectedSubtitleIndex + 1
                subtitles.isNotEmpty() -> 1
                else -> 0
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(21f)
                    .background(Color(0x99000000))
                    .pointerInput(Unit) { detectTapGestures { } }
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (event.key) {
                            Key.DirectionLeft, Key.DirectionRight -> true
                            Key.DirectionUp -> focusedSubtitleRow <= 0
                            Key.DirectionDown -> focusedSubtitleRow >= subtitles.size
                            Key.Back -> {
                                showSubtitleDialog = false
                                true
                            }
                            else -> false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(640.dp)
                        .heightIn(max = 560.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.playerScrim),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "选择字幕",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusGroup()
                                .verticalScroll(rememberScrollState())
                        ) {
                            SubtitleOptionRow(
                                label = "关闭字幕",
                                trailing = "",
                                selected = selectedSubtitleName == null,
                                focusRequester = if (initialSubtitleRow == 0) subtitleDialogFocusRequester else null,
                                onFocused = { focusedSubtitleRow = 0 },
                                onClick = {
                                    viewModel.disableSubtitles()
                                    showSubtitleDialog = false
                                }
                            )
                            subtitles.forEachIndexed { index, subtitle ->
                                SubtitleOptionRow(
                                    label = subtitle.name,
                                    trailing = listOfNotNull(
                                        subtitle.language,
                                        if (subtitle.isExact) "同名" else "智能匹配"
                                    ).joinToString(" · "),
                                    selected = selectedSubtitleName == subtitle.name,
                                    focusRequester = if (initialSubtitleRow == index + 1) {
                                        subtitleDialogFocusRequester
                                    } else null,
                                    onFocused = { focusedSubtitleRow = index + 1 },
                                    onClick = {
                                        viewModel.selectSubtitle(index)
                                        showSubtitleDialog = false
                                    }
                                )
                            }
                            if (subtitles.isEmpty()) {
                                Text(
                                    text = "未找到匹配的字幕文件（支持 srt / ass / ssa / vtt）",
                                    color = Color(0xFFAAAAAA),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 36.dp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val background by animateColorAsState(
        targetValue = if (focused) Color.White else Color.Transparent,
        animationSpec = tween(150),
        label = "buttonBackground"
    )
    val iconTint by animateColorAsState(
        targetValue = if (focused) Color(0xFF1C2026) else Color.White,
        animationSpec = tween(150),
        label = "buttonIconTint"
    )
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.12f else 1f,
        animationSpec = tween(150),
        label = "buttonScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clip(CircleShape)
            .background(background)
            .then(if (focused) Modifier.border(2.dp, AppColors.primary, CircleShape) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun ResumeDialogButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    onFocusChange: (Boolean) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val background = when {
        focused -> Color.White
        primary -> AppColors.primary
        else -> Color.Transparent
    }
    val contentColor = if (focused || primary) Color(0xFF1C2026) else Color.White

    Text(
        text = text,
        color = contentColor,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { onFocusChange(it.isFocused) }
            .clip(shape)
            .background(background)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) AppColors.primary else Color(0x66FFFFFF),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun PlayerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(50)

    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        interactionSource = interactionSource,
        modifier = modifier
            .clip(shape)
            .then(
                if (focused) {
                    Modifier
                        .background(Color(0x33FFFFFF))
                        .border(2.dp, AppColors.primary, shape)
                } else {
                    Modifier
                }
            ),
        colors = SliderDefaults.colors(
            thumbColor = if (focused) Color.White else AppColors.primary,
            activeTrackColor = AppColors.primary,
            inactiveTrackColor = Color(0xFF555555)
        )
    )
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec <= 0 -> "加载中..."
        bytesPerSec < 1024 -> "$bytesPerSec B/s"
        bytesPerSec < 1024 * 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
        else -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
    }
}

@Composable
private fun SubtitleOptionRow(
    label: String,
    trailing: String,
    selected: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val contentColor = if (focused) Color(0xFF1C2026) else Color.White
    val secondaryColor = if (focused) Color(0xCC1C2026) else Color(0xAAFFFFFF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) AppColors.onSurface else Color.Transparent)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.Check else Icons.Default.Subtitles,
            contentDescription = null,
            tint = if (selected && !focused) AppColors.primary else contentColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (trailing.isNotEmpty()) {
            Text(
                text = trailing,
                color = secondaryColor,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 音频模式没有视频画面，用渐变背景与封面占位替代 */
@Composable
private fun AudioBackdrop(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B2430), Color(0xFF0C0F14), Color(0xFF1B2430))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = Spacing.xxxl, vertical = Spacing.lg)
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(Color(0x331E88E5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(96.dp)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "音频播放",
                color = Color(0x99FFFFFF),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** 抽屉列表项：多目录系列插入组头，单目录不显示组头 */
private sealed interface PlaylistDrawerItem {
    data class Header(val label: String, val count: Int) : PlaylistDrawerItem
    data class Entry(val entry: PlaylistEntry) : PlaylistDrawerItem
}

private fun buildPlaylistDrawerItems(entries: List<PlaylistEntry>): List<PlaylistDrawerItem> {
    // groupBy 保持插入顺序，条目本身按路径排序，同目录条目自然连续
    val groups = entries.groupBy { it.folderPath }
    return buildList {
        groups.values.forEach { group ->
            if (groups.size > 1) add(PlaylistDrawerItem.Header(group.first().folderLabel, group.size))
            group.forEach { add(PlaylistDrawerItem.Entry(it)) }
        }
    }
}

@Composable
private fun PlaylistGroupHeader(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = Color(0xFF999999),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = Color(0xFFBBBBBB),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${count} 个",
            color = Color(0xFF888888),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PlaylistRow(
    entry: PlaylistEntry,
    isCurrent: Boolean,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val contentColor = if (focused) Color(0xFF1C2026) else Color.White
    val secondaryColor = if (focused) Color(0xCC1C2026) else Color(0xAAFFFFFF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) AppColors.onSurface else Color.Transparent)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "${entry.index + 1}",
            color = secondaryColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = entry.name,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (isCurrent) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "播放中",
                tint = if (focused) Color(0xFF1C2026) else Color(0xFF2196F3),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    trailingText: String = "",
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    onKeyEvent: ((KeyEvent) -> Boolean)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val contentColor = if (focused) Color(0xFF1C2026) else Color.White
    val secondaryColor = if (focused) Color(0xCC1C2026) else Color(0xAAFFFFFF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) AppColors.onSurface else Color.Transparent)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(if (onKeyEvent != null) Modifier.onKeyEvent(onKeyEvent) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge
        )
        if (trailingText.isNotEmpty()) {
            Text(
                text = trailingText,
                color = secondaryColor,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
