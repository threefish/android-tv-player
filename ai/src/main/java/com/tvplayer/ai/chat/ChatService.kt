package com.tvplayer.ai.chat

import com.tvplayer.ai.agent.AgentEvent
import com.tvplayer.ai.agent.ReActAgentService
import com.tvplayer.ai.client.OpenAiClient
import com.tvplayer.ai.client.ToolDefinition
import com.tvplayer.core.db.ChatMessageDao
import com.tvplayer.core.db.ChatMessageEntity
import com.tvplayer.core.db.ChatSessionDao
import com.tvplayer.core.db.ChatSessionEntity
import com.tvplayer.core.model.AiProviderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatService @Inject constructor(
    private val chatSessionDao: ChatSessionDao,
    private val chatMessageDao: ChatMessageDao,
    private val toolExecutor: com.tvplayer.ai.agent.ToolExecutor
) {
    private val toolDefinitions = listOf(
        ToolDefinition(
            name = "search_files",
            description = "在 WebDAV 中搜索文件，支持按名称、类型筛选",
            parameters = mapOf(
                "keyword" to mapOf("type" to "string", "description" to "搜索关键词"),
                "path" to mapOf("type" to "string", "description" to "限定搜索目录，可选"),
                "fileType" to mapOf("type" to "string", "enum" to listOf("video", "audio", "all"), "description" to "文件类型")
            ),
            required = listOf("keyword")
        ),
        ToolDefinition(
            name = "list_directory",
            description = "列出指定目录下的文件和子目录",
            parameters = mapOf(
                "path" to mapOf("type" to "string", "description" to "目录路径，默认 /")
            ),
            required = listOf("path")
        ),
        ToolDefinition(
            name = "get_play_history",
            description = "获取最近播放的文件列表",
            parameters = mapOf(
                "limit" to mapOf("type" to "integer", "description" to "返回数量，默认 10")
            )
        ),
        ToolDefinition(
            name = "play_file",
            description = "播放指定文件",
            parameters = mapOf(
                "path" to mapOf("type" to "string", "description" to "文件路径")
            ),
            required = listOf("path")
        ),
        ToolDefinition(
            name = "delete_to_trash",
            description = "将文件移到垃圾箱（软删除）",
            parameters = mapOf(
                "path" to mapOf("type" to "string", "description" to "文件路径")
            ),
            required = listOf("path")
        )
    )

    suspend fun getSessions(): List<ChatSessionEntity> {
        return chatSessionDao.getAll()
    }

    suspend fun getSessionMessages(sessionId: String): List<ChatMessageEntity> {
        return chatMessageDao.getBySessionId(sessionId)
    }

    suspend fun createSession(): ChatSessionEntity {
        val session = ChatSessionEntity(
            sessionId = UUID.randomUUID().toString()
        )
        val id = chatSessionDao.insert(session)
        return session.copy(id = id)
    }

    suspend fun deleteSession(sessionId: String) {
        chatSessionDao.delete(sessionId)
        chatMessageDao.deleteBySessionId(sessionId)
    }

    fun sendMessage(
        config: AiProviderConfig,
        sessionId: String,
        userMessage: String
    ): Flow<ChatEvent> = flow {
        val client = OpenAiClient(config.baseUrl, config.apiKey)
        val agentService = ReActAgentService(toolExecutor)

        val messages = chatMessageDao.getBySessionId(sessionId).map {
            com.tvplayer.ai.client.ChatMessage(
                role = it.role,
                content = it.content,
                toolCallId = it.toolCallId
            )
        }.toMutableList()

        val userMsgEntity = ChatMessageEntity(
            sessionId = sessionId,
            role = "user",
            content = userMessage
        )
        chatMessageDao.insert(userMsgEntity)
        messages.add(com.tvplayer.ai.client.ChatMessage(role = "user", content = userMessage))

        var assistantContent = StringBuilder()

        agentService.execute(client, config.model, messages, toolDefinitions).collect { event ->
            when (event) {
                is AgentEvent.Thinking -> {
                    emit(ChatEvent.Thinking(event.content))
                }
                is AgentEvent.ToolCall -> {
                    emit(ChatEvent.ToolCall(event.id, event.name, event.arguments))
                }
                is AgentEvent.ToolResult -> {
                    emit(ChatEvent.ToolResult(event.id, event.name, event.result))
                }
                is AgentEvent.Response -> {
                    assistantContent.append(event.content)
                    emit(ChatEvent.Response(event.content))
                }
                is AgentEvent.Error -> {
                    emit(ChatEvent.Error(event.message))
                }
                is AgentEvent.Done -> {
                    if (assistantContent.isNotEmpty()) {
                        val assistantMsgEntity = ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = assistantContent.toString()
                        )
                        chatMessageDao.insert(assistantMsgEntity)
                    }
                    chatSessionDao.updateTitle(
                        sessionId = sessionId,
                        title = userMessage.take(20),
                        updateTime = System.currentTimeMillis()
                    )
                    emit(ChatEvent.Done)
                }
            }
        }
    }
}

sealed class ChatEvent {
    data class Thinking(val content: String) : ChatEvent()
    data class ToolCall(val id: String, val name: String, val arguments: String) : ChatEvent()
    data class ToolResult(val id: String, val name: String, val result: String) : ChatEvent()
    data class Response(val content: String) : ChatEvent()
    data class Error(val message: String) : ChatEvent()
    object Done : ChatEvent()
}
