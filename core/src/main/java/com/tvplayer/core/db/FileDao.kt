package com.tvplayer.core.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    
    @Query("SELECT * FROM files WHERE path = :path AND isDeleted = 0")
    suspend fun getByPath(path: String): FileEntity?
    
    @Query("SELECT * FROM files WHERE parentId = :parentId AND isDeleted = 0 ORDER BY isDirectory DESC, name ASC")
    suspend fun getChildren(parentId: Long): List<FileEntity>
    
    @Query("SELECT * FROM files WHERE parentId = :parentId AND isDeleted = 0 ORDER BY isDirectory DESC, name ASC")
    fun getChildrenFlow(parentId: Long): Flow<List<FileEntity>>
    
    @Query("SELECT * FROM files WHERE path = '/' AND isDeleted = 0")
    suspend fun getRoot(): FileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: FileEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<FileEntity>)
    
    @Update
    suspend fun update(file: FileEntity)
    
    @Query("UPDATE files SET isDeleted = 1, deletedTime = :deletedTime WHERE path = :path")
    suspend fun markAsDeleted(path: String, deletedTime: Long = System.currentTimeMillis())
    
    @Query("UPDATE files SET isDeleted = 0, deletedTime = NULL WHERE path = :path")
    suspend fun restore(path: String)
    
    @Query("DELETE FROM files WHERE path = :path")
    suspend fun deletePermanently(path: String)
    
    @Query("SELECT * FROM files WHERE isDeleted = 1 ORDER BY deletedTime DESC")
    suspend fun getTrash(): List<FileEntity>
    
    @Query("SELECT * FROM files WHERE isDeleted = 1 ORDER BY deletedTime DESC")
    fun getTrashFlow(): Flow<List<FileEntity>>
    
    @Query("SELECT * FROM files WHERE (name LIKE '%' || :keyword || '%' OR path LIKE '%' || :keyword || '%') AND isDeleted = 0")
    suspend fun searchByName(keyword: String): List<FileEntity>
    
    @Query("SELECT * FROM files WHERE (name LIKE '%' || :keyword || '%' OR path LIKE '%' || :keyword || '%') AND isDeleted = 0 AND mimeType LIKE :mimeTypePattern")
    suspend fun searchByType(keyword: String, mimeTypePattern: String): List<FileEntity>
    
    @Query("SELECT MAX(syncTime) FROM files")
    suspend fun getLastSyncTime(): Long?
    
    @Query("UPDATE files SET path = :newPath, name = :newName WHERE path = :oldPath")
    suspend fun rename(oldPath: String, newPath: String, newName: String)
    
    @Query("UPDATE files SET path = :newPath, parentId = :newParentId WHERE path = :oldPath")
    suspend fun move(oldPath: String, newPath: String, newParentId: Long?)
    
    @Query("SELECT * FROM files WHERE isDeleted = 0 ORDER BY syncTime DESC LIMIT :limit")
    suspend fun getRecentFiles(limit: Int): List<FileEntity>

    @Query(
        """
        SELECT * FROM files
        WHERE isDeleted = 0 AND isDirectory = 0
          AND (lower(name) LIKE '%.jpg' OR lower(name) LIKE '%.jpeg'
            OR lower(name) LIKE '%.png' OR lower(name) LIKE '%.webp'
            OR lower(name) LIKE '%.bmp' OR lower(name) LIKE '%.gif')
        """
    )
    suspend fun getAllImages(): List<FileEntity>
    
    @Query("DELETE FROM files")
    suspend fun clearAll()

    @Query("DELETE FROM files WHERE path LIKE '%/#recycle' OR path LIKE '%/#recycle/%'")
    suspend fun deleteRecycleBin()

    /** 删除指定路径及其全部子孙条目，返回删除行数（用精确前缀，避免 LIKE 把 _ % 当通配符） */
    @Query("DELETE FROM files WHERE path = :path OR substr(path, 1, length(:path) + 1) = :path || '/'")
    suspend fun deleteByPathPrefix(path: String): Int

    /** 目录下全部子孙文件（不含目录本身），精确前缀匹配；媒体类型过滤交给 FileInfo 按扩展名判断 */
    @Query("SELECT * FROM files WHERE isDeleted = 0 AND isDirectory = 0 AND substr(path, 1, length(:folderPrefix) + 1) = :folderPrefix || '/'")
    suspend fun getDescendantFiles(folderPrefix: String): List<FileEntity>
}
