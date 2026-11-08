package com.tvplayer.core.repository

/** 今日累计观看时长，按本地日期（00:00–23:59:59）统计 */
interface WatchTimeRepository {
    /** 本地日期键 yyyy-MM-dd */
    fun todayKey(): String
    suspend fun addSeconds(date: String, seconds: Long)
    suspend fun getTodaySeconds(): Long
}