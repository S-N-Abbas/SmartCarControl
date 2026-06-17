package com.example.smartcarcontrol.domain.usecase

import com.example.smartcarcontrol.domain.model.CarCommand
import com.example.smartcarcontrol.domain.repository.IBluetoothRepository
import javax.inject.Inject

class SendCarCommandUseCase @Inject constructor(
    private val bluetoothRepository: IBluetoothRepository
) {
    suspend operator fun invoke(command: CarCommand): Boolean {
        return bluetoothRepository.sendCommand(command)
    }
}