package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import okio.IOException
import retrofit2.HttpException
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val service: PostsApiService,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val appDb: AppDb
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val result = when (loadType) {
                LoadType.REFRESH -> {
                    if (postRemoteKeyDao.max() == null) {
                        service.getLatest(state.config.initialLoadSize)
                    } else {
                        val id = postRemoteKeyDao.max() ?: return MediatorResult.Success(false)
                        service.getAfter(id, state.config.pageSize)
                    }
                }

                LoadType.APPEND -> {
                    val id = postRemoteKeyDao.min() ?: return MediatorResult.Success(false)
                    service.getBefore(id, state.config.pageSize)
                }

                LoadType.PREPEND -> {
                    return MediatorResult.Success(false)
                }
            }
            if (!result.isSuccessful) {
                throw HttpException(result)
            }
            val body = result.body() ?: throw ApiError(
                result.code(),
                result.message()
            )
            if (body.isEmpty()) {
                return MediatorResult.Success(false)
            }
            appDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        if (postRemoteKeyDao.max() == null) {
                            postRemoteKeyDao.clear()
                            postRemoteKeyDao.insert(
                                listOf(
                                    PostRemoteKeyEntity(
                                        PostRemoteKeyEntity.KeyType.AFTER,
                                        body.first().id,
                                    ),
                                    PostRemoteKeyEntity(
                                        PostRemoteKeyEntity.KeyType.BEFORE,
                                        body.last().id,
                                    ),
                                )
                            )
                            postDao.clear()
                        } else {
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.AFTER,
                                    body.first().id
                                )
                            )
                        }
                    }

                    LoadType.PREPEND -> {}
                    LoadType.APPEND -> {
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                PostRemoteKeyEntity.KeyType.BEFORE,
                                body.last().id
                            )
                        )
                    }
                }
                postDao.insert(body.toEntity())
            }
            return MediatorResult.Success(false)
        } catch (e: IOException) {
            return MediatorResult.Error(e)
        }
    }


}