package com.example.smartcarcontrol.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcarcontrol.domain.model.CarCommand
import com.example.smartcarcontrol.domain.model.CommandResult
import com.example.smartcarcontrol.domain.model.GeminiResult
import com.example.smartcarcontrol.domain.model.VoiceState
import com.example.smartcarcontrol.domain.repository.IBluetoothRepository
import com.example.smartcarcontrol.domain.usecase.AutoConnectUseCase
import com.example.smartcarcontrol.domain.usecase.ProcessVoiceCommandUseCase
import com.example.smartcarcontrol.domain.usecase.SendCarCommandUseCase
import com.example.smartcarcontrol.util.Constants
import com.example.smartcarcontrol.util.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sendCarCommandUseCase: SendCarCommandUseCase,
    private val processVoiceCommandUseCase: ProcessVoiceCommandUseCase,
    private val bluetoothRepository: IBluetoothRepository,
    private val autoConnectUseCase: AutoConnectUseCase,
    private val tts: TextToSpeechManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeConnectionState()
        observeIncomingData()
        startAutoConnectLoop()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            bluetoothRepository.connectionState.collect { connected ->
                _uiState.update { it.copy(isConnected = connected) }
                if (connected) {
                    android.util.Log.d("DashboardVM", "Connected — auto-connect loop will pause")
                } else {
                    android.util.Log.d("DashboardVM", "Disconnected — auto-connect loop will resume")
                }
            }
        }
    }

    private fun observeIncomingData() {
        viewModelScope.launch {
            bluetoothRepository.incomingData.collect { data ->
                val success = data.trim().uppercase() == Constants.ACK_SUCCESS
                _uiState.update { state ->
                    state.copy(
                        lastResult = state.lastCommand?.let { cmd ->
                            CommandResult(
                                command = cmd,
                                success = success,
                                message = if (success) "${cmd.name.replace("_", " ")} executed successfully"
                                else "Command failed"
                            )
                        },
                        isLoading = false
                    )
                }
            }
        }
    }

    private var autoConnectJob: Job? = null

    private fun startAutoConnectLoop() {
        autoConnectJob?.cancel()
        autoConnectJob = viewModelScope.launch {
            while (true) {
                if (!_uiState.value.isConnected) {
                    android.util.Log.d("DashboardVM", "Not connected. Trying auto-connect...")
                    autoConnectUseCase()
                } else {
                    android.util.Log.d("DashboardVM", "Already connected. Skipping auto-connect.")
                }
                delay(10000L) // Retry every 10 seconds
            }
        }
    }

    fun onVoiceInputReceived(text: String) {
        _uiState.update {
            it.copy(
                voiceState = VoiceState.Recognized(text),
                isLoading = true,
                chatReply = null
            )
        }
        viewModelScope.launch {
            when (val result = processVoiceCommandUseCase(text)) {
                is GeminiResult.Command -> {
                    if (result.carCommand == CarCommand.UNKNOWN) {
                        val reply = "Sorry, I didn't recognize that car command."
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                voiceState = VoiceState.Error(reply)
                            )
                        }
                        tts.speak(reply)
                    } else {
                        sendCommand(result.carCommand)
                    }
                }
                is GeminiResult.ChatReply -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            voiceState = VoiceState.Idle,
                            chatReply = result.message
                        )
                    }
                    tts.speak(result.message)
                }
            }
        }
    }

    fun sendCommand(command: CarCommand) {
        _uiState.update { it.copy(isLoading = true, lastCommand = command) }
        viewModelScope.launch {
            val sent = sendCarCommandUseCase(command)
            if (!sent) {
                val error = "Failed to send command. Check Bluetooth connection."
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        lastResult = CommandResult(
                            command = command,
                            success = false,
                            message = error
                        )
                    )
                }
                tts.speak(error)
            }
        }
    }

    fun onVoiceStateChanged(state: VoiceState) {
        _uiState.update { it.copy(voiceState = state) }
    }

    fun clearResult() {
        _uiState.update { it.copy(lastResult = null) }
    }

    fun clearChatReply() {
        _uiState.update { it.copy(chatReply = null) }
    }

    override fun onCleared() {
        super.onCleared()
        tts.shutdown()
    }
}

data class DashboardUiState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val voiceState: VoiceState = VoiceState.Idle,
    val lastCommand: CarCommand? = null,
    val lastResult: CommandResult? = null,
    val chatReply: String? = null
)