package com.tvplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tvplayer.app.ui.component.AppButton
import com.tvplayer.app.ui.component.AppToolbar
import com.tvplayer.app.ui.filebrowser.FileBrowserPage
import com.tvplayer.app.ui.home.HomePage
import com.tvplayer.app.ui.images.ImagesPage
import com.tvplayer.app.ui.settings.SettingsPage
import com.tvplayer.app.ui.theme.AppColors
import com.tvplayer.app.ui.theme.AppShapes
import com.tvplayer.app.ui.theme.Spacing
import com.tvplayer.app.ui.theme.TVPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TVPlayerTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AppColors.pageTop,
                                        AppColors.pageMid,
                                        AppColors.pageBottom,
                                    ),
                                    center = Offset(size.width / 2f, 0f),
                                    radius = size.maxDimension * 0.85f,
                                )
                            )
                        }
                ) {
                    MainLayout(onExit = { finish() })
                }
            }
        }
    }
}

@Composable
private fun MainLayout(onExit: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var browserPath by remember { mutableStateOf<String?>(null) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var tabFocusRequest by remember { mutableIntStateOf(0) }
    // 访问过的页面保持组合，切 Tab 只改变可见性，避免每次重建整页
    var visitedTabs by remember { mutableStateOf(setOf(0)) }
    var fileRefreshTick by remember { mutableIntStateOf(0) }
    var imageRefreshTick by remember { mutableIntStateOf(0) }

    val onTabClick: (Int) -> Unit = { tab ->
        if (tab != 1) browserPath = null
        if (tab != selectedTab) {
            // 重新进入列表页时做一次轻量刷新，数据变更（同步、删除）才能反映出来
            if (tab == 1) fileRefreshTick++
            if (tab == 2) imageRefreshTick++
            selectedTab = tab
        }
        visitedTabs = visitedTabs + tab
    }

    // 文件浏览页自行处理逐级退目录；能走到这里的返回都已在顶层，弹出退出确认
    BackHandler {
        if (showExitConfirm) showExitConfirm = false else showExitConfirm = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(
                activeTab = selectedTab,
                onTabClick = onTabClick,
                tabFocusRequest = tabFocusRequest,
            )

            Box(modifier = Modifier.weight(1f)) {
                if (0 in visitedTabs) {
                    TabHost(visible = selectedTab == 0) {
                        HomePage(
                            onOpenInBrowser = { path ->
                                browserPath = path
                                onTabClick(1)
                            }
                        )
                    }
                }
                if (1 in visitedTabs) {
                    TabHost(visible = selectedTab == 1) {
                        FileBrowserPage(
                            initialPath = browserPath,
                            isActive = selectedTab == 1,
                            refreshTick = fileRefreshTick,
                        )
                    }
                }
                if (2 in visitedTabs) {
                    TabHost(visible = selectedTab == 2) {
                        ImagesPage(
                            onBackToTabs = { tabFocusRequest++ },
                            isActive = selectedTab == 2,
                            refreshTick = imageRefreshTick,
                        )
                    }
                }
                if (3 in visitedTabs) {
                    TabHost(visible = selectedTab == 3) {
                        SettingsPage()
                    }
                }
            }
        }
    }

    if (showExitConfirm) {
        ExitConfirmOverlay(
            onConfirm = onExit,
            onDismiss = { showExitConfirm = false },
        )
    }
}

/**
 * 隐藏时不测量、不布局子节点：页面保留 remember/ViewModel，但不会绘制，
 * 也不会因为 0 尺寸节点进入焦点遍历而抢走工具栏焦点。
 */
@Composable
private fun TabHost(visible: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = if (visible) {
            Modifier.fillMaxSize()
        } else {
            Modifier.layout { _, _ -> layout(0, 0) {} }
        }
    ) {
        content()
    }
}

@Composable
private fun ExitConfirmOverlay(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { cancelFocusRequester.requestFocus() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .background(AppColors.surfaceVariant, AppShapes.large)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = "退出应用",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.listHeadline,
            )
            Text(
                text = "确定要退出${stringResource(R.string.app_name)}吗？",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.listCaption,
            )
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                AppButton(
                    onClick = onDismiss,
                    modifier = Modifier.focusRequester(cancelFocusRequester),
                ) {
                    Text("取消")
                }
                AppButton(onClick = onConfirm) {
                    Text("退出")
                }
            }
        }
    }
}
