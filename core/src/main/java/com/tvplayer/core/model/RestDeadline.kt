package com.tvplayer.core.model

/** 播放提醒休息截止时刻：到期前不允许继续播放；持久化以防退出应用绕过休息 */
data class RestDeadline(
    val untilMillis: Long,
    val autoResume: Boolean
)