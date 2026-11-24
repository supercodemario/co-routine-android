package com.diljith.android_coroutine.data.remote.dto.login

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val token: String,
    val user: User
)