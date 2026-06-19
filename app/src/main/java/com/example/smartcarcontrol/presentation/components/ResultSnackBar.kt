package com.example.smartcarcontrol.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ResultSnackbar(
    message: String?,
    isSuccess: Boolean,
    onDismiss: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            onDismiss()
        }
    }

    SnackbarHost(hostState = snackbarHostState) { data ->
        Snackbar(
            modifier = Modifier.padding(12.dp),
            containerColor = if (isSuccess) Color(0xFF1B5E20) else Color(0xFF7F0000),
            contentColor = Color.White,
            action = {
                TextButton(onClick = { data.dismiss() }) {
                    Text("OK", color = Color.White)
                }
            }
        ) {
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}