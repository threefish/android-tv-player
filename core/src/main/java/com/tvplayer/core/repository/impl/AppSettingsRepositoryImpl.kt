package com.tvplayer.core.repository.impl

import com.tvplayer.core.db.AppSettingEntity
import com.tvplayer.core.db.AppSettingsDao
import com.tvplayer.core.model.PlayReminder
import com.tvplayer.core.model.RestDeadline
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
                AppSettingEntity(KEY_REMINDER_PLAY_MINUTES, reminder.playMinutes.toString()),
                AppSettingEntity(KEY_REMINDER_REST_MINUTES, reminder.restMinutes.toString()),
                AppSettingEntity(KEY_REMINDER_AUTO_RESUME, reminder.autoResume.toString())
            )
        )
    }

    override suspend fun getRestDeadline(): RestDeadline? {
        val values = appSettingsDao.getAll().associate { it.settingKey to it.settingValue }
        val until = values[KEY_REST_UNTIL]?.toLongOrNull() ?: return null
        if (until <= 0) return null
        return RestDeadline(until, values[KEY_REST_AUTO_RESUME] == "true")
    }

    override suspend fun setRestDeadline(deadline: RestDeadline) {
        appSettingsDao.putAll(
            listOf(
                AppSettingEntity(KEY_REST_UNTIL, deadline.untilMillis.toString()),
                AppSettingEntity(KEY_REST_AUTO_RESUME, deadline.autoResume.toString())
            )
        )
    }

    override suspend fun clearRestDeadline() {
        appSettingsDao.putAll(listOf(AppSettingEntity(KEY_REST_UNTIL, "0")))
    }

    private fun List<AppSettingEntity>.toPlayReminder(): PlayReminder {
        val values = associate { it.settingKey to it.settingValue }
        return PlayReminder(
            enabled = values[KEY_REMINDER_ENABLED] == "true",
            playMinutes = values[KEY_REMINDER_PLAY_MINUTES]?.toIntOrNull()?.coerceIn(1, 240) ?: 15,
            restMinutes = values[KEY_REMINDER_REST_MINUTES]?.toIntOrNull()?.coerceIn(1, 240) ?: 5,
            autoResume = values[KEY_REMINDER_AUTO_RESUME] == "true"
        )
    }

    private companion object {
        const val KEY_REMINDER_ENABLED = "playReminderEnabled"
        // 沿用旧键，升级前已配置的连续播放时长不丢
        const val KEY_REMINDER_PLAY_MINUTES = "playReminderMinutes"
        const val KEY_REMINDER_REST_MINUTES = "playReminderRestMinutes"
        const val KEY_REMINDER_AUTO_RESUME = "playReminderAutoResume"
        const val KEY_REST_UNTIL = "playReminderRestUntil"
        const val KEY_REST_AUTO_RESUME = "playReminderRestAutoResume"
    }
}