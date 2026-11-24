package com.diljith.android_coroutine.screen

object LoginValidator {

    fun validateUserName(userName: String): String? {
        return when {
            userName.isBlank() -> "User name is required"
            userName.trim().length < 3 -> "User name must be at least 3 characters"
            else -> null
        }
    }

    fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password is required"
            password.length < 8 -> "Password must be at least 8 characters"
            else -> null
        }
    }
}
