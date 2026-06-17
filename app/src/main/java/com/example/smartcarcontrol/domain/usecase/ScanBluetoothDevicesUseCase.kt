package com.example.smartcarcontrol.domain.usecase

import com.example.smartcarcontrol.domain.model.BluetoothDevice
import com.example.smartcarcontrol.domain.repository.IBluetoothRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanBluetoothDevicesUseCase @Inject constructor(
    private val bluetoothRepository: IBluetoothRepository
) {
    operator fun invoke(): Flow<List<BluetoothDevice>> {
        return bluetoothRepository.scanDevices()
    }
}