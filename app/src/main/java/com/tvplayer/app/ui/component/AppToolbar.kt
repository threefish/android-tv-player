package com.tvplayer.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tvplayer.app.R
import com.tvplayer.app.ui.theme.AppColors
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
private fun rememberCurrentTime(): String {
    val timeState = produceState(initialValue = "") {
        val fmt = DateTimeFormatter.ofPattern("HH:mm:ss")
        while (true) {
            value = LocalTime.now().format(fmt)
            delay(1_000)
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
            .height(80.dp)
            .background(Color(0x660D111A))
            .drawBehind {
                drawLine(
                    color = Color(0x0DFFFFFF),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 56.dp),
        start = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = AppColors.primary,
                )
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary,
                )
            }
        },
        center = {
            Row(
                modifier = Modifier
                    .focusGroup()
                    .focusRequester(centerFocusRequester)
                    .background(AppColors.navBackground, CircleShape)
                    .border(1.dp, Color(0x14FFFFFF), CircleShape)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.textPrimary,
        letterSpacing = 1.sp,
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
    val containerColor by animateColorAsState(
        targetValue = when {
            focused -> Color.White
            active -> Color(0x26FFFFFF)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "tabContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            focused -> Color.Black
            active -> Color.White
            else -> AppColors.textMuted
        },
        animationSpec = tween(150),
        label = "tabContent",
    )
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.04f else 1f,
        animationSpec = tween(150),
        label = "tabScale",
    )

    Row(
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .defaultMinSize(minWidth = 58.dp, minHeight = 46.dp)
            .shadow(
                elevation = if (focused) 16.dp else 0.dp,
                shape = shape,
                ambientColor = AppColors.focusShadow,
                spotColor = AppColors.focusShadow,
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .background(containerColor, shape)
            .clip(shape)
            .padding(horizontal = 32.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
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
    // 松开固定高度，否则侧边内容会被撑满整条工具栏，文字只能顶在上沿
    val sideConstraints = constraints.copy(minWidth = 0, minHeight = 0, maxWidth = sideMaxWidth)

    val startPlaceables = subcompose("start", start).map { it.measure(sideConstraints) }
    val endPlaceables = subcompose("end", end).map { it.measure(sideConstraints) }

    val sideWidth = maxOf(
        startPlaceables.maxOfOrNull { it.width } ?: 0,
        endPlaceables.maxOfOrNull { it.width } ?: 0,
    )

    val centerMaxWidth = (constraints.maxWidth - 2 * sideWidth).coerceAtLeast(0)
    val centerPlaceables = subcompose("center", center)
        .map { it.measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = centerMaxWidth)) }

    val height = constraints.maxHeight

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
