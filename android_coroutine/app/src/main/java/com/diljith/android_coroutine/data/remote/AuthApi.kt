package com.diljith.android_coroutine.data.remote

import com.diljith.android_coroutine.data.remote.dto.login.LoginResponse
import com.diljith.android_coroutine.data.remote.dto.login.request.LoginRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}
