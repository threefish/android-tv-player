package com.tvplayer.app.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tvplayer.app.ui.theme.AppColors
import com.tvplayer.core.model.Playlist
import com.tvplayer.player.AudioTrack
import com.tvplayer.player.PlaybackState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    viewModel: AudioPlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val playlist by viewModel.currentPlaylist.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("音频播放器") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.surfaceVariant,
                    titleContentColor = AppColors.listHeadline
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppColors.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Now Playing
            NowPlayingCard(
                playbackState = playbackState,
                onPlayPause = { viewModel.togglePlayPause() },
                onPrevious = { viewModel.previous() },
                onNext = { viewModel.next() },
                onSeek = { position -> viewModel.seekTo(position) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Playlist
            if (playlist.isNotEmpty()) {
                Text(
                    text = "播放列表",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlist.withIndex().toList()) { (index, track) ->
                        TrackItem(
                            track = track,
                            isPlaying = index == currentIndex,
                            onClick = { viewModel.playTrack(index) }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "播放列表为空",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingCard(
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Track Info
            when (playbackState) {
                is PlaybackState.Playing -> {
                    Text(
                        text = playbackState.currentTrack.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (playbackState.currentTrack.artist.isNotEmpty()) {
                        Text(
                            text = playbackState.currentTrack.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
                is PlaybackState.Paused -> {
                    Text(
                        text = playbackState.currentTrack.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (playbackState.currentTrack.artist.isNotEmpty()) {
                        Text(
                            text = playbackState.currentTrack.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
                is PlaybackState.Loading -> {
                    Text(
                        text = "加载中...",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
                else -> {
                    Text(
                        text = "未播放",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Bar
            if (playbackState is PlaybackState.Playing || playbackState is PlaybackState.Paused) {
                val currentPosition = when (playbackState) {
                    is PlaybackState.Playing -> playbackState.position
                    is PlaybackState.Paused -> playbackState.position
                    else -> 0L
                }
                val currentDuration = when (playbackState) {
                    is PlaybackState.Playing -> playbackState.duration
                    is PlaybackState.Paused -> playbackState.duration
                    else -> 0L
                }
                var sliderPosition by remember { mutableStateOf(currentPosition.toFloat()) }
                
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = { onSeek(sliderPosition.toLong()) },
                    valueRange = 0f..currentDuration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = formatTime(currentDuration),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        "上一首",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        if (playbackState is PlaybackState.Playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        "播放/暂停",
                        modifier = Modifier.size(72.dp),
                        tint = Color.White
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Filled.SkipNext,
                        "下一首",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackItem(
    track: AudioTrack,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) AppColors.listButtonFocused else AppColors.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.MusicNote,
                contentDescription = null,
                tint = if (isPlaying) AppColors.success else AppColors.listOverline
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (track.artist.isNotEmpty()) {
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = formatTime(track.duration),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}
