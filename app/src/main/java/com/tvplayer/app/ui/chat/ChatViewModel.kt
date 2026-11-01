package com.tvplayer.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvplayer.ai.chat.ChatEvent
import com.tvplayer.ai.chat.ChatService
import com.tvplayer.core.db.ChatMessageEntity
import com.tvplayer.core.db.ChatSessionEntity
import com.tvplayer.core.model.AiProviderConfig
import com.tvplayer.core.repository.AiProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatService: ChatService,
    private val aiProviderRepository: AiProviderRepository
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<ChatSessionEntity>>(emptyList())
    val sessions: StateFlow<List<ChatSessionEntity>> = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _responseContent = MutableStateFlow("")
    val responseContent: StateFlow<String> = _responseContent.asStateFlow()

    private val _aiProviders = MutableStateFlow<List<AiProviderConfig>>(emptyList())
    val aiProviders: StateFlow<List<AiProviderConfig>> = _aiProviders.asStateFlow()

    private val _selectedProvider = MutableStateFlow<AiProviderConfig?>(null)
    val selectedProvider: StateFlow<AiProviderConfig?> = _selectedProvider.asStateFlow()

    private var aiConfig = AiProviderConfig(
        id = 0,
        name = "Default",
        baseUrl = "",
        apiKey = "",
        model = "gpt-4"
    )

    init {
        loadSessions()
        loadAiProviders()
    }

    private fun loadAiProviders() {
        viewModelScope.launch {
            val providers = aiProviderRepository.getAll()
            _aiProviders.value = providers
            
            // Auto-select active provider or first one
            val activeProvider = providers.find { it.isActive }
            if (activeProvider != null) {
                selectProvider(activeProvider.id)
            } else if (providers.isNotEmpty()) {
                selectProvider(providers[0].id)
            }
        }
    }

    fun selectProvider(providerId: Long) {
        viewModelScope.launch {
            val provider = _aiProviders.value.find { it.id == providerId }
            if (provider != null) {
                _selectedProvider.value = provider
                aiConfig = provider
            }
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            _sessions.value = chatService.getSessions()
            if (_sessions.value.isNotEmpty() && _currentSessionId.value == null) {
                selectSession(_sessions.value[0].sessionId)
            }
        }
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            _messages.value = chatService.getSessionMessages(sessionId)
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val session = chatService.createSession()
            _sessions.value = chatService.getSessions()
            selectSession(session.sessionId)
        }
    }

    fun setAiConfig(baseUrl: String, apiKey: String, model: String) {
        aiConfig = aiConfig.copy(baseUrl = baseUrl, apiKey = apiKey, model = model)
    }

    fun sendMessage(message: String) {
        val sessionId = _currentSessionId.value ?: return
        if (aiConfig.apiKey.isBlank()) return

        viewModelScope.launch {
            _isSending.value = true
            _responseContent.value = ""

            val userMessage = ChatMessageEntity(
                sessionId = sessionId,
                role = "user",
                content = message
            )
            _messages.value = _messages.value + userMessage

            try {
                chatService.sendMessage(aiConfig, sessionId, message).collect { event ->
                    when (event) {
                        is ChatEvent.Response -> {
                            _responseContent.value += event.content
                        }
                        is ChatEvent.Error -> {
                            // Handle error
                        }
                        is ChatEvent.Done -> {
                            _messages.value = chatService.getSessionMessages(sessionId)
                            loadSessions()
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                // Handle exception
            } finally {
                _isSending.value = false
            }
        }
    }
}
