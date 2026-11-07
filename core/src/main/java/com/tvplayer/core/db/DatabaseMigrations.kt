package com.tvplayer.core.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE play_history ADD COLUMN playCount INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS series_playlist (" +
                "folderPath TEXT NOT NULL, sortIndex INTEGER NOT NULL, filePath TEXT NOT NULL, " +
                "PRIMARY KEY(folderPath, sortIndex))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS app_settings (" +
                "settingKey TEXT NOT NULL, settingValue TEXT NOT NULL, PRIMARY KEY(settingKey))"
        )
    }
}
