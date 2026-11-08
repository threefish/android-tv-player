package com.tvplayer.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyWatchTimeDao {

    @Query("SELECT seconds FROM daily_watch_time WHERE date = :date")
    suspend fun getSeconds(date: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entry: DailyWatchTimeEntity)

    // API 23 自带 SQLite 3.9 不支持 UPSERT，先插空行再累加
    @Query("UPDATE daily_watch_time SET seconds = seconds + :delta WHERE date = :date")
    suspend fun addSeconds(date: String, delta: Long)
}