package com.tvplayer.core.repository.impl

import com.tvplayer.core.db.AiProviderDao
import com.tvplayer.core.db.AiProviderEntity
import com.tvplayer.core.model.AiProviderConfig
import com.tvplayer.core.repository.AiProviderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiProviderRepositoryImpl @Inject constructor(
    private val aiProviderDao: AiProviderDao
) : AiProviderRepository {

    override suspend fun getAll(): List<AiProviderConfig> {
        return aiProviderDao.getAll().map { it.toConfig() }
    }

    override fun getAllFlow(): Flow<List<AiProviderConfig>> {
        return aiProviderDao.getAllFlow().map { list -> list.map { it.toConfig() } }
    }

    override suspend fun getById(id: Long): AiProviderConfig? {
        return aiProviderDao.getById(id)?.toConfig()
    }

    override suspend fun getActive(): AiProviderConfig? {
        return aiProviderDao.getActive()?.toConfig()
    }

    override fun getActiveFlow(): Flow<AiProviderConfig?> {
        return aiProviderDao.getActiveFlow().map { it?.toConfig() }
    }

    override suspend fun save(config: AiProviderConfig): Long {
        val entity = config.toEntity()
        return aiProviderDao.insert(entity)
    }

    override suspend fun update(config: AiProviderConfig) {
        val entity = config.copy(updateTime = System.currentTimeMillis()).toEntity()
        aiProviderDao.update(entity)
    }

    override suspend fun delete(id: Long) {
        aiProviderDao.deleteById(id)
    }

    override suspend fun setActive(id: Long) {
        aiProviderDao.deactivateAll()
        aiProviderDao.activate(id)
    }

    private fun AiProviderEntity.toConfig() = AiProviderConfig(
        id = id,
        name = name,
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model,
        isActive = isActive,
        createTime = createTime,
        updateTime = updateTime
    )

    private fun AiProviderConfig.toEntity() = AiProviderEntity(
        id = id,
        name = name,
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model,
        isActive = isActive,
        createTime = createTime,
        updateTime = updateTime
    )
}
