package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.model.AuthState
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val repository: PostRepository,
) : ViewModel() {

    private val _dataState = MutableLiveData<AuthState>()
    val dataState: LiveData<AuthState>
        get() = _dataState
    fun signIn(login: String, password: String) = viewModelScope.launch {
        try {
            repository.signIn(login, password)
            _dataState.value = AuthState(error = false, successfully = true)
            _dataState.value = AuthState(error = false, successfully = false)
        } catch (e: Exception) {
            _dataState.value = AuthState(error = true, successfully = false)
        }
    }
}