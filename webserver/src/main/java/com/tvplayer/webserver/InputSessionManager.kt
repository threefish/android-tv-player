package com.tvplayer.webserver

import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class InputSession(
    val token: String,
    val createTime: Long = System.currentTimeMillis(),
    var result: String? = null
)

@Singleton
class InputSessionManager @Inject constructor() {

    private val sessions = ConcurrentHashMap<String, InputSession>()
    
    // Session TTL: 10 minutes
    private val SESSION_TTL = 10 * 60 * 1000L

    fun createSession(): InputSession {
        cleanupExpiredSessions()
        val token = UUID.randomUUID().toString()
        val session = InputSession(token = token)
        sessions[token] = session
        return session
    }

    fun getSession(token: String): InputSession? {
        val session = sessions[token]
        if (session != null && System.currentTimeMillis() - session.createTime > SESSION_TTL) {
            sessions.remove(token)
            return null
        }
        return session
    }

    fun setResult(token: String, result: String) {
        sessions[token]?.result = result
    }

    fun removeSession(token: String) {
        sessions.remove(token)
    }

    private fun cleanupExpiredSessions() {
        val now = System.currentTimeMillis()
        sessions.entries.removeIf { (token, session) ->
            now - session.createTime > SESSION_TTL
        }
    }
}
