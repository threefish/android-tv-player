package com.tvplayer.core.repository.impl

import com.tvplayer.core.db.FileDao
import com.tvplayer.core.db.FileEntity
import com.tvplayer.core.db.SeriesPlaylistDao
import com.tvplayer.core.db.SeriesPlaylistEntity
import com.tvplayer.core.model.FileInfo
import com.tvplayer.core.repository.SeriesPlaylistRepository
import com.tvplayer.core.util.SmartFileNameComparator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeriesPlaylistRepositoryImpl @Inject constructor(
    private val seriesPlaylistDao: SeriesPlaylistDao,
    private val fileDao: FileDao
) : SeriesPlaylistRepository {

    override suspend fun getOrBuild(folderPath: String): List<FileInfo> {
        val folder = folderPath.trimEnd('/')
        if (seriesPlaylistDao.count(folder) > 0) {
            return seriesPlaylistDao.getFiles(folder).map { it.toFileInfo() }
        }
        val files = fileDao.getDescendantFiles(folder)
            .map { it.toFileInfo() }
            .filter { it.isVideo || it.isAudio }
            .sortedWith(seriesPathComparator)
        seriesPlaylistDao.insertAll(files.mapIndexed { index, file ->
            SeriesPlaylistEntity(folderPath = folder, sortIndex = index, filePath = file.path)
        })
        return files
    }

    override suspend fun invalidate(folderPath: String) {
        seriesPlaylistDao.deleteFolder(folderPath.trimEnd('/'))
    }

    override suspend fun invalidateAll() {
        seriesPlaylistDao.clearAll()
    }

    /** 按路径分段自然排序：先比目录层级，同目录内按集数智能排序 */
    private val seriesPathComparator = Comparator<FileInfo> { a, b ->
        val left = a.path.split("/")
        val right = b.path.split("/")
        var result = 0
        for (i in 0 until minOf(left.size, right.size)) {
            result = SmartFileNameComparator.compareNames(left[i], right[i])
            if (result != 0) break
        }
        if (result != 0) result else left.size.compareTo(right.size)
    }

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