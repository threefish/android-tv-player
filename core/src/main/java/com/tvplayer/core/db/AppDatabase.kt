package com.tvplayer.core.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FileEntity::class,
        WebDavConfigEntity::class,
        PlayHistoryEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        AiProviderEntity::class,
        PlaylistEntity::class,
        HomeGridEntity::class,
        SeriesPlaylistEntity::class,
        AppSettingEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun webDavConfigDao(): WebDavConfigDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun aiProviderDao(): AiProviderDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun homeGridDao(): HomeGridDao
    abstract fun seriesPlaylistDao(): SeriesPlaylistDao
    abstract fun appSettingsDao(): AppSettingsDao
}
