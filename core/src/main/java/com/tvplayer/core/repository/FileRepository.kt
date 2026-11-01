package com.tvplayer.core.repository

import com.tvplayer.core.model.FileInfo
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    suspend fun getByPath(path: String): FileInfo?
    suspend fun getChildren(parentPath: String): List<FileInfo>
    fun getChildrenFlow(parentPath: String): Flow<List<FileInfo>>
    suspend fun search(keyword: String, fileType: String = "all"): List<FileInfo>
    suspend fun markAsDeleted(path: String)
    suspend fun restore(path: String)
    suspend fun getTrash(): List<FileInfo>
    fun getTrashFlow(): Flow<List<FileInfo>>
    suspend fun rename(oldPath: String, newName: String)
    suspend fun move(oldPath: String, targetDir: String)
    suspend fun getRecentFiles(limit: Int): List<FileInfo>
    suspend fun getImages(): List<FileInfo>

    /** 目录下指定扩展名的普通文件，用于字幕等非媒体文件扫描 */
    suspend fun getFilesByExtensions(parentPath: String, extensions: Set<String>): List<FileInfo>
}
