package com.tvplayer.app.ui.qrcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.tvplayer.webserver.WebServer
import java.net.InetAddress
import javax.inject.Inject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

@HiltViewModel
class QRCodeViewModel @Inject constructor(
    private val webServer: WebServer
) : ViewModel() {

    private val okHttpClient = OkHttpClient()

    private val _qrCodeUrl = MutableStateFlow<String?>(null)
    val qrCodeUrl: StateFlow<String?> = _qrCodeUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun generateQRCode() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Create input session
                val request = Request.Builder()
                    .url("http://127.0.0.1:${webServer.activePort.value}/api/input/create")
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val json = JSONObject(body ?: "{}")
                    val url = json.optString("url", "")
                    
                    if (url.isNotEmpty()) {
                        _qrCodeUrl.value = url
                    } else {
                        _error.value = "未获取到二维码 URL"
                    }
                } else {
                    _error.value = "创建会话失败: ${response.code}"
                }
            } catch (e: Exception) {
                _error.value = "生成二维码失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
