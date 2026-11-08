package com.tvplayer.core.repository.impl

import com.tvplayer.core.db.DailyWatchTimeDao
import com.tvplayer.core.db.DailyWatchTimeEntity
import com.tvplayer.core.repository.WatchTimeRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchTimeRepositoryImpl @Inject constructor(
    private val dailyWatchTimeDao: DailyWatchTimeDao
) : WatchTimeRepository {

    override fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    override suspend fun addSeconds(date: String, seconds: Long) {
        if (seconds <= 0) return
        dailyWatchTimeDao.insertIgnore(DailyWatchTimeEntity(date = date))
        dailyWatchTimeDao.addSeconds(date, seconds)
    }

    override suspend fun getTodaySeconds(): Long = dailyWatchTimeDao.getSeconds(todayKey()) ?: 0L
}