package com.tvplayer.core.repository.impl

import com.tvplayer.core.db.HomeGridDao
import com.tvplayer.core.db.HomeGridEntity
import com.tvplayer.core.model.HomeGridItem
import com.tvplayer.core.repository.HomeGridRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeGridRepositoryImpl @Inject constructor(
    private val homeGridDao: HomeGridDao
) : HomeGridRepository {

    override suspend fun getAll(): List<HomeGridItem> {
        return homeGridDao.getAll().map { it.toModel() }
    }

    override fun getAllFlow(): Flow<List<HomeGridItem>> {
        return homeGridDao.getAllFlow().map { list -> list.map { it.toModel() } }
    }

    override suspend fun getByPath(folderPath: String): HomeGridItem? {
        return homeGridDao.getByPath(folderPath)?.toModel()
    }

    override suspend fun add(folderPath: String, displayName: String): Long {
        val existing = homeGridDao.getByPath(folderPath)
        if (existing != null) return existing.id
        return homeGridDao.insert(HomeGridEntity(folderPath = folderPath, displayName = displayName))
    }

    override suspend fun remove(id: Long) {
        homeGridDao.deleteById(id)
    }

    private fun HomeGridEntity.toModel() = HomeGridItem(
        id = id,
        folderPath = folderPath,
        displayName = displayName,
        sortOrder = sortOrder,
        createTime = createTime
    )
}
