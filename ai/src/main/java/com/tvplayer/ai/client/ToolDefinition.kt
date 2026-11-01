package com.tvplayer.ai.client

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>,
    val required: List<String> = emptyList()
)
