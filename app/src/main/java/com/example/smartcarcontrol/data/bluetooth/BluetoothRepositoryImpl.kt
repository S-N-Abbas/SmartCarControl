package com.example.smartcarcontrol.data.bluetooth

import com.example.smartcarcontrol.domain.model.BluetoothDevice
import com.example.smartcarcontrol.domain.model.CarCommand
import com.example.smartcarcontrol.domain.repository.IBluetoothRepository
import com.example.smartcarcontrol.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class BluetoothRepositoryImpl @Inject constructor(
    private val bleManager: BleManager
) : IBluetoothRepository {

    override val connectionState: Flow<Boolean> = bleManager.connectionState
    override val incomingData: Flow<String> = bleManager.incomingData

    override fun scanDevices(): Flow<List<BluetoothDevice>> = flow {
        bleManager.startScan()
        bleManager.scannedDevices.collect { emit(it) }
    }

    override suspend fun connectToDevice(device: BluetoothDevice): Boolean {
        return bleManager.connect(device)
    }

    override suspend fun sendCommand(command: CarCommand): Boolean {
        val commandString = when (command) {
            CarCommand.UNLOCK_DOOR -> Constants.CMD_UNLOCK_DOOR
            CarCommand.LOCK_DOOR -> Constants.CMD_LOCK_DOOR
            CarCommand.OPEN_WINDOW -> Constants.CMD_OPEN_WINDOW
            CarCommand.CLOSE_WINDOW -> Constants.CMD_CLOSE_WINDOW
            CarCommand.OPEN_TRUNK -> Constants.CMD_OPEN_TRUNK
            CarCommand.CLOSE_TRUNK -> Constants.CMD_CLOSE_TRUNK
            CarCommand.UNKNOWN -> return false
        }
        return bleManager.sendData(commandString)
    }

    override suspend fun disconnect() {
        bleManager.disconnect()
    }
}