package com.tvplayer.core.model

data class WebDavConfig(
    val id: Long = 0,
    val name: String,
    val url: String,
    val username: String,
    val password: String,
    val rootPath: String = "/",
    val isEnabled: Boolean = true
)
