package com.diljith.android_coroutine.screen

data class LoginUiState(
    val userName: String = "",
    val password: String = "",
    val userNameError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val isLoginSuccess: Boolean=false
)
