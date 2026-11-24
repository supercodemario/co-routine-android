package com.diljith.android_coroutine.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun AppButton(
    buttonText: String = "Click",
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ElevatedButton(
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        onClick = onClick
    ) {
        Text(buttonText)
    }

}

@Preview
@Composable
private fun AppButtonPrev() {

    AppButton(buttonText = "Press", onClick = {})

}