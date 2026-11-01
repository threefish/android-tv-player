package com.tvplayer.app.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvplayer.app.R
import com.tvplayer.app.ui.theme.AppColors
import com.tvplayer.app.ui.theme.AppTypography
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
private fun rememberCurrentTime(): String {
    val timeState = produceState(initialValue = "") {
        val fmt = DateTimeFormatter.ofPattern("HH:mm")
        while (true) {
            value = LocalTime.now().format(fmt)
            delay(30_000)
        }
    }
    return timeState.value
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppToolbar(
    activeTab: Int,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabFocusRequest: Int = 0,
) {
    val tabs = listOf("首页", "视频", "图片", "设置")
    val centerFocusRequester = remember { FocusRequester() }
    val tabFocusRequesters = remember { List(tabs.size) { FocusRequester() } }

    LaunchedEffect(Unit) {
        centerFocusRequester.requestFocus()
    }

    LaunchedEffect(tabFocusRequest) {
        if (tabFocusRequest > 0) {
            runCatching { tabFocusRequesters[activeTab].requestFocus() }
        }
    }

    ToolbarLayout(
        modifier = modifier
            .height(95.dp)
            .padding(horizontal = 48.dp, vertical = 27.dp),
        start = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = AppColors.primary,
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = AppTypography.titleMedium,
                    color = AppColors.listHeader,
                )
            }
        },
        center = {
            Row(
                modifier = Modifier
                    .focusGroup()
                    .focusRequester(centerFocusRequester),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, label ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val focused by interactionSource.collectIsFocusedAsState()

                    // TV 顶部导航按“焦点即切换”处理，无需再按 OK
                    LaunchedEffect(focused) {
                        if (focused && activeTab != index) onTabClick(index)
                    }

                    ToolbarTab(
                        label = label,
                        active = activeTab == index,
                        onClick = { onTabClick(index) },
                        interactionSource = interactionSource,
                        modifier = Modifier.focusRequester(tabFocusRequesters[index]),
                    )
                }
            }
        },
        end = {
            ToolbarClock()
        },
    )
}

@Composable
private fun ToolbarClock() {
    val time = rememberCurrentTime()
    Text(
        text = time,
        fontSize = 20.sp,
        color = Color.White,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolbarTab(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = CircleShape
    // 激活态持续标识当前页；聚焦态叠加白色描边，两种状态可同时辨认
    val containerColor = when {
        active -> AppColors.primary
        focused -> AppColors.buttonFocused
        else -> AppColors.button
    }
    val contentColor = when {
        active -> AppColors.background
        focused -> AppColors.onButtonFocused
        else -> AppColors.onButton
    }

    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 58.dp, minHeight = 40.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .background(containerColor, shape)
            .then(if (active && focused) Modifier.border(2.dp, Color.White, shape) else Modifier)
            .padding(AppButtonDefaults.ContentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = contentColor,
        )
    }
}

@Composable
private fun ToolbarLayout(
    modifier: Modifier = Modifier,
    start: @Composable () -> Unit = {},
    center: @Composable () -> Unit = {},
    end: @Composable () -> Unit = {},
) = SubcomposeLayout(modifier = modifier) { constraints ->
    val sideMaxWidth = constraints.maxWidth / 3
    val sideConstraints = constraints.copy(minWidth = 0, maxWidth = sideMaxWidth)

    val startPlaceables = subcompose("start", start).map { it.measure(sideConstraints) }
    val endPlaceables = subcompose("end", end).map { it.measure(sideConstraints) }

    val sideWidth = maxOf(
        startPlaceables.maxOfOrNull { it.width } ?: 0,
        endPlaceables.maxOfOrNull { it.width } ?: 0,
    )

    val centerMaxWidth = (constraints.maxWidth - 2 * sideWidth).coerceAtLeast(0)
    val centerPlaceables = subcompose("center", center)
        .map { it.measure(constraints.copy(minWidth = 0, maxWidth = centerMaxWidth)) }

    val height = listOf(
        startPlaceables.maxOfOrNull { it.height } ?: 0,
        centerPlaceables.maxOfOrNull { it.height } ?: 0,
        endPlaceables.maxOfOrNull { it.height } ?: 0,
    ).maxOrNull() ?: 0

    layout(constraints.maxWidth, height) {
        startPlaceables.forEach { it.placeRelative(0, (height - it.height) / 2) }
        centerPlaceables.forEach {
            it.place((constraints.maxWidth - it.width) / 2, (height - it.height) / 2)
        }
        endPlaceables.forEach {
            it.placeRelative(constraints.maxWidth - it.width, (height - it.height) / 2)
        }
    }
}
