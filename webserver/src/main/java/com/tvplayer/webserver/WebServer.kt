package com.tvplayer.webserver

import com.tvplayer.ai.chat.ChatService
import com.tvplayer.core.repository.AiProviderRepository
import com.tvplayer.core.repository.FileRepository
import com.tvplayer.core.repository.PlayHistoryRepository
import com.tvplayer.core.repository.WebDavConfigRepository
import com.tvplayer.webdav.SyncService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.websocket.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.websocket.*
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebServer @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val fileRepository: FileRepository,
    private val playHistoryRepository: PlayHistoryRepository,
    private val webDavConfigRepository: WebDavConfigRepository,
    private val syncService: SyncService,
    private val chatService: ChatService,
    private val aiProviderRepository: AiProviderRepository,
    private val webSocketService: WebSocketService,
    private val inputSessionManager: InputSessionManager
) {
    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: ApplicationEngine? = null
    private val appContext = context

    // 启动时端口可能被占用，实际监听端口以这里为准
    private val _activePort = MutableStateFlow(DEFAULT_PORT)
    val activePort: StateFlow<Int> = _activePort.asStateFlow()

    companion object {
        private const val TAG = "WebServer"
        private const val DEFAULT_PORT = 8080
        private const val MAX_PORT_ATTEMPTS = 100
    }

    fun start(port: Int = DEFAULT_PORT) {
        serverScope.launch {
            var candidate = port
            repeat(MAX_PORT_ATTEMPTS) {
                if (isPortAvailable(candidate)) {
                    try {
                        server = embeddedServer(Netty, port = candidate) {
                            configureServer()
                        }.start(wait = false)
                        _activePort.value = candidate
                        Log.i(TAG, "WebServer started on port $candidate")
                        startEventCollector()
                        return@launch
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start WebServer on port $candidate", e)
                    }
                } else {
                    Log.w(TAG, "Port $candidate is in use, trying ${candidate + 1}")
                }
                candidate++
            }
            Log.e(TAG, "No available port in range $port..${candidate - 1}")
        }
    }

    private fun isPortAvailable(port: Int): Boolean = try {
        ServerSocket().use { socket ->
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(port))
        }
        true
    } catch (e: Exception) {
        false
    }

    private fun startEventCollector() {
        serverScope.launch {
            webSocketService.events.collect { event ->
                webSocketService.broadcastEvent(event)
            }
        }
    }

    fun stop() {
        server?.stop(1000, 2000)
    }

    private fun Application.configureServer() {
        install(ContentNegotiation) {
            json()
        }
        
        install(WebSockets) {
            pingPeriod = java.time.Duration.ofSeconds(15)
            timeout = java.time.Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
        
        install(CORS) {
            anyHost()
        }

        routing {
            get("/") {
                try {
                    val inputStream = appContext.assets.open("web/index.html")
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    call.respondBytes(bytes, ContentType.Text.Html)
                } catch (e: Exception) {
                    call.respondText("TV Player Web Server - Frontend not available")
                }
            }
            
            // Serve static files from assets directory
            get("/assets/{fileName}") {
                val fileName = call.parameters["fileName"] ?: ""
                try {
                    val inputStream = appContext.assets.open("web/assets/$fileName")
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    
                    val contentType = when {
                        fileName.endsWith(".js") -> ContentType.Application.JavaScript
                        fileName.endsWith(".css") -> ContentType.Text.CSS
                        fileName.endsWith(".json") -> ContentType.Application.Json
                        fileName.endsWith(".png") -> ContentType.Image.PNG
                        fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> ContentType.Image.JPEG
                        fileName.endsWith(".svg") -> ContentType.Image.SVG
                        else -> ContentType.Application.OctetStream
                    }
                    
                    call.respondBytes(bytes, contentType)
                } catch (e: Exception) {
                    call.respondText("File not found: $fileName", status = HttpStatusCode.NotFound)
                }
            }
            
            // WebSocket endpoint
            webSocket("/ws") {
                val sessionId = UUID.randomUUID().toString()
                webSocketService.addConnection(sessionId, this)
                
                try {
                    // Send welcome message
                    send(Frame.Text("""{"type":"connected","data":{"sessionId":"$sessionId"}}"""))
                    
                    // Listen for incoming messages (if needed)
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                // Handle incoming messages if needed
                            }
                            is Frame.Binary -> {
                                // Handle binary messages if needed
                            }
                            else -> {}
                        }
                    }
                } catch (e: Exception) {
                    // Connection closed
                } finally {
                    webSocketService.removeConnection(sessionId)
                }
            }
            
            // API routes (must be before SPA fallback)
            configureApiRoutes(fileRepository, playHistoryRepository, webDavConfigRepository, syncService, chatService, aiProviderRepository, webSocketService, inputSessionManager, serverPort = { _activePort.value })
            
            // SPA fallback - serve index.html for all other routes
            get("/{path...}") {
                try {
                    val inputStream = appContext.assets.open("web/index.html")
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    call.respondBytes(bytes, ContentType.Text.Html)
                } catch (e: Exception) {
                    call.respondText("TV Player Web Server", status = HttpStatusCode.OK)
                }
            }
        }
    }
}
