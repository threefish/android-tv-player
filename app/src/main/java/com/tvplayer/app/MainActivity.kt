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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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

    // 文件浏览页自行处理逐级退目录；能走到这里的返回都已在顶层，弹出退出确认
    BackHandler {
        if (showExitConfirm) showExitConfirm = false else showExitConfirm = true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppToolbar(
            activeTab = selectedTab,
            onTabClick = { tab ->
                if (tab != 1) browserPath = null
                selectedTab = tab
            },
            tabFocusRequest = tabFocusRequest,
        )

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> HomePage(
                    onOpenInBrowser = { path ->
                        browserPath = path
                        selectedTab = 1
                    }
                )
                1 -> FileBrowserPage(initialPath = browserPath)
                2 -> ImagesPage(
                    onBackToTabs = { tabFocusRequest++ }
                )
                3 -> SettingsPage()
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
