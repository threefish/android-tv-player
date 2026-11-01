package com.tvplayer.app

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import com.tvplayer.app.image.WebDavImageFetcher
import com.tvplayer.webserver.WebServer
import javax.inject.Inject

@HiltAndroidApp
class TVPlayerApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var webDavImageFetcherFactory: WebDavImageFetcher.Factory

    override fun onCreate() {
        super.onCreate()
        try {
            val entryPoint = EntryPointAccessors.fromApplication(this, WebServerEntryPoint::class.java)
            entryPoint.webServer().start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WebServer", e)
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(webDavImageFetcherFactory) }
            .crossfade(false)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("images"))
                    .maxSizeBytes(128L * 1024 * 1024)
                    .build()
            }
            .build()

    companion object {
        private const val TAG = "TVPlayerApp"
    }
}

@dagger.hilt.EntryPoint
@InstallIn(dagger.hilt.components.SingletonComponent::class)
interface WebServerEntryPoint {
    fun webServer(): WebServer
}
