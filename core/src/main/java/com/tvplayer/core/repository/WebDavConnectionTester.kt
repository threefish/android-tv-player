package com.tvplayer.core.repository

import com.tvplayer.core.model.WebDavConfig

interface WebDavConnectionTester {
    suspend fun testConnection(config: WebDavConfig): Boolean
}
