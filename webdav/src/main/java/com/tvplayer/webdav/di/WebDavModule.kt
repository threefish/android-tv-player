package com.tvplayer.webdav.di

import com.tvplayer.core.repository.WebDavConnectionTester
import com.tvplayer.webdav.SyncService
import com.tvplayer.webdav.WebDavConnectionTesterImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WebDavModule {

    @Binds
    @Singleton
    abstract fun bindConnectionTester(impl: WebDavConnectionTesterImpl): WebDavConnectionTester
}
