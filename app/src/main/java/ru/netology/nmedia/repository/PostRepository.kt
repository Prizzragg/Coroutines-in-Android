package ru.netology.nmedia.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post
import java.io.File

interface PostRepository {
    val data: Flow<PagingData<Post>>

    fun getNewer(): Flow<Int>
    suspend fun getAll()
    suspend fun save(post: Post, photo: File?)
    suspend fun removeById(id: Long)
    suspend fun likeById(id: Long)
    suspend fun updatePosts()

    suspend fun signIn(login: String, password: String)

    suspend fun signUp(name: String, login: String, password: String)
}
