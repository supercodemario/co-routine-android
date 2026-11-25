package com.diljith.android_coroutine.screen


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.rounded.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diljith.android_coroutine.component.AppButton
import com.diljith.android_coroutine.component.AppTextField

@Composable
fun LoginScreen(
    padding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isLoginSuccess) {
        if (state.isLoginSuccess) {
            onLoginSuccess()
        }
    }

    LoginContent(
        padding = padding,
        modifier = modifier,
        state = state,
        onUserNameChange = viewModel::onUserNameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLogin = viewModel::login
    )
}

@Composable
private fun LoginContent(
    padding: PaddingValues,
    modifier: Modifier,
    state: LoginUiState,
    onUserNameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit
) {


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.fillMaxHeight(0.2f))

        // App logo
        Icon(
            modifier = modifier
                .height(150.dp)
                .width(150.dp),
            imageVector = Icons.Rounded.Face,
            contentDescription = "Star Icon"
        )
        Spacer(modifier = modifier.height(20.dp))
        AppTextField(
            labelText = "User Name",
            value = state.userName,
            isError = state.userNameError != null,
            onResult = onUserNameChange
        )
        state.userNameError?.let { error ->
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = modifier.height(10.dp))
        AppTextField(
            labelText = "Password",
            value = state.password,
            isError = state.passwordError != null,
            onResult = onPasswordChange
        )
        state.passwordError?.let { error ->
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = modifier.height(26.dp))
        AppButton(
            onClick = onLogin,
            buttonText = if (state.isLoading) "Logging in..." else "Login",
            enabled = !state.isLoading
        )
        state.message?.let { message ->
            Spacer(modifier = modifier.height(16.dp))
            Text(text = message)
        }

    }

}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPrev() {
    LoginContent(
        modifier = Modifier,
        padding = PaddingValues(0.dp),
        state = LoginUiState(),
        onUserNameChange = {},
        onPasswordChange = {},
        onLogin = {}
    )

}
