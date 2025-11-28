package ru.netology.nmedia.otherModules

import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class FireBaseMessagingModule {

    @Singleton
    @Provides
    fun provideInstance(): FirebaseMessaging = FirebaseMessaging.getInstance()
}