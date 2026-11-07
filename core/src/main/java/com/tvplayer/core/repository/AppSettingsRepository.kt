package com.tvplayer.core.repository

import com.tvplayer.core.model.PlayReminder
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    suspend fun getPlayReminder(): PlayReminder
    fun getPlayReminderFlow(): Flow<PlayReminder>
    suspend fun setPlayReminder(reminder: PlayReminder)
}