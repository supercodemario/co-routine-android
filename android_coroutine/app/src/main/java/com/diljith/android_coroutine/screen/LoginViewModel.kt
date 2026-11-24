package com.diljith.android_coroutine.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diljith.android_coroutine.data.repository.AuthRepository
import com.diljith.android_coroutine.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onUserNameChange(userName: String) {
        _uiState.update {
            it.copy(
                userName = userName,
                userNameError = LoginValidator.validateUserName(userName),
                message = null
            )
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = LoginValidator.validatePassword(password),
                message = null
            )
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return

        val userNameError = LoginValidator.validateUserName(state.userName)
        val passwordError = LoginValidator.validatePassword(state.password)

        _uiState.update {
            it.copy(
                userNameError = userNameError,
                passwordError = passwordError,
                message = null
            )
        }

        if (userNameError != null || passwordError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }

            when (val result = authRepository.login(state.userName, state.password)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Welcome ${result.data.user.name}"
                        )
                    }
                }

                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = result.message
                        )
                    }
                }
            }
        }
    }
}
