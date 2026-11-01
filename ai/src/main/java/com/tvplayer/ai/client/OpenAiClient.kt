package com.tvplayer.ai.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAiClient(
    private val baseUrl: String,
    private val apiKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun chatCompletion(
        model: String,
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>? = null,
        temperature: Float = 0.7f,
        maxTokens: Int? = null
    ): ChatResponse {
        val url = "$baseUrl/chat/completions"
        val requestBody = buildRequestBody(model, messages, tools, temperature, maxTokens, stream = false)
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        // 阻塞式网络调用必须切到 IO 线程，避免占住 Ktor/Netty 的事件循环线程
        val body = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: throw Exception("Empty response")
                if (!response.isSuccessful) {
                    throw Exception("AI 接口返回 HTTP ${response.code}: ${text.take(300)}")
                }
                text
            }
        }

        return parseResponse(body)
    }

    fun streamChatCompletion(
        model: String,
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>? = null,
        temperature: Float = 0.7f,
        maxTokens: Int? = null
    ): Flow<StreamEvent> {
        return kotlinx.coroutines.flow.callbackFlow {
            val url = "$baseUrl/chat/completions"
            val requestBody = buildRequestBody(model, messages, tools, temperature, maxTokens, stream = true)
            
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val factory = EventSources.createFactory(client)
            val eventSource = factory.newEventSource(request, object : EventSourceListener() {
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    if (data == "[DONE]") {
                        trySend(StreamEvent.Done)
                        close()
                        return
                    }
                    
                    try {
                        val json = JSONObject(data)
                        val choices = json.getJSONArray("choices")
                        if (choices.length() > 0) {
                            val choice = choices.getJSONObject(0)
                            val delta = choice.getJSONObject("delta")
                            
                            if (delta.has("content")) {
                                val content = delta.getString("content")
                                trySend(StreamEvent.Content(content))
                            }
                            
                            if (delta.has("tool_calls")) {
                                val toolCalls = delta.getJSONArray("tool_calls")
                                for (i in 0 until toolCalls.length()) {
                                    val toolCall = toolCalls.getJSONObject(i)
                                    val function = toolCall.getJSONObject("function")
                                    trySend(StreamEvent.ToolCall(
                                        id = toolCall.getString("id"),
                                        name = function.getString("name"),
                                        arguments = function.getString("arguments")
                                    ))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        trySend(StreamEvent.Error(e.message ?: "Parse error"))
                    }
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                    trySend(StreamEvent.Error(t?.message ?: "Connection failed"))
                    close()
                }

                override fun onClosed(eventSource: EventSource) {
                    close()
                }
            })

            awaitClose { eventSource.cancel() }
        }
    }

    private fun buildRequestBody(
        model: String,
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>?,
        temperature: Float,
        maxTokens: Int?,
        stream: Boolean
    ): String {
        val json = JSONObject()
        json.put("model", model)
        // Android 系统 org.json 没有 put(String, float)，只能传 double
        json.put("temperature", temperature.toDouble())
        json.put("stream", stream)
        
        if (maxTokens != null) {
            json.put("max_tokens", maxTokens)
        }

        val messagesArray = JSONArray()
        for (msg in messages) {
            val msgJson = JSONObject()
            msgJson.put("role", msg.role)
            msgJson.put("content", msg.content)
            if (msg.toolCallId != null) {
                msgJson.put("tool_call_id", msg.toolCallId)
            }
            if (msg.toolCalls != null) {
                val toolCallsArray = JSONArray()
                for (tc in msg.toolCalls) {
                    val tcJson = JSONObject()
                    tcJson.put("id", tc.id)
                    tcJson.put("type", "function")
                    val funcJson = JSONObject()
                    funcJson.put("name", tc.name)
                    funcJson.put("arguments", tc.arguments)
                    tcJson.put("function", funcJson)
                    toolCallsArray.put(tcJson)
                }
                msgJson.put("tool_calls", toolCallsArray)
            }
            messagesArray.put(msgJson)
        }
        json.put("messages", messagesArray)

        if (tools != null && tools.isNotEmpty()) {
            val toolsArray = JSONArray()
            for (tool in tools) {
                val toolJson = JSONObject()
                toolJson.put("type", "function")
                val funcJson = JSONObject()
                funcJson.put("name", tool.name)
                funcJson.put("description", tool.description)
                
                val paramsJson = JSONObject()
                paramsJson.put("type", "object")
                val propsJson = JSONObject()
                for ((key, value) in tool.parameters) {
                    // 嵌套的 Map/List 必须转成 JSONObject/JSONArray，
                    // 否则系统 org.json 会把它们当成普通对象输出成字符串
                    propsJson.put(key, toJsonValue(value))
                }
                paramsJson.put("properties", propsJson)
                
                if (tool.required.isNotEmpty()) {
                    val reqArray = JSONArray()
                    for (req in tool.required) {
                        reqArray.put(req)
                    }
                    paramsJson.put("required", reqArray)
                }
                
                funcJson.put("parameters", paramsJson)
                toolJson.put("function", funcJson)
                toolsArray.put(toolJson)
            }
            json.put("tools", toolsArray)
        }

        return json.toString()
    }

    private fun toJsonValue(value: Any?): Any = when (value) {
        is Map<*, *> -> JSONObject().apply {
            for ((k, v) in value) {
                put(k.toString(), toJsonValue(v))
            }
        }
        is Iterable<*> -> JSONArray().apply {
            for (item in value) {
                put(toJsonValue(item))
            }
        }
        else -> value ?: JSONObject.NULL
    }

    private fun parseResponse(body: String): ChatResponse {
        val json = JSONObject(body)
        val choices = json.getJSONArray("choices")
        val choice = choices.getJSONObject(0)
        val message = choice.getJSONObject("message")
        
        val content = message.optString("content", "")
        val toolCalls = mutableListOf<ToolCall>()
        
        if (message.has("tool_calls")) {
            val toolCallsArray = message.getJSONArray("tool_calls")
            for (i in 0 until toolCallsArray.length()) {
                val tc = toolCallsArray.getJSONObject(i)
                val func = tc.getJSONObject("function")
                toolCalls.add(ToolCall(
                    id = tc.getString("id"),
                    name = func.getString("name"),
                    arguments = func.getString("arguments")
                ))
            }
        }
        
        return ChatResponse(
            content = content,
            toolCalls = toolCalls
        )
    }
}

data class ChatMessage(
    val role: String,
    val content: String,
    val toolCallId: String? = null,
    val toolCalls: List<ToolCall>? = null
)

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String
)

data class ChatResponse(
    val content: String,
    val toolCalls: List<ToolCall> = emptyList()
)

sealed class StreamEvent {
    data class Content(val text: String) : StreamEvent()
    data class ToolCall(val id: String, val name: String, val arguments: String) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
    object Done : StreamEvent()
}
