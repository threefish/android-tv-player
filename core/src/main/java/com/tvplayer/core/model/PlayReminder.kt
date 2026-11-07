package com.tvplayer.core.model

/** 播放提醒：连续播放达到指定分钟数后自动暂停休息 */
data class PlayReminder(
    val enabled: Boolean = false,
    val minutes: Int = 15
)