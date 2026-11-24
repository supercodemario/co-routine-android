package com.diljith.android_coroutine.data.remote.dto.login.request

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)