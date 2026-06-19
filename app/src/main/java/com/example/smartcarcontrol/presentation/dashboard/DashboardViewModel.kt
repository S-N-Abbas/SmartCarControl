package com.example.smartcarcontrol.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcarcontrol.domain.model.CarCommand
import com.example.smartcarcontrol.domain.model.CommandResult
import com.example.smartcarcontrol.domain.model.VoiceState
import com.example.smartcarcontrol.domain.repository.IBluetoothRepository
import com.example.smartcarcontrol.domain.usecase.ProcessVoiceCommandUseCase
import com.example.smartcarcontrol.domain.usecase.SendCarCommandUseCase
import com.example.smartcarcontrol.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sendCarCommandUseCase: SendCarCommandUseCase,
    private val processVoiceCommandUseCase: ProcessVoiceCommandUseCase,
    private val bluetoothRepository: IBluetoothRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeConnectionState()
        observeIncomingData()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            bluetoothRepository.connectionState.collect { connected ->
                _uiState.update { it.copy(isConnected = connected) }
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
                                message = if (success) "Command executed successfully" else "Command failed"
                            )
                        },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onVoiceCommandRecognized(text: String) {
        _uiState.update { it.copy(voiceState = VoiceState.Recognized(text), isLoading = true) }
        viewModelScope.launch {
            val command = processVoiceCommandUseCase(text)
            if (command == CarCommand.UNKNOWN) {
                _uiState.update {
                    it.copy(
                        voiceState = VoiceState.Error("Command not recognized: \"$text\""),
                        isLoading = false
                    )
                }
            } else {
                sendCommand(command)
            }
        }
    }

    fun sendCommand(command: CarCommand) {
        _uiState.update { it.copy(isLoading = true, lastCommand = command) }
        viewModelScope.launch {
            if (Constants.TEST_MODE) {
                // Simulate ESP32 response in test mode
                kotlinx.coroutines.delay(1000)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        lastResult = CommandResult(
                            command = command,
                            success = true,
                            message = "[TEST] ${command.name} executed successfully"
                        )
                    )
                }
                return@launch
            }

            val sent = sendCarCommandUseCase(command)
            if (!sent) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        lastResult = CommandResult(
                            command = command,
                            success = false,
                            message = "Failed to send command. Check Bluetooth connection."
                        )
                    )
                }
            }
            // If sent successfully, result will come via observeIncomingData()
        }
    }

    fun onVoiceStateChanged(state: VoiceState) {
        _uiState.update { it.copy(voiceState = state) }
    }

    fun clearResult() {
        _uiState.update { it.copy(lastResult = null) }
    }
}

data class DashboardUiState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val voiceState: VoiceState = VoiceState.Idle,
    val lastCommand: CarCommand? = null,
    val lastResult: CommandResult? = null
)