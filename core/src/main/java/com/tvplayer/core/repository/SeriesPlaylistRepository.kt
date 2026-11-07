package com.tvplayer.core.repository

import com.tvplayer.core.model.FileInfo

interface SeriesPlaylistRepository {
    /** 读系列缓存；没有缓存时递归扫描目录（含子目录）构建并写入 SQLite */
    suspend fun getOrBuild(folderPath: String): List<FileInfo>
    suspend fun invalidate(folderPath: String)
    suspend fun invalidateAll()
}