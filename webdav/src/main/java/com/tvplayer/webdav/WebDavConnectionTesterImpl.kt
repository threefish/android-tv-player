package com.tvplayer.webdav

import android.util.Log
import com.tvplayer.core.model.WebDavConfig
import com.tvplayer.core.repository.WebDavConnectionTester
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavConnectionTesterImpl @Inject constructor() : WebDavConnectionTester {
    
    override suspend fun testConnection(config: WebDavConfig): Boolean {
        return try {
            Log.d("WebDavTester", "Testing connection to: ${config.url}")
            val client = WebDavClient(config)
            val result = client.testConnection()
            Log.d("WebDavTester", "Connection test result: $result")
            result
        } catch (e: Exception) {
            Log.e("WebDavTester", "Connection test failed", e)
            false
        }
    }
}
