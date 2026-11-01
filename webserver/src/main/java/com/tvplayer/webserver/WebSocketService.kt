package com.tvplayer.webserver

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class WebSocketEvent(
    val type: String,
    val data: JsonElement = JsonNull
)

@Singleton
class WebSocketService @Inject constructor() {

    private val connections = ConcurrentHashMap<String, DefaultWebSocketServerSession>()
    private val json = Json { ignoreUnknownKeys = true }

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 0)
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    fun addConnection(sessionId: String, session: DefaultWebSocketServerSession) {
        connections[sessionId] = session
    }

    fun removeConnection(sessionId: String) {
        connections.remove(sessionId)
    }

    suspend fun broadcastEvent(event: WebSocketEvent) {
        val message = json.encodeToString(event)
        connections.values.forEach { session ->
            try {
                session.send(Frame.Text(message))
            } catch (e: Exception) {
                // Connection might be closed, will be cleaned up
            }
        }
    }

    suspend fun sendToSession(sessionId: String, event: WebSocketEvent) {
        val session = connections[sessionId] ?: return
        val message = json.encodeToString(event)
        try {
            session.send(Frame.Text(message))
        } catch (e: Exception) {
            removeConnection(sessionId)
        }
    }

    suspend fun emitSyncProgress(current: Int, total: Int, path: String) {
        _events.emit(WebSocketEvent(
            type = "sync_progress",
            data = buildJsonObject {
                put("current", current)
                put("total", total)
                put("path", path)
            }
        ))
    }

    suspend fun emitSyncCompleted(fileCount: Int, duration: Long) {
        _events.emit(WebSocketEvent(
            type = "sync_completed",
            data = buildJsonObject {
                put("fileCount", fileCount)
                put("duration", duration)
            }
        ))
    }

    suspend fun emitSyncError(message: String) {
        _events.emit(WebSocketEvent(
            type = "sync_error",
            data = buildJsonObject {
                put("message", message)
            }
        ))
    }

    suspend fun emitChatResponse(sessionId: String, content: String) {
        _events.emit(WebSocketEvent(
            type = "chat_response",
            data = buildJsonObject {
                put("sessionId", sessionId)
                put("content", content)
            }
        ))
    }

    suspend fun emitChatToolCall(sessionId: String, toolName: String, arguments: Map<String, Any?>) {
        _events.emit(WebSocketEvent(
            type = "chat_tool_call",
            data = buildJsonObject {
                put("sessionId", sessionId)
                put("toolName", toolName)
                put("arguments", buildJsonObject {
                    arguments.forEach { (key, value) ->
                        when (value) {
                            is String -> put(key, value)
                            is Number -> put(key, value)
                            is Boolean -> put(key, value)
                            else -> put(key, JsonNull)
                        }
                    }
                })
            }
        ))
    }

    suspend fun emitChatToolResult(sessionId: String, toolName: String, result: String) {
        _events.emit(WebSocketEvent(
            type = "chat_tool_result",
            data = buildJsonObject {
                put("sessionId", sessionId)
                put("toolName", toolName)
                put("result", result)
            }
        ))
    }

    suspend fun emitFileChanged(path: String, action: String) {
        _events.emit(WebSocketEvent(
            type = "file_changed",
            data = buildJsonObject {
                put("path", path)
                put("action", action)
            }
        ))
    }

    fun getConnectionCount(): Int = connections.size
}
