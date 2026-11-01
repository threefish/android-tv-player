package com.tvplayer.ai.agent

import com.tvplayer.ai.client.ChatMessage
import com.tvplayer.ai.client.OpenAiClient
import com.tvplayer.ai.client.ToolDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReActAgentService @Inject constructor(
    private val toolExecutor: ToolExecutor
) {
    private val systemPrompt = """
你是一个智能助手，运行在 Android TV 音视频播放器中。你可以帮助用户管理文件、播放媒体。

## 核心能力
1. 文件管理：搜索、浏览、重命名、移动、删除文件
2. 播放控制：播放视频/音频、获取播放历史
3. 智能推荐：根据上下文推荐文件

## 工作流程
1. 分析用户的任务需求
2. 调用合适的工具获取信息或执行操作
3. 根据工具返回的结果回应用户

## 重要规则
- 请始终使用简体中文与我对话
- 回复的详略与任务相称：简单的确认、寒暄或轻量操作，一句话自然回应即可
- 对于复杂任务，可以分步调用多个工具
- 删除操作只是软删除，文件会移到垃圾箱
    """.trimIndent()

    fun execute(
        client: OpenAiClient,
        model: String,
        history: List<ChatMessage>,
        tools: List<ToolDefinition>
    ): Flow<AgentEvent> = flow {
        val conversation = mutableListOf<ChatMessage>(
            ChatMessage(role = "system", content = systemPrompt)
        )
        conversation.addAll(history)

        var iteration = 0
        val maxIterations = 10

        while (iteration < maxIterations) {
            iteration++
            emit(AgentEvent.Thinking("迭代 $iteration"))

            val response = try {
                client.chatCompletion(model, conversation, tools)
            } catch (e: Exception) {
                emit(AgentEvent.Error("AI 调用失败: ${e.message}"))
                return@flow
            }

            if (response.toolCalls.isEmpty()) {
                emit(AgentEvent.Response(response.content))
                emit(AgentEvent.Done)
                return@flow
            }

            val assistantMessage = ChatMessage(
                role = "assistant",
                content = response.content,
                toolCalls = response.toolCalls.map { 
                    com.tvplayer.ai.client.ToolCall(it.id, it.name, it.arguments) 
                }
            )
            conversation.add(assistantMessage)

            for (toolCall in response.toolCalls) {
                emit(AgentEvent.ToolCall(toolCall.id, toolCall.name, toolCall.arguments))

                val result = try {
                    val args = JSONObject(toolCall.arguments).toMap()
                    toolExecutor.execute(toolCall.name, args)
                } catch (e: Exception) {
                    "执行异常: ${e.message}"
                }

                emit(AgentEvent.ToolResult(toolCall.id, toolCall.name, result))

                conversation.add(ChatMessage(
                    role = "tool",
                    content = result,
                    toolCallId = toolCall.id
                ))
            }
        }

        emit(AgentEvent.Error("达到最大迭代次数"))
        emit(AgentEvent.Done)
    }

    private fun JSONObject.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        for (key in keys()) {
            map[key] = get(key)
        }
        return map
    }
}

sealed class AgentEvent {
    data class Thinking(val content: String) : AgentEvent()
    data class ToolCall(val id: String, val name: String, val arguments: String) : AgentEvent()
    data class ToolResult(val id: String, val name: String, val result: String) : AgentEvent()
    data class Response(val content: String) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
    object Done : AgentEvent()
}
