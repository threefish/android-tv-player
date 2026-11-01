package com.tvplayer.core.di

import com.tvplayer.core.repository.AiProviderRepository
import com.tvplayer.core.repository.FileRepository
import com.tvplayer.core.repository.HomeGridRepository
import com.tvplayer.core.repository.PlayHistoryRepository
import com.tvplayer.core.repository.PlaylistRepository
import com.tvplayer.core.repository.WebDavConfigRepository
import com.tvplayer.core.repository.impl.AiProviderRepositoryImpl
import com.tvplayer.core.repository.impl.FileRepositoryImpl
import com.tvplayer.core.repository.impl.HomeGridRepositoryImpl
import com.tvplayer.core.repository.impl.PlayHistoryRepositoryImpl
import com.tvplayer.core.repository.impl.PlaylistRepositoryImpl
import com.tvplayer.core.repository.impl.WebDavConfigRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds
    @Singleton
    abstract fun bindPlayHistoryRepository(impl: PlayHistoryRepositoryImpl): PlayHistoryRepository

    @Binds
    @Singleton
    abstract fun bindWebDavConfigRepository(impl: WebDavConfigRepositoryImpl): WebDavConfigRepository

    @Binds
    @Singleton
    abstract fun bindAiProviderRepository(impl: AiProviderRepositoryImpl): AiProviderRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindHomeGridRepository(impl: HomeGridRepositoryImpl): HomeGridRepository
}
