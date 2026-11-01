package com.tvplayer.app.ui.player

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.tvplayer.app.ui.theme.TVPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoPlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoPath = intent.getStringExtra("videoPath")
        val folderPath = intent.getStringExtra("folderPath")
        if (videoPath == null && folderPath == null) {
            finish()
            return
        }

        setContent {
            TVPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: VideoPlayerViewModel = hiltViewModel()
                    VideoPlayerScreen(
                        onBack = {
                            // 带上最后播放的条目，文件列表返回时据此定位焦点
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra("lastPlayedPath", viewModel.lastPlayedPath())
                            )
                            finish()
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
