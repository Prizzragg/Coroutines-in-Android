package ru.netology.nmedia.auth

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.PushToken
import ru.netology.nmedia.dto.Token

class AppAuth private constructor(context: Context) {
    companion object {
        private const val ID_KEY = "ID_KEY"
        private const val TOKEN_KEY = "TOKEN_KEY"
        private var INSTANCE: AppAuth? = null

        fun init(context: Context) {
            INSTANCE = AppAuth(context)
        }

        fun getInstance() = requireNotNull(INSTANCE) {
            "Need call init() first"
        }
    }

    private val prefs =
        context.applicationContext.getSharedPreferences("auth", Context.MODE_PRIVATE)

    private val _data: MutableStateFlow<Token?>

    init {
        val id = prefs.getLong(ID_KEY, 0L)
        val token = prefs.getString(TOKEN_KEY, null)
        if (id == 0L || token == null) {
            prefs.edit { clear() }
            _data = MutableStateFlow(null)
        } else {
            _data = MutableStateFlow(Token(id, token))
        }
        sendPushToken()
    }

    val data = _data.asStateFlow()

    fun sendPushToken(token: String? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                PostsApi.service.sendPushToken(PushToken(token ?: Firebase.messaging.token.await()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setAuth(id: Long, token: String) {
        prefs.edit {
            putLong(ID_KEY, id)
            putString(TOKEN_KEY, token)
        }
        _data.value = Token(id, token)
        sendPushToken()
    }

    fun removeAuth() {
        prefs.edit { clear() }
        _data.value = null
        sendPushToken()
    }
}