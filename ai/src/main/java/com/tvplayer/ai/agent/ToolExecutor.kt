package com.tvplayer.ai.agent

import android.content.Context
import android.content.Intent
import com.tvplayer.core.repository.FileRepository
import com.tvplayer.core.repository.PlayHistoryRepository
import com.tvplayer.core.repository.WebDavConfigRepository
import com.tvplayer.webdav.WebDavClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileRepository: FileRepository,
    private val playHistoryRepository: PlayHistoryRepository,
    private val webDavConfigRepository: WebDavConfigRepository
) {
    suspend fun execute(toolName: String, args: Map<String, Any>): String {
        return try {
            val result = when (toolName) {
                "search_files" -> searchFiles(args)
                "list_directory" -> listDirectory(args)
                "get_file_info" -> getFileInfo(args)
                "get_play_history" -> getPlayHistory(args)
                "rename_file" -> renameFile(args)
                "move_file" -> moveFile(args)
                "delete_to_trash" -> deleteToTrash(args)
                "restore_from_trash" -> restoreFromTrash(args)
                "play_file" -> playFile(args)
                else -> "错误: 未知工具 $toolName"
            }
            result
        } catch (e: Exception) {
            "执行异常: ${e.message}"
        }
    }

    private suspend fun searchFiles(args: Map<String, Any>): String {
        val keyword = args["keyword"] as? String ?: return "错误: 缺少 keyword 参数"
        val fileType = args["fileType"] as? String ?: "all"
        val pathFilter = (args["path"] as? String)?.trim().orEmpty()
        val files = fileRepository.search(keyword, fileType)
            .filter { pathFilter.isEmpty() || it.path.startsWith(pathFilter) }
            .sortedBy { it.path }
            .take(20)
        return formatFileList(files)
    }

    private suspend fun listDirectory(args: Map<String, Any>): String {
        val path = args["path"] as? String ?: "/"
        val files = fileRepository.getChildren(path)
        return formatFileList(files)
    }

    private suspend fun getFileInfo(args: Map<String, Any>): String {
        val path = args["path"] as? String ?: return "错误: 缺少 path 参数"
        val file = fileRepository.getByPath(path)
        return if (file != null) {
            JSONObject().apply {
                put("path", file.path)
                put("name", file.name)
                put("isDirectory", file.isDirectory)
                put("size", file.size)
                put("modifiedTime", file.modifiedTime)
            }.toString()
        } else {
            "文件不存在"
        }
    }

    private suspend fun getPlayHistory(args: Map<String, Any>): String {
        val limit = (args["limit"] as? Number)?.toInt() ?: 10
        val history = playHistoryRepository.getRecent(limit)
        val jsonArray = JSONArray()
        for (h in history) {
            jsonArray.put(JSONObject().apply {
                put("filePath", h.filePath)
                put("fileName", h.fileName)
                put("position", h.position)
                put("duration", h.duration)
            })
        }
        return jsonArray.toString()
    }

    private suspend fun renameFile(args: Map<String, Any>): String {
        val path = args["path"] as? String ?: return "错误: 缺少 path 参数"
        val newName = args["newName"] as? String ?: return "错误: 缺少 newName 参数"
        
        // Get WebDAV config and create client
        val config = webDavConfigRepository.getActiveConfig()
            ?: return "错误: 未配置 WebDAV"
        val webDavClient = WebDavClient(config)
        
        // Calculate new path
        val parentPath = path.substringBeforeLast("/", "/")
        val newPath = if (parentPath == "/") "/$newName" else "$parentPath/$newName"
        
        // Move on WebDAV server (rename is implemented as move)
        val success = webDavClient.move(path, newPath)
        if (!success) {
            return "错误: WebDAV 重命名失败"
        }
        
        // Update local database
        fileRepository.rename(path, newName)
        return "已重命名为 $newName"
    }

    private suspend fun moveFile(args: Map<String, Any>): String {
        val sourcePath = args["sourcePath"] as? String ?: return "错误: 缺少 sourcePath 参数"
        val targetDir = args["targetDir"] as? String ?: return "错误: 缺少 targetDir 参数"
        
        // Get WebDAV config and create client
        val config = webDavConfigRepository.getActiveConfig()
            ?: return "错误: 未配置 WebDAV"
        val webDavClient = WebDavClient(config)
        
        // Calculate new path
        val fileName = sourcePath.substringAfterLast("/")
        val newPath = if (targetDir == "/") "/$fileName" else "$targetDir/$fileName"
        
        // Move on WebDAV server
        val success = webDavClient.move(sourcePath, newPath)
        if (!success) {
            return "错误: WebDAV 移动失败"
        }
        
        // Update local database
        fileRepository.move(sourcePath, targetDir)
        return "已移动到 $targetDir"
    }

    private suspend fun deleteToTrash(args: Map<String, Any>): String {
        val path = args["path"] as? String ?: return "错误: 缺少 path 参数"
        fileRepository.markAsDeleted(path)
        return "已移到垃圾箱"
    }

    private suspend fun restoreFromTrash(args: Map<String, Any>): String {
        val path = args["path"] as? String ?: return "错误: 缺少 path 参数"
        fileRepository.restore(path)
        return "已从垃圾箱恢复"
    }

    private suspend fun playFile(args: Map<String, Any>): String {
        val rawPath = (args["path"] as? String)?.trim().orEmpty()
        if (rawPath.isEmpty()) return "错误: 缺少 path 参数"
        val path = if (rawPath.startsWith("/")) rawPath else "/$rawPath"
        val file = fileRepository.getByPath(path) ?: return "文件不存在: $path"
        if (file.isDirectory) return "这是一个目录，请指定具体的视频文件: $path"

        val intent = Intent().apply {
            setClassName(context.packageName, "com.tvplayer.app.ui.player.VideoPlayerActivity")
            putExtra("videoPath", file.path)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        withContext(Dispatchers.Main) {
            context.startActivity(intent)
        }
        return "正在电视上播放: ${file.name}"
    }

    private fun formatFileList(files: List<com.tvplayer.core.model.FileInfo>): String {
        if (files.isEmpty()) return "没有找到文件"
        val jsonArray = JSONArray()
        for (file in files) {
            jsonArray.put(JSONObject().apply {
                put("path", file.path)
                put("name", file.name)
                put("isDirectory", file.isDirectory)
                put("size", file.size)
            })
        }
        return jsonArray.toString()
    }
}
