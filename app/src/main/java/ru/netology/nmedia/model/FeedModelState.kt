package ru.netology.nmedia.model

data class FeedModelState(
    val loading: Boolean = false,
    val error: Boolean = false,
    val refreshing: Boolean = false,
    val errorRemove: Boolean = false,
    val errorLike: Boolean = false,
    val id: Long = 0,
)
