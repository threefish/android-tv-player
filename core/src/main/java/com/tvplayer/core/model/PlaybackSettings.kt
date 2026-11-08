package com.tvplayer.core.model

/** 电视端播放设置：默认倍数、快进/快退步长、字幕字号与双字幕开关 */
data class PlaybackSettings(
    val playbackSpeed: Float = 1.0f,
    val seekStepSeconds: Int = 10,
    val subtitleSize: Float = 1.0f,
    val dualSubtitle: Boolean = false
)
