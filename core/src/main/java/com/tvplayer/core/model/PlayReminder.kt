package com.tvplayer.core.model

/** 播放提醒：连续播放 playMinutes 分钟后自动暂停，休息 restMinutes 分钟 */
data class PlayReminder(
    val enabled: Boolean = false,
    val playMinutes: Int = 15,
    val restMinutes: Int = 5,
    /** true 休息倒计时结束自动继续播放，false 需手动按 OK 确认 */
    val autoResume: Boolean = false
)