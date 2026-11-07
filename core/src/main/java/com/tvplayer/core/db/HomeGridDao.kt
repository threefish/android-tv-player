package com.tvplayer.core.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeGridDao {
    @Query("SELECT * FROM home_grid ORDER BY sortOrder ASC, createTime ASC")
    suspend fun getAll(): List<HomeGridEntity>

    @Query("SELECT * FROM home_grid ORDER BY sortOrder ASC, createTime ASC")
    fun getAllFlow(): Flow<List<HomeGridEntity>>

    @Query("SELECT * FROM home_grid WHERE id = :id")
    suspend fun getById(id: Long): HomeGridEntity?

    @Query("SELECT * FROM home_grid WHERE folderPath = :folderPath LIMIT 1")
    suspend fun getByPath(folderPath: String): HomeGridEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HomeGridEntity): Long

    @Update
    suspend fun update(entity: HomeGridEntity)

    @Query("DELETE FROM home_grid WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 清掉绑定的目录已被同步删除后失效的首页卡片，返回删除行数 */
    @Query(
        """
        DELETE FROM home_grid WHERE NOT EXISTS (
            SELECT 1 FROM files
            WHERE files.path = home_grid.folderPath AND files.isDeleted = 0 AND files.isDirectory = 1
        )
        """
    )
    suspend fun deleteMissingFolders(): Int
}
