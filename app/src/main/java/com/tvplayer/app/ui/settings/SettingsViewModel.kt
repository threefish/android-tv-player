package com.tvplayer.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvplayer.core.model.PlayReminder
import com.tvplayer.core.model.PlaybackSettings
import com.tvplayer.core.model.WebDavConfig
import com.tvplayer.core.repository.AppSettingsRepository
import com.tvplayer.core.repository.WebDavConfigRepository
import com.tvplayer.webserver.WebServer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val webDavConfigRepository: WebDavConfigRepository,
    private val appSettingsRepository: AppSettingsRepository,
    webServer: WebServer
) : ViewModel() {

    val webPort: StateFlow<Int> = webServer.activePort

    private val _webDavConfig = MutableStateFlow<WebDavConfig?>(null)
    val webDavConfig: StateFlow<WebDavConfig?> = _webDavConfig.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    /** null=尚未测试，true=连接正常，false=连接失败 */
    private val _connectionStatus = MutableStateFlow<Boolean?>(null)
    val connectionStatus: StateFlow<Boolean?> = _connectionStatus.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _seekStep = MutableStateFlow(10)
    val seekStep: StateFlow<Int> = _seekStep.asStateFlow()

    private val _subtitleSize = MutableStateFlow(1.0f)
    val subtitleSize: StateFlow<Float> = _subtitleSize.asStateFlow()

    private val _dualSubtitle = MutableStateFlow(false)
    val dualSubtitle: StateFlow<Boolean> = _dualSubtitle.asStateFlow()

    private val _playReminder = MutableStateFlow(PlayReminder())
    val playReminder: StateFlow<PlayReminder> = _playReminder.asStateFlow()

    val version = "1.1.0"

    init {
        loadWebDavConfig()
        viewModelScope.launch {
            _playReminder.value = appSettingsRepository.getPlayReminder()
            val settings = appSettingsRepository.getPlaybackSettings()
            _playbackSpeed.value = settings.playbackSpeed
            _seekStep.value = settings.seekStepSeconds
            _subtitleSize.value = settings.subtitleSize
            _dualSubtitle.value = settings.dualSubtitle
        }
    }

    fun updatePlayReminder(reminder: PlayReminder) {
        _playReminder.value = reminder
        viewModelScope.launch {
            appSettingsRepository.setPlayReminder(reminder)
        }
    }

    private fun loadWebDavConfig() {
        viewModelScope.launch {
            _webDavConfig.value = webDavConfigRepository.getActiveConfig()
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _isTesting.value = true
            _toastMessage.value = null
            try {
                val config = _webDavConfig.value
                if (config == null) {
                    _toastMessage.value = "未配置文件来源，请先新增"
                } else {
                    val success = webDavConfigRepository.testConnection(config)
                    _connectionStatus.value = success
                    _toastMessage.value = if (success) {
                        "连接成功"
                    } else {
                        "连接失败，请检查配置"
                    }
                }
            } catch (e: Exception) {
                _connectionStatus.value = false
                _toastMessage.value = "连接失败: ${e.message}"
                android.util.Log.e("SettingsVM", "testConnection exception", e)
            } finally {
                _isTesting.value = false
            }
        }
    }

    fun deleteConfig() {
        viewModelScope.launch {
            val config = _webDavConfig.value ?: return@launch
            webDavConfigRepository.save(config.copy(isEnabled = false))
            _webDavConfig.value = null
            _connectionStatus.value = null
        }
    }

    fun saveConfig(config: WebDavConfig) {
        viewModelScope.launch {
            webDavConfigRepository.save(config)
            // 新增后重新读取，确保拿到数据库生成的真实 id
            _webDavConfig.value = webDavConfigRepository.getActiveConfig()
            _connectionStatus.value = null
        }
    }

    fun updatePlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        persistPlaybackSettings()
    }

    fun updateSeekStep(step: Int) {
        _seekStep.value = step
        persistPlaybackSettings()
    }

    fun updateSubtitleSize(size: Float) {
        _subtitleSize.value = size
        persistPlaybackSettings()
    }

    fun toggleDualSubtitle() {
        _dualSubtitle.value = !_dualSubtitle.value
        persistPlaybackSettings()
    }

    private fun persistPlaybackSettings() {
        val settings = PlaybackSettings(
            playbackSpeed = _playbackSpeed.value,
            seekStepSeconds = _seekStep.value,
            subtitleSize = _subtitleSize.value,
            dualSubtitle = _dualSubtitle.value
        )
        viewModelScope.launch {
            appSettingsRepository.setPlaybackSettings(settings)
        }
    }
}
