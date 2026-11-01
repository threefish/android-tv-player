package com.tvplayer.app.image

import android.content.Context
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import com.tvplayer.core.repository.WebDavConfigRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Coil 的数据 key：只用路径与修改时间参与缓存，账号密码不会进入 URL、缓存键或界面状态。
 */
data class WebDavImageKey(
    val sourceId: Long,
    val path: String,
    val modifiedTime: Long,
) : Keyer<WebDavImageKey> {
    override fun key(data: WebDavImageKey, options: Options): String =
        "webdav:${data.sourceId}:${data.modifiedTime}:${data.path}"
}

class WebDavImageFetcher private constructor(
    private val key: WebDavImageKey,
    private val configRepository: WebDavConfigRepository,
    private val client: OkHttpClient,
    private val context: Context,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val config = configRepository.getActiveConfig()
            ?: throw IOException("未配置 WebDAV")

        val url = "${config.url.trimEnd('/')}/${key.path.trimStart('/')}"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(config.username, config.password))
            .get()
            .build()

        val call = client.newCall(request)
        return suspendCancellableCoroutine { continuation ->
            // 滚动出可见区域时 Coil 会取消请求，顺带断开下载
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body
                    if (!response.isSuccessful || body == null) {
                        response.close()
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IOException("图片下载失败: HTTP ${response.code}")
                            )
                        }
                        return
                    }
                    if (!continuation.isActive) {
                        response.close()
                        return
                    }
                    continuation.resume(
                        SourceResult(
                            source = ImageSource(body.source(), context),
                            mimeType = body.contentType()?.toString(),
                            dataSource = DataSource.NETWORK,
                        )
                    )
                }
            })
        }
    }

    @Singleton
    class Factory @Inject constructor(
        private val configRepository: WebDavConfigRepository,
    ) : Fetcher.Factory<WebDavImageKey> {

        private val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        override fun create(
            data: WebDavImageKey,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = WebDavImageFetcher(data, configRepository, client, options.context)
    }
}
