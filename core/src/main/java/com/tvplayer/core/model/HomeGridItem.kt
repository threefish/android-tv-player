package com.tvplayer.core.model

data class HomeGridItem(
    val id: Long = 0,
    val folderPath: String,
    val displayName: String,
    val sortOrder: Int = 0,
    val createTime: Long = System.currentTimeMillis()
)
