package com.tvplayer.app.ui.settings

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tvplayer.app.ui.component.AppButton
import com.tvplayer.app.ui.component.AppButtonDefaults
import com.tvplayer.app.ui.theme.AppColors
import com.tvplayer.app.util.NetworkUtils
import com.tvplayer.app.util.QrCodeGenerator
import com.tvplayer.core.model.WebDavConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val webDavConfig by viewModel.webDavConfig.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val seekStep by viewModel.seekStep.collectAsState()
    val subtitleSize by viewModel.subtitleSize.collectAsState()
    val dualSubtitle by viewModel.dualSubtitle.collectAsState()
    val playReminder by viewModel.playReminder.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val webPort by viewModel.webPort.collectAsState()
    val localIp = remember { NetworkUtils.getLocalIpAddress() }
    val webUrl = "http://${localIp ?: "127.0.0.1"}:$webPort"
    val qrBitmap = remember(webUrl) { QrCodeGenerator.generate(webUrl, 400) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // 去掉大标题与分组卡片，用分组标题 + 紧凑行承载参数，一屏可扫读
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 56.dp, end = 56.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(end = 4.dp, bottom = 12.dp),
        ) {
            SectionHeader(
                title = "文件来源",
                action = {
                    ActionButton(label = "+ 新增", onClick = { showEditDialog = true })
                },
            )
            if (webDavConfig != null) {
                SourceBlock(
                    url = webDavConfig!!.url,
                    status = connectionStatus,
                    isTesting = isTesting,
                    onTest = { viewModel.testConnection() },
                    onEdit = { showEditDialog = true },
                    onDelete = { showDeleteConfirm = true },
                )
            } else {
                Text(
                    text = "暂无文件来源，点击右侧新增",
                    fontSize = 13.sp,
                    color = AppColors.textMuted,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionHeader(title = "播放")
                    SettingRow(label = "默认倍速") {
                        SpeedSelector(currentSpeed = playbackSpeed, onSpeedChange = viewModel::updatePlaybackSpeed)
                    }
                    SettingRow(label = "快进 / 快退步长", showDivider = false) {
                        SeekStepSelector(currentStep = seekStep, onStepChange = viewModel::updateSeekStep)
                    }
                    Spacer(Modifier.height(12.dp))
                    SectionHeader(title = "字幕")
                    SettingRow(label = "字幕大小") {
                        SubtitleSizeSelector(currentSize = subtitleSize, onSizeChange = viewModel::updateSubtitleSize)
                    }
                    SettingRow(
                        label = "双字幕",
                        description = "播放页可选第二字幕同屏显示",
                        showDivider = false,
                    ) {
                        Switch(
                            checked = dualSubtitle,
                            onCheckedChange = { viewModel.toggleDualSubtitle() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppColors.primary,
                                uncheckedThumbColor = AppColors.onSurfaceVariant,
                                uncheckedTrackColor = AppColors.outline,
                            )
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    SectionHeader(title = "播放提醒")
                    SettingRow(label = "启用播放提醒") {
                        Switch(
                            checked = playReminder.enabled,
                            onCheckedChange = { viewModel.updatePlayReminder(playReminder.copy(enabled = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppColors.primary,
                                uncheckedThumbColor = AppColors.onSurfaceVariant,
                                uncheckedTrackColor = AppColors.outline,
                            )
                        )
                    }
                    SettingRow(label = "连续播放时长") {
                        MinutesSelector(
                            currentMinutes = playReminder.playMinutes,
                            options = listOf(1, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180, 240),
                            onMinutesChange = { viewModel.updatePlayReminder(playReminder.copy(playMinutes = it)) }
                        )
                    }
                    SettingRow(label = "休息时长") {
                        MinutesSelector(
                            currentMinutes = playReminder.restMinutes,
                            options = listOf(1, 3, 5, 10, 15, 20, 30, 60),
                            onMinutesChange = { viewModel.updatePlayReminder(playReminder.copy(restMinutes = it)) }
                        )
                    }
                    SettingRow(label = "休息结束后", showDivider = false) {
                        ResumeModeSelector(
                            autoResume = playReminder.autoResume,
                            onModeChange = { viewModel.updatePlayReminder(playReminder.copy(autoResume = it)) }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    SectionHeader(title = "关于")
                    SettingRow(label = "版本", showDivider = false) {
                        Text(
                            text = viewModel.version,
                            fontSize = 13.sp,
                            color = AppColors.textMuted,
                        )
                    }
                }
            }
        }

        // 右栏 1fr：Web 管理二维码
        Column(modifier = Modifier.weight(1f)) {
            QrCard(webUrl = webUrl, qrBitmap = qrBitmap)
        }
    }

    if (showEditDialog) {
        WebDavEditDialog(
            existingConfig = webDavConfig,
            isTesting = isTesting,
            onTest = viewModel::testConnection,
            onDismiss = { showEditDialog = false },
            onSave = { config ->
                viewModel.saveConfig(config)
                showEditDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除此文件来源吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteConfig()
                    showDeleteConfirm = false
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp)
            .heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(12.dp)
                .background(AppColors.primary, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            color = AppColors.textMuted,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

@Composable
private fun SettingRow(
    label: String,
    description: String? = null,
    showDivider: Boolean = true,
    trailing: @Composable RowScope.() -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    color = AppColors.textPrimary,
                )
                if (description != null) {
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = AppColors.textMuted,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
            trailing()
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.divider)
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    danger: Boolean = false,
    modifier: Modifier = Modifier,
) {
    AppButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        colors = if (danger) AppButtonDefaults.dangerColors() else AppButtonDefaults.colors(),
        borderColor = if (danger) Color(0x4DF87171) else Color(0x26FFFFFF),
    ) {
        Text(text = label, fontSize = 14.sp)
    }
}

@Composable
private fun SourceBlock(
    url: String,
    status: Boolean?,
    isTesting: Boolean,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.inset)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "WebDAV 服务",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary,
                )
                StatusTag(status = status)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(
                    onClick = onTest,
                    enabled = !isTesting,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(if (isTesting) "测试中…" else "测试连接", fontSize = 14.sp)
                }
                ActionButton(label = "修改", onClick = onEdit)
                ActionButton(label = "删除", onClick = onDelete, danger = true)
            }
        }
        Text(
            text = url,
            fontSize = 12.sp,
            color = AppColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun StatusTag(status: Boolean?) {
    val text = when (status) {
        true -> "已连接"
        false -> "连接失败"
        null -> "已配置"
    }
    val content = when (status) {
        true -> AppColors.accentSoft
        false -> AppColors.danger
        null -> AppColors.textMuted
    }
    val background = when (status) {
        true -> Color(0x333B82F6)
        false -> Color(0x33EF4444)
        null -> Color(0x1AFFFFFF)
    }
    val border = when (status) {
        true -> Color(0x663B82F6)
        false -> Color(0x66EF4444)
        null -> Color(0x33FFFFFF)
    }
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = content,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

@Composable
private fun QrCard(webUrl: String, qrBitmap: Bitmap?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.card)
            .border(1.dp, AppColors.cardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "扫码访问 Web 管理后台",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "使用局域网设备打开浏览器访问：",
            fontSize = 12.sp,
            color = AppColors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Box(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 8.dp)
                .size(104.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Web管理二维码",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = "二维码生成失败",
                    fontSize = 11.sp,
                    color = Color(0xFF666666),
                )
            }
        }
        Text(
            text = webUrl,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.accentSoft,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SelectorButton(text: String, onClick: () -> Unit) {
    AppButton(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(text = text, fontSize = 14.sp)
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SpeedSelector(currentSpeed: Float, onSpeedChange: (Float) -> Unit) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    var expanded by remember { mutableStateOf(false) }
    Box {
        SelectorButton(text = "${currentSpeed}x") { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            speeds.forEach { speed ->
                DropdownMenuItem(text = { Text("${speed}x") }, onClick = { onSpeedChange(speed); expanded = false })
            }
        }
    }
}

@Composable
private fun SeekStepSelector(currentStep: Int, onStepChange: (Int) -> Unit) {
    val steps = listOf(5, 10, 15, 30, 60)
    var expanded by remember { mutableStateOf(false) }
    Box {
        SelectorButton(text = "${currentStep}秒") { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            steps.forEach { step ->
                DropdownMenuItem(text = { Text("${step}秒") }, onClick = { onStepChange(step); expanded = false })
            }
        }
    }
}

@Composable
private fun SubtitleSizeSelector(currentSize: Float, onSizeChange: (Float) -> Unit) {
    val sizes = listOf(0.8f, 1.0f, 1.2f, 1.5f, 2.0f)
    var expanded by remember { mutableStateOf(false) }
    Box {
        SelectorButton(text = "${(currentSize * 100).toInt()}%") { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sizes.forEach { size ->
                DropdownMenuItem(text = { Text("${(size * 100).toInt()}%") }, onClick = { onSizeChange(size); expanded = false })
            }
        }
    }
}

@Composable
private fun MinutesSelector(currentMinutes: Int, options: List<Int>, onMinutesChange: (Int) -> Unit) {
    // Web 端可设任意 1-240，选项列表并入当前值保证回显
    val choices = remember(currentMinutes, options) { (options + currentMinutes).distinct().sorted() }
    var expanded by remember { mutableStateOf(false) }
    Box {
        SelectorButton(text = "${currentMinutes}分钟") { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { minutes ->
                DropdownMenuItem(text = { Text("${minutes}分钟") }, onClick = { onMinutesChange(minutes); expanded = false })
            }
        }
    }
}

@Composable
private fun ResumeModeSelector(autoResume: Boolean, onModeChange: (Boolean) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SelectorButton(text = if (autoResume) "自动继续" else "手动确认") { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("手动确认继续") }, onClick = { onModeChange(false); expanded = false })
            DropdownMenuItem(text = { Text("自动继续播放") }, onClick = { onModeChange(true); expanded = false })
        }
    }
}

@Composable
private fun WebDavEditDialog(
    existingConfig: WebDavConfig?,
    isTesting: Boolean,
    onTest: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (WebDavConfig) -> Unit
) {
    var url by remember { mutableStateOf(existingConfig?.url ?: "") }
    var username by remember { mutableStateOf(existingConfig?.username ?: "") }
    var password by remember { mutableStateOf(existingConfig?.password ?: "") }
    var rootPath by remember { mutableStateOf(existingConfig?.rootPath ?: "/") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = AppColors.textPrimary,
        unfocusedTextColor = AppColors.textPrimary,
        focusedContainerColor = AppColors.surfaceVariant,
        unfocusedContainerColor = AppColors.surfaceVariant,
        cursorColor = AppColors.primary,
        focusedBorderColor = AppColors.primary,
        unfocusedBorderColor = AppColors.outline,
        focusedLabelColor = AppColors.primary,
        unfocusedLabelColor = AppColors.textMuted,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingConfig != null) "修改文件来源" else "新增文件来源") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("http://192.168.1.100:5005") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors,
                )
                OutlinedTextField(
                    value = rootPath,
                    onValueChange = { rootPath = it },
                    label = { Text("根路径") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppButton(
                        onClick = onTest,
                        enabled = !isTesting,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (isTesting) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text("测试连接")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(WebDavConfig(id = existingConfig?.id ?: 0, name = "WebDAV", url = url, username = username, password = password, rootPath = rootPath))
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
