package com.example.smartcarcontrol.domain.usecase

import com.example.smartcarcontrol.domain.model.BluetoothDevice
import com.example.smartcarcontrol.domain.repository.IBluetoothRepository
import javax.inject.Inject

class ConnectToDeviceUseCase @Inject constructor(
    private val bluetoothRepository: IBluetoothRepository
) {
    suspend operator fun invoke(device: BluetoothDevice): Boolean {
        return bluetoothRepository.connectToDevice(device)
    }
}