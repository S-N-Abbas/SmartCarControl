package com.example.smartcarcontrol.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcarcontrol.domain.model.BluetoothDevice
import com.example.smartcarcontrol.domain.usecase.ConnectToDeviceUseCase
import com.example.smartcarcontrol.domain.usecase.ScanBluetoothDevicesUseCase
import com.example.smartcarcontrol.domain.repository.IBluetoothRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val scanBluetoothDevicesUseCase: ScanBluetoothDevicesUseCase,
    private val connectToDeviceUseCase: ConnectToDeviceUseCase,
    private val bluetoothRepository: IBluetoothRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeConnectionState()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            bluetoothRepository.connectionState.collect { connected ->
                _uiState.update { it.copy(isConnected = connected) }
            }
        }
    }

    fun startScan() {
        _uiState.update { it.copy(isScanning = true, devices = emptyList()) }
        viewModelScope.launch {
            scanBluetoothDevicesUseCase().collect { devices ->
                _uiState.update { it.copy(devices = devices) }
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        _uiState.update { it.copy(isConnecting = true, selectedDevice = device) }
        viewModelScope.launch {
            val success = connectToDeviceUseCase(device)
            _uiState.update {
                it.copy(
                    isConnecting = false,
                    isScanning = false,
                    errorMessage = if (!success) "Failed to connect to ${device.name}" else null
                )
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            bluetoothRepository.disconnect()
            _uiState.update { it.copy(selectedDevice = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onPermissionDenied() {
        _uiState.update {
            it.copy(
                isScanning = false,
                errorMessage = "Bluetooth permission denied. Please grant permission in Settings."
            )
        }
    }
}

data class SettingsUiState(
    val isConnected: Boolean = false,
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false,
    val devices: List<BluetoothDevice> = emptyList(),
    val selectedDevice: BluetoothDevice? = null,
    val errorMessage: String? = null
)