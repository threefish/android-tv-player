package com.tvplayer.webserver

import com.tvplayer.ai.chat.ChatService
import com.tvplayer.core.model.AiProviderConfig
import com.tvplayer.core.model.PlayHistory
import com.tvplayer.core.model.PlayReminder
import com.tvplayer.core.model.WebDavConfig
import com.tvplayer.core.repository.AiProviderRepository
import com.tvplayer.core.repository.AppSettingsRepository
import com.tvplayer.core.repository.FileRepository
import com.tvplayer.core.repository.PlayHistoryRepository
import com.tvplayer.core.repository.WebDavConfigRepository
import com.tvplayer.webdav.SyncService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.net.Inet4Address
import java.net.NetworkInterface

fun Routing.configureApiRoutes(
    fileRepository: FileRepository,
    playHistoryRepository: PlayHistoryRepository,
    webDavConfigRepository: WebDavConfigRepository,
    syncService: SyncService,
    chatService: ChatService,
    aiProviderRepository: com.tvplayer.core.repository.AiProviderRepository,
    appSettingsRepository: AppSettingsRepository,
    webSocketService: WebSocketService,
    inputSessionManager: InputSessionManager,
    serverPort: () -> Int
) {
    route("/api") {
        get("/files") {
            val path = call.request.queryParameters["path"] ?: "/"
            val files = fileRepository.getChildren(path)
            call.respond(files.map { it.toDto() })
        }
        
        get("/files/search") {
            val keyword = call.request.queryParameters["keyword"] ?: ""
            val fileType = call.request.queryParameters["fileType"] ?: "all"
            val files = fileRepository.search(keyword, fileType)
            call.respond(files.map { it.toDto() })
        }

        post("/files/{id}/rename") {
            val path = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val body = call.receive<RenameRequest>()
            fileRepository.rename(path, body.newName)
            call.respond(SuccessResponse(success = true))
        }

        post("/files/{id}/move") {
            val path = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val body = call.receive<MoveRequest>()
            fileRepository.move(path, body.targetDir)
            call.respond(SuccessResponse(success = true))
        }

        post("/files/{id}/delete") {
            val path = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            fileRepository.markAsDeleted(path)
            call.respond(SuccessResponse(success = true))
        }

        post("/files/{id}/restore") {
            val path = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            fileRepository.restore(path)
            call.respond(SuccessResponse(success = true))
        }

        get("/trash") {
            val files = fileRepository.getTrash()
            call.respond(files.map { it.toDto() })
        }

        get("/history") {
            val history = playHistoryRepository.getAll()
            call.respond(history.map { it.toDto() })
        }

        get("/history/recent") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val history = playHistoryRepository.getRecent(limit)
            call.respond(history.map { it.toDto() })
        }

        // WebDAV Config
        get("/config/webdav") {
            val config = webDavConfigRepository.getActiveConfig()
            if (config != null) {
                call.respond(config.toDto())
            } else {
                call.respond(HttpStatusCode.NotFound, "No WebDAV config found")
            }
        }

        put("/config/webdav") {
            val body = call.receive<WebDavConfigDto>()
            val config = body.toWebDavConfig()
            val id = webDavConfigRepository.save(config)
            call.respond(IdResponse(id = id))
        }

        get("/config/webdav/list") {
            call.respond(webDavConfigRepository.getAll().map { it.toDto() })
        }

        post("/config/webdav") {
            val body = call.receive<WebDavConfigDto>()
            val id = webDavConfigRepository.save(body.copy(id = 0).toWebDavConfig())
            call.respond(IdResponse(id = id))
        }

        put("/config/webdav/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid config id")
            val body = call.receive<WebDavConfigDto>()
            val savedId = webDavConfigRepository.save(body.copy(id = id).toWebDavConfig())
            call.respond(IdResponse(id = savedId))
        }

        post("/config/webdav/{id}/activate") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid config id")
            webDavConfigRepository.setActive(id)
            call.respond(SuccessResponse(success = true))
        }

        delete("/config/webdav/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid config id")
            webDavConfigRepository.delete(id)
            call.respond(SuccessResponse(success = true))
        }

        post("/config/webdav/test") {
            val body = call.receive<WebDavConfigDto>()
            val config = body.toWebDavConfig()
            val success = webDavConfigRepository.testConnection(config)
            call.respond(SuccessResponse(success = success))
        }

        // Sync
        get("/sync/status") {
            val status = when (val state = syncService.syncState.value) {
                is SyncService.SyncState.Idle -> SyncStatusResponse(status = "idle")
                is SyncService.SyncState.Syncing -> SyncStatusResponse(
                    status = "syncing",
                    current = state.current,
                    total = state.total,
                    path = state.path
                )
                is SyncService.SyncState.Completed -> SyncStatusResponse(
                    status = "completed",
                    fileCount = state.fileCount,
                    duration = state.duration,
                    failedCount = state.failedCount
                )
                is SyncService.SyncState.Error -> SyncStatusResponse(
                    status = "error",
                    message = state.message
                )
            }
            call.respond(status)
        }

        post("/sync/trigger") {
            kotlinx.coroutines.GlobalScope.launch {
                syncService.fullSync()
            }
            call.respond(SuccessResponse(success = true, message = "Sync triggered"))
        }

        // File streaming (for subtitles, etc.)
        get("/files/stream") {
            val path = call.request.queryParameters["path"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            // TODO: Implement file streaming from WebDAV
            call.respond(HttpStatusCode.NotImplemented, "File streaming not implemented")
        }

        // Chat
        get("/chat/sessions") {
            val sessions = chatService.getSessions()
            call.respond(sessions.map { 
                ChatSessionDto(
                    id = it.id,
                    sessionId = it.sessionId,
                    title = it.title,
                    createTime = it.createTime,
                    updateTime = it.updateTime
                )
            })
        }

        get("/chat/sessions/{sessionId}/messages") {
            val sessionId = call.parameters["sessionId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val messages = chatService.getSessionMessages(sessionId)
            call.respond(messages.map {
                ChatMessageDto(
                    id = it.id,
                    role = it.role,
                    content = it.content,
                    createTime = it.createTime
                )
            })
        }

        post("/chat/send") {
            val body = call.receive<ChatSendRequest>()
            val config = AiProviderConfig(
                id = 0,
                name = "Default",
                baseUrl = body.baseUrl,
                apiKey = body.apiKey,
                model = body.model
            )
            
            var sessionId = body.sessionId
            if (sessionId.isBlank()) {
                val session = chatService.createSession()
                sessionId = session.sessionId
            }
            
            val events = mutableListOf<ChatEventDto>()
            try {
                chatService.sendMessage(config, sessionId, body.message).collect { event ->
                    when (event) {
                        is com.tvplayer.ai.chat.ChatEvent.Response -> {
                            events.add(ChatEventDto(type = "response", content = event.content))
                        }
                        is com.tvplayer.ai.chat.ChatEvent.Error -> {
                            events.add(ChatEventDto(type = "error", message = event.message))
                        }
                        is com.tvplayer.ai.chat.ChatEvent.Done -> {
                            events.add(ChatEventDto(type = "done"))
                        }
                        else -> {}
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 客户端断开导致的取消需要继续向上传播
                throw e
            } catch (t: Throwable) {
                // 这里必须兜住 Throwable：历史问题里 NoSuchMethodError 逃逸后
                // Ktor 只能返回空体 500，完全看不到原因
                android.util.Log.e("ChatApi", "chat/send failed", t)
                events.add(ChatEventDto(
                    type = "error",
                    message = "${t.javaClass.simpleName}: ${t.message ?: "Unknown error"}"
                ))
            }
            call.respond(ChatEventsResponse(sessionId = sessionId, events = events))
        }

        // AI Provider Config
        get("/config/ai-providers") {
            val providers = aiProviderRepository.getAll()
            call.respond(providers.map { it.toDto() })
        }

        get("/config/ai-providers/active") {
            val provider = aiProviderRepository.getActive()
            if (provider != null) {
                call.respond(provider.toDto())
            } else {
                call.respond(HttpStatusCode.NotFound, "No active AI provider found")
            }
        }

        post("/config/ai-providers") {
            val body = call.receive<AiProviderConfigDto>()
            val config = body.toAiProviderConfig()
            val id = aiProviderRepository.save(config)
            call.respond(IdResponse(id = id))
        }

        put("/config/ai-providers/{id}") {
            val id = call.parameters["id"]?.toLongOrNull() 
                ?: return@put call.respond(HttpStatusCode.BadRequest)
            val body = call.receive<AiProviderConfigDto>()
            val config = body.toAiProviderConfig().copy(id = id)
            aiProviderRepository.update(config)
            call.respond(SuccessResponse(success = true))
        }

        delete("/config/ai-providers/{id}") {
            val id = call.parameters["id"]?.toLongOrNull() 
                ?: return@delete call.respond(HttpStatusCode.BadRequest)
            aiProviderRepository.delete(id)
            call.respond(SuccessResponse(success = true))
        }

        post("/config/ai-providers/{id}/activate") {
            val id = call.parameters["id"]?.toLongOrNull() 
                ?: return@post call.respond(HttpStatusCode.BadRequest)
            aiProviderRepository.setActive(id)
            call.respond(SuccessResponse(success = true))
        }

        // 播放提醒：播放达到设定分钟数后自动暂停休息
        get("/config/play-reminder") {
            val reminder = appSettingsRepository.getPlayReminder()
            call.respond(PlayReminderDto(enabled = reminder.enabled, minutes = reminder.minutes))
        }

        put("/config/play-reminder") {
            val body = call.receive<PlayReminderDto>()
            if (body.minutes !in 1..240) {
                return@put call.respond(HttpStatusCode.BadRequest, "minutes must be 1..240")
            }
            appSettingsRepository.setPlayReminder(PlayReminder(enabled = body.enabled, minutes = body.minutes))
            call.respond(SuccessResponse(success = true))
        }

        // QR Code Input
        post("/input/create") {
            val session = inputSessionManager.createSession()
            call.respond(InputCreateResponse(
                token = session.token,
                url = "http://${localIpAddress()}:${serverPort()}/input/${session.token}"
            ))
        }

        get("/input/{token}") {
            val token = call.parameters["token"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val session = inputSessionManager.getSession(token)
            if (session == null) {
                call.respond(HttpStatusCode.NotFound, "Session expired or not found")
            } else {
                call.respond(InputStatusResponse(
                    token = session.token,
                    createTime = session.createTime,
                    hasResult = (session.result != null)
                ))
            }
        }

        post("/input/{token}/submit") {
            val token = call.parameters["token"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val body = call.receive<Map<String, String>>()
            val input = body["input"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            
            val session = inputSessionManager.getSession(token)
            if (session == null) {
                call.respond(HttpStatusCode.NotFound, "Session expired or not found")
            } else {
                inputSessionManager.setResult(token, input)
                webSocketService.emitChatResponse("input_$token", input)
                call.respond(SuccessResponse(success = true))
            }
        }

        get("/input/{token}/result") {
            val token = call.parameters["token"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val session = inputSessionManager.getSession(token)
            if (session == null) {
                call.respond(HttpStatusCode.NotFound, "Session expired or not found")
            } else if (session.result != null) {
                call.respond(InputResultResponse(hasResult = true, result = session.result!!))
                inputSessionManager.removeSession(token)
            } else {
                call.respond(InputResultResponse(hasResult = false))
            }
        }
    }
}

// InetAddress.getLocalHost() 在 Android 上返回 127.0.0.1，扫码地址必须用局域网 IP
private fun localIpAddress(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
        for (iface in interfaces) {
            if (iface.isLoopback || !iface.isUp) continue
            for (addr in iface.inetAddresses) {
                if (addr is Inet4Address && !addr.isLoopbackAddress) {
                    return addr.hostAddress ?: "127.0.0.1"
                }
            }
        }
    } catch (_: Exception) {}
    return "127.0.0.1"
}

@Serializable
data class SuccessResponse(val success: Boolean, val message: String = "")

@Serializable
data class IdResponse(val id: Long, val success: Boolean = true)

@Serializable
data class FileInfoDto(
    val id: Long,
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val modifiedTime: Long
)

@Serializable
data class PlayHistoryDto(
    val id: Long,
    val filePath: String,
    val fileName: String,
    val position: Long,
    val duration: Long,
    val lastPlayedTime: Long
)

@Serializable
data class SyncStatusResponse(
    val status: String,
    val current: Int = 0,
    val total: Int = 0,
    val path: String = "",
    val fileCount: Int = 0,
    val duration: Long = 0,
    val failedCount: Int = 0,
    val message: String = ""
)

@Serializable
data class ChatSessionDto(
    val id: Long,
    val sessionId: String,
    val title: String,
    val createTime: Long,
    val updateTime: Long
)

@Serializable
data class ChatMessageDto(
    val id: Long,
    val role: String,
    val content: String,
    val createTime: Long
)

@Serializable
data class ChatSendRequest(
    val sessionId: String,
    val message: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String
)

@Serializable
data class ChatEventDto(val type: String, val content: String = "", val message: String = "")

@Serializable
data class ChatEventsResponse(val sessionId: String, val events: List<ChatEventDto>)

@Serializable
data class RenameRequest(val newName: String)

@Serializable
data class MoveRequest(val targetDir: String)

@Serializable
data class WebDavConfigDto(
    val id: Long = 0,
    val name: String = "",
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val rootPath: String = "/",
    val isEnabled: Boolean = true
)

@Serializable
data class AiProviderConfigDto(
    val id: Long = 0,
    val name: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val isActive: Boolean = false
)

@Serializable
data class PlayReminderDto(val enabled: Boolean = false, val minutes: Int = 15)

@Serializable
data class InputCreateResponse(val token: String, val url: String)

@Serializable
data class InputStatusResponse(val token: String, val createTime: Long, val hasResult: Boolean)

@Serializable
data class InputResultResponse(val hasResult: Boolean, val result: String = "")

private fun WebDavConfig.toDto() = WebDavConfigDto(
    id = id,
    name = name,
    url = url,
    username = username,
    password = password,
    rootPath = rootPath,
    isEnabled = isEnabled
)

private fun WebDavConfigDto.toWebDavConfig() = WebDavConfig(
    id = id,
    name = name,
    url = url,
    username = username,
    password = password,
    rootPath = rootPath,
    isEnabled = isEnabled
)

private fun AiProviderConfig.toDto() = AiProviderConfigDto(
    id = id,
    name = name,
    baseUrl = baseUrl,
    apiKey = apiKey,
    model = model,
    isActive = isActive
)

private fun AiProviderConfigDto.toAiProviderConfig() = AiProviderConfig(
    id = id,
    name = name,
    baseUrl = baseUrl,
    apiKey = apiKey,
    model = model,
    isActive = isActive
)

private fun com.tvplayer.core.model.FileInfo.toDto() = FileInfoDto(
    id = id,
    path = path,
    name = name,
    isDirectory = isDirectory,
    size = size,
    modifiedTime = modifiedTime
)

private fun PlayHistory.toDto() = PlayHistoryDto(
    id = id,
    filePath = filePath,
    fileName = fileName,
    position = position,
    duration = duration,
    lastPlayedTime = lastPlayedTime
)
