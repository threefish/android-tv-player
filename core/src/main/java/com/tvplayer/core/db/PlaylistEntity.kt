package com.tvplayer.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "playlists")
@TypeConverters(PlaylistConverters::class)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val trackPaths: List<String> = emptyList(),
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
)

class PlaylistConverters {
    @TypeConverter
    fun fromStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList()
        else value.split("|||")
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return list.joinToString("|||")
    }
}
