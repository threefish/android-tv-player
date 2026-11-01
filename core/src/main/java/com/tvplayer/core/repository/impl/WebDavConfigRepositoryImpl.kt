package com.tvplayer.core.repository.impl

import com.tvplayer.core.db.WebDavConfigDao
import com.tvplayer.core.db.WebDavConfigEntity
import com.tvplayer.core.model.WebDavConfig
import com.tvplayer.core.repository.WebDavConfigRepository
import com.tvplayer.core.repository.WebDavConnectionTester
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavConfigRepositoryImpl @Inject constructor(
    private val webDavConfigDao: WebDavConfigDao,
    private val connectionTester: WebDavConnectionTester
) : WebDavConfigRepository {

    override suspend fun getActiveConfig(): WebDavConfig? {
        return webDavConfigDao.getActiveConfig()?.toWebDavConfig()
    }

    override fun getActiveConfigFlow(): Flow<WebDavConfig?> {
        return webDavConfigDao.getActiveConfigFlow().map { it?.toWebDavConfig() }
    }

    override suspend fun getAll(): List<WebDavConfig> {
        return webDavConfigDao.getAll().map { it.toWebDavConfig() }
    }

    override suspend fun save(config: WebDavConfig): Long {
        val entity = config.toEntity()
        if (config.id == 0L) {
            // 新增的来源直接启用，并停用其它来源
            val newId = webDavConfigDao.insert(entity.copy(isEnabled = true))
            webDavConfigDao.setActive(newId)
            return newId
        }

        webDavConfigDao.update(entity)
        if (config.isEnabled) {
            webDavConfigDao.setActive(config.id)
        }
        return config.id
    }

    override suspend fun setActive(id: Long) {
        webDavConfigDao.setActive(id)
    }

    override suspend fun delete(id: Long) {
        webDavConfigDao.delete(id)
        // 删除的是当前激活项时，自动启用剩余的第一条配置
        if (webDavConfigDao.getActiveConfig() == null) {
            webDavConfigDao.getAll().firstOrNull()?.let {
                webDavConfigDao.setActive(it.id)
            }
        }
    }

    override suspend fun testConnection(config: WebDavConfig): Boolean {
        if (config.url.isBlank() || config.username.isBlank() || config.password.isBlank()) {
            return false
        }
        return connectionTester.testConnection(config)
    }

    private fun WebDavConfigEntity.toWebDavConfig() = WebDavConfig(
        id = id,
        name = name,
        url = url,
        username = username,
        password = password,
        rootPath = rootPath,
        isEnabled = isEnabled
    )

    private fun WebDavConfig.toEntity() = WebDavConfigEntity(
        id = id,
        name = name,
        url = url,
        username = username,
        password = password,
        rootPath = rootPath,
        isEnabled = isEnabled
    )
}
