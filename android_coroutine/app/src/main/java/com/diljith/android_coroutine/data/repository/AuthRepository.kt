package com.diljith.android_coroutine.data.repository

import com.diljith.android_coroutine.data.remote.AuthApi
import com.diljith.android_coroutine.data.remote.dto.login.LoginResponse
import com.diljith.android_coroutine.data.remote.dto.login.request.LoginRequest
import com.diljith.android_coroutine.network.ApiResult
import com.diljith.android_coroutine.network.NetworkClient
import com.diljith.android_coroutine.network.safeApiCall
import javax.inject.Inject

class AuthRepository @Inject constructor() {

    private val api: AuthApi = NetworkClient.create()
    suspend fun login(username: String, password: String): ApiResult<LoginResponse> {
        return safeApiCall {
            api.login(
                LoginRequest(
                    email = username.trim(),
                    password = password
                )
            )
        }
    }
}
