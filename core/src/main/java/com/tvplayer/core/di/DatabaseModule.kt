package com.tvplayer.core.di

import android.content.Context
import androidx.room.Room
import com.tvplayer.core.db.AiProviderDao
import com.tvplayer.core.db.AppDatabase
import com.tvplayer.core.db.AppSettingsDao
import com.tvplayer.core.db.ChatMessageDao
import com.tvplayer.core.db.ChatSessionDao
import com.tvplayer.core.db.FileDao
import com.tvplayer.core.db.HomeGridDao
import com.tvplayer.core.db.MIGRATION_2_3
import com.tvplayer.core.db.MIGRATION_3_4
import com.tvplayer.core.db.PlayHistoryDao
import com.tvplayer.core.db.PlaylistDao
import com.tvplayer.core.db.SeriesPlaylistDao
import com.tvplayer.core.db.WebDavConfigDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tvplayer.db"
        ).addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFileDao(database: AppDatabase): FileDao {
        return database.fileDao()
    }

    @Provides
    fun provideWebDavConfigDao(database: AppDatabase): WebDavConfigDao {
        return database.webDavConfigDao()
    }

    @Provides
    fun providePlayHistoryDao(database: AppDatabase): PlayHistoryDao {
        return database.playHistoryDao()
    }

    @Provides
    fun provideChatSessionDao(database: AppDatabase): ChatSessionDao {
        return database.chatSessionDao()
    }

    @Provides
    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }

    @Provides
    fun provideAiProviderDao(database: AppDatabase): AiProviderDao {
        return database.aiProviderDao()
    }

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideHomeGridDao(database: AppDatabase): HomeGridDao {
        return database.homeGridDao()
    }

    @Provides
    fun provideSeriesPlaylistDao(database: AppDatabase): SeriesPlaylistDao {
        return database.seriesPlaylistDao()
    }

    @Provides
    fun provideAppSettingsDao(database: AppDatabase): AppSettingsDao {
        return database.appSettingsDao()
    }
}
