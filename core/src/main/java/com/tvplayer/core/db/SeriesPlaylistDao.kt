package com.tvplayer.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SeriesPlaylistDao {

    @Query("SELECT COUNT(*) FROM series_playlist WHERE folderPath = :folderPath")
    suspend fun count(folderPath: String): Int

    /** JOIN files 过滤已同步删除的条目，顺序以缓存的 sortIndex 为准 */
    @Query(
        """
        SELECT f.* FROM series_playlist s
        JOIN files f ON f.path = s.filePath
        WHERE s.folderPath = :folderPath AND f.isDeleted = 0
        ORDER BY s.sortIndex
        """
    )
    suspend fun getFiles(folderPath: String): List<FileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<SeriesPlaylistEntity>)

    @Query("DELETE FROM series_playlist WHERE folderPath = :folderPath")
    suspend fun deleteFolder(folderPath: String)

    @Query("DELETE FROM series_playlist")
    suspend fun clearAll()
}