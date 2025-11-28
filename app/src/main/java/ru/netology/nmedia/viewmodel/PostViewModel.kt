package ru.netology.nmedia.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File
import javax.inject.Inject

private val empty = Post(
    id = 0,
    content = "",
    author = "",
    authorId = 0,
    authorAvatar = "",
    likedByMe = false,
    likes = 0,
    published = ""
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val appAuth: AppAuth,
) : ViewModel() {
    val data: Flow<PagingData<Post>> = appAuth.data.flatMapLatest { token ->
        repository.data
            .map { posts ->
                posts.map { post ->
                    post.copy(ownedByMe = post.authorId == token?.id)
                }
            }
    }
        .flowOn(Dispatchers.Default)

    private val _photo = MutableLiveData<PhotoModel?>()
    val photo: LiveData<PhotoModel?>
        get() = _photo

    //val newerCount = data.flatMapLatest {
    //repository.getNewer()
    //.catch { _dataState.postValue(FeedModelState(error = true)) }
    //.flowOn(Dispatchers.Default)
    //}
    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    //init {
    //loadPosts()
    //}

    fun updatePhoto(uri: Uri, file: File) {
        _photo.value = PhotoModel(uri, file)
    }

    //fun loadPosts() = viewModelScope.launch {
    //try {
    //_dataState.value = FeedModelState(loading = true)
    //repository.getAll()
    //_dataState.value = FeedModelState()
    //} catch (e: Exception) {
    //_dataState.value = FeedModelState(error = true)
    //}
    //}

    //fun refreshPosts() = viewModelScope.launch {
    //try {
    //_dataState.value = FeedModelState(refreshing = true)
    //repository.getAll()
    //_dataState.value = FeedModelState()
    //} catch (e: Exception) {
    //_dataState.value = FeedModelState(error = true)
    //}
    //}

    fun save() {
        edited.value?.let {
            _postCreated.value = Unit
            viewModelScope.launch {
                try {
                    repository.save(it, photo.value?.file)
                    _dataState.value = FeedModelState()
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
        edited.value = empty
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun likeById(id: Long) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(errorLike = false)
            repository.likeById(id)
        } catch (e: Exception) {
            _dataState.value = FeedModelState(errorLike = true, id = id)
        }
    }

    fun removeById(id: Long) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(errorRemove = false)
            repository.removeById(id)
        } catch (e: Exception) {
            _dataState.value = FeedModelState(errorRemove = true, id = id)
        }
    }

    fun updatePosts() = viewModelScope.launch {
        try {
            repository.updatePosts()
        } catch (e: Exception) {
        }
    }

    fun removePhoto() {
        _photo.value == null
    }
}
