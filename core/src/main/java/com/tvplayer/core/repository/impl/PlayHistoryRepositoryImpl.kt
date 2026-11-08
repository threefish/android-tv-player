package com.tvplayer.core.repository.impl

import com.tvplayer.core.db.PlayHistoryDao
import com.tvplayer.core.db.PlayHistoryEntity
import com.tvplayer.core.model.PlayHistory
import com.tvplayer.core.repository.PlayHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayHistoryRepositoryImpl @Inject constructor(
    private val playHistoryDao: PlayHistoryDao
) : PlayHistoryRepository {

    // 周期保存与退出保存可能并发，filePath 无唯一索引，串行化避免同一视频插入多条
    private val saveMutex = Mutex()

    override suspend fun getAll(): List<PlayHistory> {
        return playHistoryDao.getAll().map { it.toPlayHistory() }
    }

    override fun getAllFlow(): Flow<List<PlayHistory>> {
        return playHistoryDao.getAllFlow().map { list -> list.map { it.toPlayHistory() } }
    }

    override suspend fun getRecent(limit: Int): List<PlayHistory> {
        return playHistoryDao.getRecent(limit).map { it.toPlayHistory() }
    }

    override suspend fun getByPath(filePath: String): PlayHistory? {
        return playHistoryDao.getByPath(filePath)?.toPlayHistory()
    }

    override suspend fun recordPlayStart(filePath: String, fileName: String) {
        saveMutex.withLock {
            val existing = playHistoryDao.getByPath(filePath)
            val now = System.currentTimeMillis()
            if (existing != null) {
                // 只累加次数与时间，保留已有的续播进度
                playHistoryDao.update(existing.copy(
                    playCount = existing.playCount + 1,
                    lastPlayedTime = now
                ))
            } else {
                playHistoryDao.insert(PlayHistoryEntity(
                    filePath = filePath,
                    fileName = fileName,
                    position = 0,
                    duration = 0,
                    playCount = 1,
                    lastPlayedTime = now
                ))
            }
        }
    }

    override suspend fun save(history: PlayHistory) {
        saveMutex.withLock {
            val existing = playHistoryDao.getByPath(history.filePath)
            if (existing != null) {
                playHistoryDao.update(existing.copy(
                    position = history.position,
                    duration = history.duration,
                    lastPlayedTime = System.currentTimeMillis()
                ))
            } else {
                playHistoryDao.insert(history.toEntity())
            }
        }
    }

    override suspend fun savePlaybackSettings(
        filePath: String,
        fileName: String,
        volume: Float?,
        resizeMode: Int?,
        playbackSpeed: Float?,
        subtitleSize: Float?
    ) {
        if (volume == null && resizeMode == null && playbackSpeed == null && subtitleSize == null) return
        saveMutex.withLock {
            val existing = playHistoryDao.getByPath(filePath)
            if (existing != null) {
                playHistoryDao.update(existing.copy(
                    volume = volume ?: existing.volume,
                    // 只有真正写入过音量才视为“调整过”，旧版本增益值不参与系统音量恢复
                    volumeSet = existing.volumeSet || volume != null,
                    resizeMode = resizeMode ?: existing.resizeMode,
                    playbackSpeed = playbackSpeed ?: existing.playbackSpeed,
                    subtitleSize = subtitleSize ?: existing.subtitleSize
                ))
            } else {
                playHistoryDao.insert(PlayHistoryEntity(
                    filePath = filePath,
                    fileName = fileName,
                    position = 0,
                    duration = 0,
                    volume = volume ?: 1.0f,
                    volumeSet = volume != null,
                    resizeMode = resizeMode ?: 0,
                    playbackSpeed = playbackSpeed,
                    subtitleSize = subtitleSize
                ))
            }
        }
    }

    override suspend fun delete(id: Long) {
        playHistoryDao.delete(id)
    }

    override suspend fun clearAll() {
        playHistoryDao.clearAll()
    }

    override suspend fun getLatestInFolder(folderPath: String): PlayHistory? {
        val prefix = folderPath.trimEnd('/')
        return playHistoryDao.getLatestInFolder(prefix)?.toPlayHistory()
    }

    private fun PlayHistoryEntity.toPlayHistory() = PlayHistory(
        id = id,
        filePath = filePath,
        fileName = fileName,
        position = position,
        duration = duration,
        playCount = playCount,
        lastPlayedTime = lastPlayedTime,
        volume = volume,
        volumeSet = volumeSet,
        resizeMode = resizeMode,
        playbackSpeed = playbackSpeed,
        subtitleSize = subtitleSize
    )

    private fun PlayHistory.toEntity() = PlayHistoryEntity(
        id = id,
        filePath = filePath,
        fileName = fileName,
        position = position,
        duration = duration,
        playCount = playCount,
        lastPlayedTime = lastPlayedTime,
        volume = volume,
        volumeSet = volumeSet,
        resizeMode = resizeMode,
        playbackSpeed = playbackSpeed,
        subtitleSize = subtitleSize
    )
}
