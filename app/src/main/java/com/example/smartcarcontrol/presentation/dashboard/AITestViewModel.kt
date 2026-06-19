package com.example.smartcarcontrol.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcarcontrol.domain.model.CarCommand
import com.example.smartcarcontrol.domain.usecase.ProcessVoiceCommandUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AITestViewModel @Inject constructor(
    private val processVoiceCommandUseCase: ProcessVoiceCommandUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AITestUiState())
    val uiState = _uiState.asStateFlow()

    fun testCommand(input: String) {
        _uiState.update { it.copy(isLoading = true, result = null) }
        viewModelScope.launch {
            val command = processVoiceCommandUseCase(input)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    result = AITestResult(
                        input = input,
                        command = command.name,
                        isSuccess = command != CarCommand.UNKNOWN
                    )
                )
            }
        }
    }
}

data class AITestUiState(
    val isLoading: Boolean = false,
    val result: AITestResult? = null
)

data class AITestResult(
    val input: String,
    val command: String,
    val isSuccess: Boolean
)