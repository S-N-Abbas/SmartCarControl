package com.example.smartcarcontrol.domain.usecase

import com.example.smartcarcontrol.domain.repository.IBluetoothRepository
import javax.inject.Inject

class AutoConnectUseCase @Inject constructor(
    private val bluetoothRepository: IBluetoothRepository
) {
    suspend operator fun invoke(): Boolean {
        return bluetoothRepository.autoConnect()
    }
}