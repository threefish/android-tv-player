package com.tvplayer.core.db

import androidx.room.Entity

/** 首页绑定文件夹的系列播放列表缓存，一个目录（含子孙目录）算一个系列 */
@Entity(
    tableName = "series_playlist",
    primaryKeys = ["folderPath", "sortIndex"]
)
data class SeriesPlaylistEntity(
    val folderPath: String,
    val sortIndex: Int,
    val filePath: String
)