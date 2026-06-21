package com.example.smartcarcontrol.domain.repository

import com.example.smartcarcontrol.domain.model.BluetoothDevice
import com.example.smartcarcontrol.domain.model.CarCommand
import kotlinx.coroutines.flow.Flow

interface IBluetoothRepository {
    val connectionState: Flow<Boolean>
    val incomingData: Flow<String>
    fun scanDevices(): Flow<List<BluetoothDevice>>
    suspend fun connectToDevice(device: BluetoothDevice): Boolean
    suspend fun sendCommand(command: CarCommand): Boolean
    suspend fun disconnect()
    suspend fun getSavedDevice(): BluetoothDevice?
    suspend fun autoConnect(): Boolean
}