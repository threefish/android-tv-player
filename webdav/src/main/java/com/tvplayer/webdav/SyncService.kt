package com.tvplayer.webdav

import android.util.Log
import com.tvplayer.core.db.FileDao
import com.tvplayer.core.db.FileEntity
import com.tvplayer.core.db.WebDavConfigDao
import com.tvplayer.core.model.FileInfo
import com.tvplayer.core.model.WebDavConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncService @Inject constructor(
    private val webDavConfigDao: WebDavConfigDao,
    private val fileDao: FileDao
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    sealed class SyncState {
        object Idle : SyncState()
        data class Syncing(val current: Int, val total: Int, val path: String) : SyncState()
        data class Error(val message: String) : SyncState()
        data class Completed(
            val fileCount: Int,
            val duration: Long,
            val failedCount: Int = 0
        ) : SyncState()
    }

    private val syncMutex = Mutex()

    suspend fun fullSync(): Result<Int> = syncMutex.withLock {
        withContext(Dispatchers.IO) { runSync() }
    }

    /**
     * 距上次成功同步不足 [maxAgeMs] 且库里已有数据时直接跳过，返回 null。
     * 用于进入文件浏览页时的自动同步，避免每次都全量重扫。
     */
    suspend fun syncIfStale(maxAgeMs: Long = DEFAULT_SYNC_INTERVAL_MS): Result<Int>? = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            val entity = webDavConfigDao.getActiveConfig() ?: return@withContext null
            val lastSync = entity.lastSyncTime ?: 0L
            val fresh = fileDao.getRoot() != null &&
                System.currentTimeMillis() - lastSync < maxAgeMs
            if (fresh) null else runSync()
        }
    }

    private suspend fun runSync(): Result<Int> {
        val entity = webDavConfigDao.getActiveConfig() ?: return Result.failure(Exception("No WebDAV config"))
        val config = WebDavConfig(
            id = entity.id,
            name = entity.name,
            url = entity.url,
            username = entity.username,
            password = entity.password,
            rootPath = entity.rootPath,
            isEnabled = entity.isEnabled
        )
        val client = WebDavClient(config)

        _syncState.value = SyncState.Syncing(0, 0, "准备同步")
        val startTime = System.currentTimeMillis()
        val stats = SyncStats()

        return try {
            val root = ensureRootDirectory()
            // 清掉旧版本同步进来的回收站记录
            fileDao.deleteRecycleBin()
            syncDirectory(
                client = client,
                path = "/",
                parentId = root.id,
                visited = mutableSetOf(),
                stats = stats
            )

            val syncTime = System.currentTimeMillis()
            webDavConfigDao.updateLastSyncTime(config.id, syncTime)
            _lastSyncTime.value = syncTime

            val duration = System.currentTimeMillis() - startTime
            _syncState.value = SyncState.Completed(stats.entryCount, duration, stats.failedCount)
            Log.d(
                "SyncService",
                "Sync completed: ${stats.entryCount} entries in ${duration}ms, failed=${stats.failedCount}"
            )

            Result.success(stats.entryCount)
        } catch (e: Exception) {
            Log.e("SyncService", "Sync failed", e)
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            Result.failure(e)
        }
    }

    private suspend fun ensureRootDirectory(): FileEntity {
        fileDao.getByPath("/")?.let { return it }

        fileDao.insert(
            FileEntity(
                path = "/",
                name = "/",
                isDirectory = true,
                size = 0,
                modifiedTime = 0
            )
        )
        return fileDao.getByPath("/") ?: throw IllegalStateException("创建根目录记录失败")
    }

    /**
     * 扫描并立即入库单层目录：
     * 1. 本层条目保存完成后再递归子目录，任何一层失败都不影响已写入的数据；
     * 2. 单个目录 PROPFIND 失败时跳过该目录，继续扫描其它目录；
     * 3. 单个条目写库失败时记入失败数，不中断本层其它条目。
     */
    private suspend fun syncDirectory(
        client: WebDavClient,
        path: String,
        parentId: Long,
        visited: MutableSet<String>,
        stats: SyncStats
    ) {
        // 同一目录只扫一次，挡住软链与自引用造成的环
        if (!visited.add(path)) return

        _syncState.value = SyncState.Syncing(stats.entryCount, stats.entryCount, path)

        val items = try {
            client.list(path)
        } catch (e: Exception) {
            // 根目录都读不到时没有任何数据可以保存，按整体失败处理
            if (path == "/") throw e
            stats.failedCount++
            Log.e("SyncService", "跳过目录 $path：${e.message}", e)
            return
        }
        Log.d("SyncService", "syncDirectory($path) returned ${items.size} items")

        // 本层已有记录一次查出，避免逐个条目查询数据库
        val localChildren = fileDao.getChildren(parentId).associateBy { it.path }
        val subDirectories = mutableListOf<Pair<String, Long>>()
        val newFiles = mutableListOf<FileEntity>()

        for (item in items) {
            // Depth:1 的 PROPFIND 响应第一项就是集合自身，跳过它否则会原地无限递归
            if (item.path == path) continue
            // 服务器回收站不进库，也不下探
            if (item.name.equals(RECYCLE_DIR_NAME, ignoreCase = true)) continue

            try {
                val existing = localChildren[item.path] ?: fileDao.getByPath(item.path)
                if (existing == null) {
                    if (item.isDirectory) {
                        // 目录必须立刻拿到 id，才能继续写入它的子目录
                        val newId = fileDao.insert(item.toEntity(parentId))
                        subDirectories.add(item.path to newId)
                    } else {
                        newFiles.add(item.toEntity(parentId))
                    }
                } else {
                    if (item.isDirectory) {
                        subDirectories.add(item.path to existing.id)
                    }
                    val changed = existing.size != item.size ||
                        existing.modifiedTime != item.modifiedTime ||
                        existing.mimeType != item.mimeType ||
                        existing.parentId != parentId
                    if (changed) {
                        fileDao.update(
                            existing.copy(
                                size = item.size,
                                modifiedTime = item.modifiedTime,
                                mimeType = item.mimeType,
                                parentId = parentId
                            )
                        )
                    }
                }
                stats.entryCount++
            } catch (e: Exception) {
                stats.failedCount++
                Log.e("SyncService", "跳过条目 ${item.path}：${e.message}", e)
            }
        }

        // 本层普通文件集中写入；批量失败时降级为逐条写入，避免一条坏数据拖垮整层
        if (newFiles.isNotEmpty()) {
            try {
                fileDao.insertAll(newFiles)
                stats.entryCount += newFiles.size
            } catch (e: Exception) {
                Log.w("SyncService", "批量写入 $path 失败，改为逐条写入", e)
                for (file in newFiles) {
                    try {
                        fileDao.insert(file)
                        stats.entryCount++
                    } catch (itemError: Exception) {
                        stats.failedCount++
                        Log.e("SyncService", "跳过文件 ${file.path}：${itemError.message}", itemError)
                    }
                }
            }
        }

        // 本层已经保存，再进入下一层；单目录异常只影响它自己的子树
        for ((dirPath, dirId) in subDirectories) {
            try {
                syncDirectory(client, dirPath, dirId, visited, stats)
            } catch (e: Exception) {
                stats.failedCount++
                Log.e("SyncService", "跳过目录 $dirPath：${e.message}", e)
            }
        }
    }

    private class SyncStats {
        var entryCount = 0
        var failedCount = 0
    }

    private companion object {
        const val DEFAULT_SYNC_INTERVAL_MS = 60 * 60 * 1000L
        const val RECYCLE_DIR_NAME = "#recycle"
    }

    private fun FileInfo.toEntity(parentId: Long? = null): FileEntity {
        return FileEntity(
            id = 0,
            path = path,
            name = name,
            isDirectory = isDirectory,
            size = size,
            modifiedTime = modifiedTime,
            parentId = parentId,
            mimeType = mimeType
        )
    }
}
