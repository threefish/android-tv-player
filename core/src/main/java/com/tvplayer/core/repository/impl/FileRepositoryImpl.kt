package com.tvplayer.core.repository.impl

import com.tvplayer.core.db.FileDao
import com.tvplayer.core.db.FileEntity
import com.tvplayer.core.model.FileInfo
import com.tvplayer.core.repository.FileRepository
import com.tvplayer.core.util.SmartFileNameComparator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val fileDao: FileDao
) : FileRepository {

    override suspend fun getByPath(path: String): FileInfo? {
        return fileDao.getByPath(path)?.toFileInfo()
    }

    override suspend fun getChildren(parentPath: String): List<FileInfo> {
        val parent = fileDao.getByPath(parentPath) ?: return emptyList()
        return fileDao.getChildren(parent.id).toPlayableFiles()
    }

    override fun getChildrenFlow(parentPath: String): Flow<List<FileInfo>> {
        return kotlinx.coroutines.flow.flow {
            val parent = fileDao.getByPath(parentPath)
            if (parent != null) {
                emitAll(fileDao.getChildrenFlow(parent.id).map { list -> list.toPlayableFiles() })
            } else {
                emit(emptyList())
            }
        }
    }

    override suspend fun search(keyword: String, fileType: String): List<FileInfo> {
        val results = when (fileType) {
            "video" -> fileDao.searchByType(keyword, "video/%")
            "audio" -> fileDao.searchByType(keyword, "audio/%")
            else -> fileDao.searchByName(keyword)
        }
        return results.map { it.toFileInfo() }
    }

    override suspend fun markAsDeleted(path: String) {
        fileDao.markAsDeleted(path)
    }

    override suspend fun restore(path: String) {
        fileDao.restore(path)
    }

    override suspend fun getTrash(): List<FileInfo> {
        return fileDao.getTrash().map { it.toFileInfo() }
    }

    override fun getTrashFlow(): Flow<List<FileInfo>> {
        return fileDao.getTrashFlow().map { list -> list.map { it.toFileInfo() } }
    }

    override suspend fun rename(oldPath: String, newName: String) {
        val parentPath = oldPath.substringBeforeLast("/", "/")
        val newPath = if (parentPath == "/") "/$newName" else "$parentPath/$newName"
        fileDao.rename(oldPath, newPath, newName)
    }

    override suspend fun move(oldPath: String, targetDir: String) {
        val fileName = oldPath.substringAfterLast("/")
        val newPath = if (targetDir == "/") "/$fileName" else "$targetDir/$fileName"
        val targetParent = fileDao.getByPath(targetDir)
        fileDao.move(oldPath, newPath, targetParent?.id)
    }

    override suspend fun getRecentFiles(limit: Int): List<FileInfo> {
        return fileDao.getRecentFiles(limit).map { it.toFileInfo() }
    }

    override suspend fun getImages(): List<FileInfo> {
        return fileDao.getAllImages()
            .map { it.toFileInfo() }
            .filter { it.isImage }
    }

    override suspend fun getFilesByExtensions(parentPath: String, extensions: Set<String>): List<FileInfo> {
        val parent = fileDao.getByPath(parentPath) ?: return emptyList()
        return fileDao.getChildren(parent.id)
            .asSequence()
            .filter { !it.isDirectory }
            .filter { it.name.substringAfterLast('.', "").lowercase() in extensions }
            .map { it.toFileInfo() }
            .toList()
    }

    /** 文件浏览展示目录、视频与音频，并统一使用智能排序 */
    private fun List<FileEntity>.toPlayableFiles(): List<FileInfo> =
        map { it.toFileInfo() }
            .filter { it.isDirectory || it.isVideo || it.isAudio }
            .sortedWith(SmartFileNameComparator)

    private fun FileEntity.toFileInfo() = FileInfo(
        id = id,
        path = path,
        name = name,
        isDirectory = isDirectory,
        size = size,
        modifiedTime = modifiedTime,
        parentId = parentId,
        isDeleted = isDeleted,
        deletedTime = deletedTime,
        mimeType = mimeType,
        duration = duration,
        width = width,
        height = height
    )
}
