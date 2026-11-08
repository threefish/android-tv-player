package com.tvplayer.app.ui.settings

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tvplayer.app.ui.component.AppButton
import com.tvplayer.app.ui.component.AppButtonDefaults
import com.tvplayer.app.ui.theme.AppColors
import com.tvplayer.app.ui.theme.AppShapes
import com.tvplayer.app.ui.theme.Spacing
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.xxxl, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item {
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.listHeadline
            )
        }

        // File Sources + QR Code row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                // File Sources section
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("文件来源", style = MaterialTheme.typography.titleMedium, color = AppColors.listHeadline)
                        AppButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = AppColors.onButton)
                            Spacer(Modifier.width(Spacing.xs))
                            Text("新增")
                        }
                    }

                    if (webDavConfig != null) {
                        SourceRow(
                            type = "WebDAV",
                            url = webDavConfig!!.url,
                            isTesting = isTesting,
                            onTest = { viewModel.testConnection() },
                            onEdit = { showEditDialog = true },
                            onDelete = { showDeleteConfirm = true }
                        )
                    } else {
                        Text(
                            "暂无文件来源，点击新增添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.listCaption
                        )
                    }
                }

                // QR Code
                Column(
                    modifier = Modifier.width(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Web管理二维码",
                            modifier = Modifier.size(140.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("二维码生成失败", style = MaterialTheme.typography.bodySmall, color = AppColors.listCaption)
                        }
                    }
                    Text("扫码访问Web管理", style = MaterialTheme.typography.bodySmall, color = AppColors.listCaption)
                    Text(
                        text = webUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.listHeadline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Playback Settings
        item {
            SettingsSection(title = "播放设置") {
                SettingsRow("默认倍数") {
                    SpeedSelector(currentSpeed = playbackSpeed, onSpeedChange = viewModel::updatePlaybackSpeed)
                }
                SettingsRow("快进/快退步长") {
                    SeekStepSelector(currentStep = seekStep, onStepChange = viewModel::updateSeekStep)
                }
            }
        }

        // Play Reminder
        item {
            SettingsSection(title = "播放提醒") {
                SettingsRow("启用播放提醒") {
                    Switch(
                        checked = playReminder.enabled,
                        onCheckedChange = { viewModel.updatePlayReminder(playReminder.copy(enabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AppColors.primary,
                            checkedTrackColor = AppColors.primaryContainer,
                            uncheckedThumbColor = AppColors.onSurfaceVariant,
                            uncheckedTrackColor = AppColors.outline,
                        )
                    )
                }
                SettingsRow("连续播放时长") {
                    MinutesSelector(
                        currentMinutes = playReminder.playMinutes,
                        options = listOf(1, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180, 240),
                        onMinutesChange = { viewModel.updatePlayReminder(playReminder.copy(playMinutes = it)) }
                    )
                }
                SettingsRow("休息时长") {
                    MinutesSelector(
                        currentMinutes = playReminder.restMinutes,
                        options = listOf(1, 3, 5, 10, 15, 20, 30, 60),
                        onMinutesChange = { viewModel.updatePlayReminder(playReminder.copy(restMinutes = it)) }
                    )
                }
                SettingsRow("休息结束后") {
                    ResumeModeSelector(
                        autoResume = playReminder.autoResume,
                        onModeChange = { viewModel.updatePlayReminder(playReminder.copy(autoResume = it)) }
                    )
                }
            }
        }

        // Subtitle Settings
        item {
            SettingsSection(title = "字幕设置") {
                SettingsRow("字幕大小") {
                    SubtitleSizeSelector(currentSize = subtitleSize, onSizeChange = viewModel::updateSubtitleSize)
                }
                SettingsRow("双字幕开关") {
                    Switch(
                        checked = dualSubtitle,
                        onCheckedChange = { viewModel.toggleDualSubtitle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AppColors.primary,
                            checkedTrackColor = AppColors.primaryContainer,
                            uncheckedThumbColor = AppColors.onSurfaceVariant,
                            uncheckedTrackColor = AppColors.outline,
                        )
                    )
                }
            }
        }

        // About
        item {
            SettingsSection(title = "关于") {
                SettingsRow("版本") {
                    Text(viewModel.version, style = MaterialTheme.typography.bodyMedium, color = AppColors.listCaption)
                }
            }
        }
    }

    // Edit dialog
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

    // Delete confirmation
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
private fun SourceRow(
    type: String,
    url: String,
    isTesting: Boolean,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(AppColors.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(type, style = MaterialTheme.typography.titleSmall, color = AppColors.listHeadline)
                Text(url, style = MaterialTheme.typography.bodySmall, color = AppColors.listCaption, maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppButton(onClick = onEdit) { Text("修改") }
                AppButton(onClick = onDelete) { Text("删除", color = AppColors.error) }
                AppButton(onClick = onTest, enabled = !isTesting) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AppColors.onButton)
                    } else {
                        Text("测试连接")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = AppColors.listHeadline)
        content()
    }
}

@Composable
private fun SettingsRow(
    label: String,
    trailing: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = AppColors.onBackground)
        trailing()
    }
}

@Composable
private fun SpeedSelector(currentSpeed: Float, onSpeedChange: (Float) -> Unit) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    var expanded by remember { mutableStateOf(false) }
    Box {
        AppButton(onClick = { expanded = true }) {
            Text("${currentSpeed}x")
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
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
        AppButton(onClick = { expanded = true }) {
            Text("${currentStep}秒")
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
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
        AppButton(onClick = { expanded = true }) {
            Text("${(currentSize * 100).toInt()}%")
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
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
        AppButton(onClick = { expanded = true }) {
            Text("${currentMinutes}分钟")
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
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
        AppButton(onClick = { expanded = true }) {
            Text(if (autoResume) "自动继续" else "手动确认")
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
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
        focusedTextColor = AppColors.listHeadline,
        unfocusedTextColor = AppColors.listHeadline,
        focusedContainerColor = AppColors.surfaceVariant,
        unfocusedContainerColor = AppColors.surfaceVariant,
        cursorColor = AppColors.primary,
        focusedBorderColor = AppColors.primary,
        unfocusedBorderColor = AppColors.outline,
        focusedLabelColor = AppColors.primary,
        unfocusedLabelColor = AppColors.listCaption,
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
