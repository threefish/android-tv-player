package com.tvplayer.core.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 按本地日期累计的实际观看秒数，date 为 yyyy-MM-dd */
@Entity(tableName = "daily_watch_time")
data class DailyWatchTimeEntity(
    @PrimaryKey val date: String,
    @ColumnInfo(defaultValue = "0")
    val seconds: Long = 0
)