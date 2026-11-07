package com.tvplayer.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings")
    suspend fun getAll(): List<AppSettingEntity>

    @Query("SELECT * FROM app_settings")
    fun getAllFlow(): Flow<List<AppSettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAll(settings: List<AppSettingEntity>)
}