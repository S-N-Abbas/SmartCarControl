package com.example.smartcarcontrol.data.bluetooth

import com.example.smartcarcontrol.data.local.DevicePreferences
import com.example.smartcarcontrol.domain.model.BluetoothDevice
import com.example.smartcarcontrol.domain.model.CarCommand
import com.example.smartcarcontrol.domain.repository.IBluetoothRepository
import com.example.smartcarcontrol.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class BluetoothRepositoryImpl @Inject constructor(
    private val bleManager: BleManager,
    private val devicePreferences: DevicePreferences
) : IBluetoothRepository {

    override val connectionState: Flow<Boolean> = bleManager.connectionState
    override val incomingData: Flow<String> = bleManager.incomingData

    override fun scanDevices(): Flow<List<BluetoothDevice>> = flow {
        bleManager.startScan()
        bleManager.scannedDevices.collect { emit(it) }
    }

    override suspend fun connectToDevice(device: BluetoothDevice): Boolean {
        val success = bleManager.connect(device)
        if (success) {
            // Save device for auto-connect next time
            devicePreferences.saveDevice(device.name, device.address)
        }
        return success
    }

    override suspend fun sendCommand(command: CarCommand): Boolean {
        val commandString = when (command) {
            CarCommand.UNLOCK_DOOR  -> Constants.CMD_UNLOCK_DOOR
            CarCommand.LOCK_DOOR    -> Constants.CMD_LOCK_DOOR
            CarCommand.OPEN_WINDOW  -> Constants.CMD_OPEN_WINDOW
            CarCommand.CLOSE_WINDOW -> Constants.CMD_CLOSE_WINDOW
            CarCommand.OPEN_TRUNK   -> Constants.CMD_OPEN_TRUNK
            CarCommand.CLOSE_TRUNK  -> Constants.CMD_CLOSE_TRUNK
            CarCommand.UNKNOWN      -> return false
        }
        return bleManager.sendData(commandString)
    }

    override suspend fun disconnect() {
        bleManager.disconnect()
        devicePreferences.clearDevice()
    }

    override suspend fun getSavedDevice(): BluetoothDevice? {
        val name = devicePreferences.savedDeviceName.first() ?: return null
        val address = devicePreferences.savedDeviceAddress.first() ?: return null
        return BluetoothDevice(name, address)
    }

    override suspend fun autoConnect(): Boolean {
        val device = getSavedDevice() ?: return false
        android.util.Log.d("BtRepository", "Auto-connecting to saved device: ${device.name}")
        return bleManager.connect(device)
    }
}