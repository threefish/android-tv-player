package com.tvplayer.core.repository.impl

import com.tvplayer.core.db.AppSettingEntity
import com.tvplayer.core.db.AppSettingsDao
import com.tvplayer.core.model.PlayReminder
import com.tvplayer.core.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsRepositoryImpl @Inject constructor(
    private val appSettingsDao: AppSettingsDao
) : AppSettingsRepository {

    override suspend fun getPlayReminder(): PlayReminder =
        appSettingsDao.getAll().toPlayReminder()

    override fun getPlayReminderFlow(): Flow<PlayReminder> =
        appSettingsDao.getAllFlow().map { it.toPlayReminder() }

    override suspend fun setPlayReminder(reminder: PlayReminder) {
        appSettingsDao.putAll(
            listOf(
                AppSettingEntity(KEY_REMINDER_ENABLED, reminder.enabled.toString()),
                AppSettingEntity(KEY_REMINDER_MINUTES, reminder.minutes.toString())
            )
        )
    }

    private fun List<AppSettingEntity>.toPlayReminder(): PlayReminder {
        val values = associate { it.settingKey to it.settingValue }
        return PlayReminder(
            enabled = values[KEY_REMINDER_ENABLED] == "true",
            minutes = values[KEY_REMINDER_MINUTES]?.toIntOrNull()?.coerceIn(1, 240) ?: 15
        )
    }

    private companion object {
        const val KEY_REMINDER_ENABLED = "playReminderEnabled"
        const val KEY_REMINDER_MINUTES = "playReminderMinutes"
    }
}