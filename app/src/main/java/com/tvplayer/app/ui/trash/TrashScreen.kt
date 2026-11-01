package com.tvplayer.app.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tvplayer.app.ui.theme.AppColors
import com.tvplayer.core.model.FileInfo
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val trashFiles by viewModel.trashFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedFile by remember { mutableStateOf<FileInfo?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("垃圾箱") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.surfaceVariant,
                    titleContentColor = AppColors.listHeadline
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppColors.background)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (trashFiles.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "垃圾箱为空",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trashFiles, key = { it.id }) { file ->
                        TrashFileItem(
                            file = file,
                            onRestore = {
                                selectedFile = file
                                showRestoreDialog = true
                            },
                            onDelete = {
                                // TODO: Implement permanent delete if needed
                            }
                        )
                    }
                }
            }
        }
    }

    if (showRestoreDialog && selectedFile != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("恢复文件") },
            text = { Text("确定要恢复 “${selectedFile!!.name}” 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreFile(selectedFile!!.path)
                        showRestoreDialog = false
                        selectedFile = null
                    }
                ) {
                    Text("恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun TrashFileItem(
    file: FileInfo,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val deletedDate = file.deletedTime?.let { dateFormat.format(Date(it)) } ?: "未知"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "删除时间: $deletedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (!file.isDirectory) {
                    Text(
                        text = "大小: ${formatFileSize(file.size)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onRestore) {
                    Icon(
                        Icons.Filled.Restore,
                        "恢复",
                        tint = AppColors.success
                    )
                }
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "%.1f KB".format(size / 1024.0)
        size < 1024 * 1024 * 1024 -> "%.1f MB".format(size / (1024.0 * 1024.0))
        else -> "%.2f GB".format(size / (1024.0 * 1024.0 * 1024.0))
    }
}
