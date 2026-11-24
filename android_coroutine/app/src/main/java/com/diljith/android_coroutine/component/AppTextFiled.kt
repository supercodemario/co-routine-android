package com.diljith.android_coroutine.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AppTextField(value: String, labelText: String = "", isError : Boolean, onResult: (String) -> Unit) {
    OutlinedTextField(
        isError = isError,
        modifier = Modifier.fillMaxWidth(),
        // 1. Set the curved corners
        shape = RoundedCornerShape(16.dp),
        // 2. Set the background (container) and border colors
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF3F4F6),
            unfocusedContainerColor = Color(0xFFF3F4F6),
            disabledContainerColor = Color(0xFFE5E7EB),
            focusedBorderColor = Color(0xFF6200EE),
            unfocusedBorderColor = Color(0xFFCCCCCC),
        ),
        //modifier = Modifier.background(),
        label = { Text(labelText) },
        value = value,
        onValueChange = onResult
    )

}

@Preview
@Composable
private fun AppTextFieldPrev() {
    AppTextField("", labelText = "Text Label", isError = false, onResult = {})
}