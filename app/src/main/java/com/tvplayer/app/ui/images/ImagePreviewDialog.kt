package com.tvplayer.app.ui.images

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Dimension
import coil.size.Precision
import coil.size.Size
import com.tvplayer.app.image.WebDavImageKey
import com.tvplayer.app.ui.theme.AppColors
import com.tvplayer.app.ui.theme.Spacing
import com.tvplayer.core.model.FileInfo
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PREVIEW_ZOOM_LEVELS = listOf(1f, 1.5f, 2f)

// 电视 GPU 纹理上限 4096，超过会整张图无法上屏；解码尺寸需限制在该范围内
private const val MAX_PREVIEW_TEXTURE_PX = 4000

@Composable
fun ImagePreviewDialog(
    images: List<FileInfo>,
    startIndex: Int,
    sourceId: Long,
    onClose: (Int) -> Unit,
) {
    if (images.isEmpty()) return

    var index by remember {
        mutableIntStateOf(startIndex.coerceIn(0, images.lastIndex))
    }
    var zoom by remember { mutableFloatStateOf(1f) }
    val focusRequester = remember { FocusRequester() }
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
    val verticalStep = screenHeightPx * 0.45f
    val horizontalStep = screenWidthPx * 0.45f

    val file = images.getOrNull(index) ?: return

    val request = remember(file.path, file.modifiedTime, sourceId, screenWidthPx) {
        ImageRequest.Builder(context)
            .data(WebDavImageKey(sourceId, file.path, file.modifiedTime))
            // 只限制宽度、保留原始长宽比：长图按屏宽解码后再上下滚动，而不是缩成整屏
            .size(
                Size(
                    Dimension.Pixels(screenWidthPx),
                    Dimension.Pixels(MAX_PREVIEW_TEXTURE_PX),
                )
            )
            // 小图不放大，避免无谓的内存占用
            .precision(Precision.INEXACT)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .crossfade(false)
            .build()
    }
    val painter = rememberAsyncImagePainter(model = request)
    val painterState = painter.state

    LaunchedEffect(index) {
        zoom = 1f
        verticalScroll.scrollTo(0)
        horizontalScroll.scrollTo(0)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val cycleZoom = {
        val current = PREVIEW_ZOOM_LEVELS.indexOf(zoom).coerceAtLeast(0)
        val next = PREVIEW_ZOOM_LEVELS[(current + 1) % PREVIEW_ZOOM_LEVELS.size]
        val factor = next / zoom
        val oldVertical = verticalScroll.value
        val oldHorizontal = horizontalScroll.value
        val fromFitWidth = zoom == 1f
        zoom = next
        scope.launch {
            // 等新尺寸完成测量后再恢复滚动位置，否则 maxValue 还是旧值
            withFrameNanos { }
            withFrameNanos { }
            verticalScroll.scrollTo((oldVertical * factor).roundToInt())
            val target = if (fromFitWidth) {
                horizontalScroll.maxValue / 2
            } else {
                (oldHorizontal * factor).roundToInt()
            }
            horizontalScroll.scrollTo(target)
        }
    }

    Dialog(
        onDismissRequest = { onClose(index) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (zoom > 1f) {
                                horizontalScroll.dispatchRawDelta(-horizontalStep)
                            } else if (index > 0) {
                                index--
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            if (zoom > 1f) {
                                horizontalScroll.dispatchRawDelta(horizontalStep)
                            } else if (index < images.lastIndex) {
                                index++
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            verticalScroll.dispatchRawDelta(-verticalStep)
                            true
                        }
                        Key.DirectionDown -> {
                            verticalScroll.dispatchRawDelta(verticalStep)
                            true
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            if (event.nativeKeyEvent.repeatCount == 0) cycleZoom()
                            true
                        }
                        else -> false
                    }
                }
                .focusable(),
        ) {
            val viewportWidth = maxWidth
            val viewportHeight = maxHeight
            val intrinsic = painter.intrinsicSize
            val hasIntrinsic = intrinsic.isSpecified &&
                intrinsic.width > 0f && intrinsic.height > 0f
            val aspect = if (hasIntrinsic) intrinsic.width / intrinsic.height else 0f
            val contentWidth = viewportWidth * zoom
            val contentHeight = if (aspect > 0f) contentWidth / aspect else viewportHeight

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScroll)
                    .verticalScroll(verticalScroll),
            ) {
                if (hasIntrinsic && painterState is AsyncImagePainter.State.Success) {
                    Image(
                        painter = painter,
                        contentDescription = file.name,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .offset(
                                x = if (contentWidth < viewportWidth) {
                                    (viewportWidth - contentWidth) / 2
                                } else {
                                    0.dp
                                },
                                y = if (contentHeight < viewportHeight) {
                                    (viewportHeight - contentHeight) / 2
                                } else {
                                    0.dp
                                },
                            )
                            .width(contentWidth)
                            .height(contentHeight),
                    )
                }
            }

            if (painterState !is AsyncImagePainter.State.Success) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (painterState is AsyncImagePainter.State.Error) {
                        Text(
                            text = "图片加载失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.listCaption,
                        )
                    } else {
                        CircularProgressIndicator(color = AppColors.primary)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.88f), Color.Transparent),
                        )
                    )
                    .padding(horizontal = Spacing.xxxl, vertical = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1}/${images.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.primary,
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Text(
                    text = zoomLabel(zoom),
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.primary,
                )
            }

            Text(
                text = if (zoom == 1f) {
                    "← → 切换 · ↑ ↓ 滚动 · OK 放大 · 返回关闭"
                } else {
                    "方向键移动画面 · OK 放大 / 还原 · 返回关闭"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        )
                    )
                    .padding(vertical = Spacing.lg),
            )
        }
    }
}

private fun zoomLabel(zoom: Float): String = when (zoom) {
    1f -> "适应宽度"
    2f -> "放大 2×"
    else -> "放大 ${zoom}×"
}
