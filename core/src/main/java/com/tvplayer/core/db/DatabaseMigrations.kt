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

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS daily_watch_time (" +
                "date TEXT NOT NULL, seconds INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(date))"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 历史版本写入过同地址重复记录，同源保留最小 id
        db.execSQL(
            "DELETE FROM webdav_config WHERE id NOT IN " +
                "(SELECT MIN(id) FROM webdav_config GROUP BY url, username, rootPath)"
        )
        db.execSQL(
            "UPDATE webdav_config SET isEnabled = CASE WHEN id = " +
                "(SELECT MIN(id) FROM webdav_config WHERE isEnabled = 1) THEN 1 ELSE 0 END"
        )
    }
}
