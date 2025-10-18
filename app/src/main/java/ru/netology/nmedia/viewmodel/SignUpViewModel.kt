package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.model.AuthState
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl

class SignUpViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(context = application).postDao())

    private val _dataState = MutableLiveData<AuthState>()
    val dataState: LiveData<AuthState>
        get() = _dataState

    fun signUp(name: String, login: String, password: String) = viewModelScope.launch {
        try {
            repository.signUp(name, login, password)
            _dataState.value = AuthState(error = false, successfully = true)
            _dataState.value = AuthState(error = false, successfully = false)
        } catch (e: Exception) {
            _dataState.value = AuthState(error = true, successfully = false)
        }
    }
}