package com.tvplayer.core.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val trackPaths: List<String> = emptyList(),
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
)
