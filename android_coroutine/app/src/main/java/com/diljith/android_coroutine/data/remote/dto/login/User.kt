package com.diljith.android_coroutine.data.remote.dto.login

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val email: String,
    val id: Int,
    val name: String
)