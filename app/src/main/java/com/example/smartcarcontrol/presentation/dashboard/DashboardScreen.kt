package com.example.smartcarcontrol.presentation.dashboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Window
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartcarcontrol.domain.model.CarCommand
import com.example.smartcarcontrol.domain.model.VoiceState
import com.example.smartcarcontrol.presentation.components.CommandButton
import com.example.smartcarcontrol.presentation.components.ResultSnackbar
import com.example.smartcarcontrol.presentation.components.StatusBar
import com.example.smartcarcontrol.presentation.components.VoiceButton
import java.util.Locale

data class CommandItem(
    val label: String,
    val command: CarCommand,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAITest: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Speech Recognizer
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(Unit) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        onDispose { speechRecognizer?.destroy() }
    }

    // Microphone Permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening(speechRecognizer, viewModel)
    }

    // Command Grid Items
    val commands = listOf(
        CommandItem("Unlock Door", CarCommand.UNLOCK_DOOR, Icons.Default.LockOpen),
        CommandItem("Lock Door", CarCommand.LOCK_DOOR, Icons.Default.Lock),
        CommandItem("Open Window", CarCommand.OPEN_WINDOW, Icons.Default.Window),
        CommandItem("Close Window", CarCommand.CLOSE_WINDOW, Icons.Default.Window),
        CommandItem("Open Trunk", CarCommand.OPEN_TRUNK, Icons.Default.DirectionsCar),
        CommandItem("Close Trunk", CarCommand.CLOSE_TRUNK, Icons.Default.DirectionsCar),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text("Smart Car Control")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAITest) {
                        Icon(
                            Icons.Default.Science,
                            contentDescription = "Test AI"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // BT Status
                StatusBar(isConnected = uiState.isConnected)

                Spacer(modifier = Modifier.height(16.dp))

                // Voice State Text
                Text(
                    text = when (val state = uiState.voiceState) {
                        is VoiceState.Idle -> "Tap mic to speak a command"
                        is VoiceState.Listening -> "Listening..."
                        is VoiceState.Recognized -> "\"${state.text}\""
                        is VoiceState.Error -> state.message
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (uiState.voiceState) {
                        is VoiceState.Error -> MaterialTheme.colorScheme.error
                        is VoiceState.Listening -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onBackground
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Command Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(commands) { item ->
                        CommandButton(
                            label = item.label,
                            icon = item.icon,
                            enabled = uiState.isConnected && !uiState.isLoading,
                            onClick = { viewModel.sendCommand(item.command) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Button
                VoiceButton(
                    voiceState = uiState.voiceState,
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Loading Overlay
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Result Snackbar
            ResultSnackbar(
                message = uiState.lastResult?.message,
                isSuccess = uiState.lastResult?.success == true,
                onDismiss = viewModel::clearResult
            )
        }
    }
}

private fun startListening(
    speechRecognizer: SpeechRecognizer?,
    viewModel: DashboardViewModel
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    speechRecognizer?.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            viewModel.onVoiceStateChanged(VoiceState.Listening)
        }
        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            viewModel.onVoiceCommandRecognized(text)
        }
        override fun onError(error: Int) {
            viewModel.onVoiceStateChanged(VoiceState.Error("Could not hear command. Try again."))
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    speechRecognizer?.startListening(intent)
}