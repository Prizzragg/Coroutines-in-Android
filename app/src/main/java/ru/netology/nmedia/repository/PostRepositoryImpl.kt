package ru.netology.nmedia.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.IOException
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.DbError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import java.io.File
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val dao: PostDao,
    private val apiService: PostsApiService
) : PostRepository {

    @Inject
    lateinit var appAuth: AppAuth

    override val data = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = false),
        pagingSourceFactory = {
            PostPagingSource(
                apiService
            )
        }
    ).flow

    override suspend fun getAll() {
        try {
            val response = apiService.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun save(post: Post, photo: File?) {
        try {
            val media = photo?.let {
                upload(it)
            }
            val postWithAttachment = post.copy(attachment = media?.let {
                Attachment(url = it.id, type = AttachmentType.IMAGE)
            })
            val response = apiService.save(postWithAttachment)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    private suspend fun upload(file: File): Media =
        apiService.upload(
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody()
            )
        )

    override suspend fun removeById(id: Long) {
        val old = dao.findById(id)
        dao.removeById(id)
        try {
            val response = apiService.removeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            dao.insert(old)
            throw NetworkError
        } catch (e: Exception) {
            dao.insert(old)
            throw UnknownError
        }
    }

    override suspend fun likeById(id: Long) {
        val old = dao.findById(id)
        try {
            if (old.likedByMe) {
                dao.insert(old.copy(likedByMe = false, likes = old.likes - 1))
                val response = apiService.dislikeById(id)
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }
                val body = response.body() ?: throw ApiError(response.code(), response.message())
                dao.insert(post = PostEntity.fromDto(body))
            } else {
                dao.insert(old.copy(likedByMe = true, likes = old.likes + 1))
                val response = apiService.likeById(id)
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }
                val body = response.body() ?: throw ApiError(response.code(), response.message())
                dao.insert(post = PostEntity.fromDto(body))
            }
        } catch (e: IOException) {
            dao.insert(old)
            throw NetworkError
        } catch (e: Exception) {
            dao.insert(old)
            throw UnknownError
        }
    }


    override fun getNewer(): Flow<Int> = flow {
        while (true) {
            delay(10_000)
            val id = dao.howManyPosts()
            val response = apiService.getNewer(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            val result = body.toEntity()
            dao.insert(result.map { it.copy(isUpdated = 0) })
            emit(body.size)
        }
    }.catch { e ->
        throw AppError.from(e)
    }

    override suspend fun updatePosts() {
        try {
            return dao.updatePosts()
        } catch (e: DbError) {
            throw DbError
        }
    }

    override suspend fun signIn(login: String, password: String) {
        try {
            val response = apiService.updateUser(login, password)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            appAuth.setAuth(body.id, body.token)
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun signUp(
        name: String,
        login: String,
        password: String
    ) {
        try {
            val response = apiService.registerUser(login, password, name)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            appAuth.setAuth(body.id, body.token)
        } catch (e: Exception) {
            throw UnknownError
        }
    }


}
