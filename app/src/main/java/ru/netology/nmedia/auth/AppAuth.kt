package ru.netology.nmedia.auth

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.dto.PushToken
import ru.netology.nmedia.dto.Token
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuth @Inject constructor(
    @ApplicationContext
    private val context: Context
) {
    private val idKey = "ID_KEY"
    private val tokenKey = "TOKEN_KEY"
    private val prefs =
        context.applicationContext.getSharedPreferences("auth", Context.MODE_PRIVATE)

    private val _data: MutableStateFlow<Token?>

    init {
        val id = prefs.getLong(idKey, 0L)
        val token = prefs.getString(tokenKey, null)
        if (id == 0L || token == null) {
            prefs.edit { clear() }
            _data = MutableStateFlow(null)
        } else {
            _data = MutableStateFlow(Token(id, token))
        }
        sendPushToken()
    }

    val data = _data.asStateFlow()

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface AppAuthEntryPoint {
        fun getApiService(): PostsApiService
    }

    fun sendPushToken(token: String? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val entryPoint =
                    EntryPointAccessors.fromApplication(context, AppAuthEntryPoint::class.java)
                entryPoint.getApiService()
                    .sendPushToken(PushToken(token ?: Firebase.messaging.token.await()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setAuth(id: Long, token: String) {
        prefs.edit {
            putLong(idKey, id)
            putString(tokenKey, token)
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