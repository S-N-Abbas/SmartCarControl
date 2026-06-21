package com.example.smartcarcontrol.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.example.smartcarcontrol.domain.model.BluetoothDevice
import com.example.smartcarcontrol.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
@SuppressLint("MissingPermission")
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var bluetoothGatt: BluetoothGatt? = null
    private var esp32Characteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow(false)
    val connectionState = _connectionState.asStateFlow()

    private val _incomingData = MutableSharedFlow<String>()
    val incomingData = _incomingData.asSharedFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices = _scannedDevices.asStateFlow()

    private var isScanning = false

    fun startScan() {
        _scannedDevices.value = emptyList()
        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            android.util.Log.e("BleManager", "BLE Scanner is NULL — Bluetooth might be off")
            return
        }
        // Stop any existing scan first
        if (isScanning) {
            android.util.Log.d("BleManager", "Stopping existing scan before starting new one")
            scanner.stopScan(scanCallback)
            isScanning = false
        }
        android.util.Log.d("BleManager", "Starting BLE scan...")
        scanner.startScan(scanCallback)
        isScanning = true
        android.util.Log.d("BleManager", "Scan started successfully")
    }

    fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        isScanning = false
        android.util.Log.d("BleManager", "Scan stopped")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: return
            android.util.Log.d("BleManager", "Found device: $name — ${device.address}")
            val newDevice = BluetoothDevice(name, device.address)
            val current = _scannedDevices.value.toMutableList()
            if (current.none { it.address == device.address }) {
                current.add(newDevice)
                _scannedDevices.value = current
            }
        }

        override fun onScanFailed(errorCode: Int) {
            android.util.Log.e("BleManager", "Scan failed with error code: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = true
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = false
                    esp32Characteristic = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(UUID.fromString(Constants.ESP32_SERVICE_UUID))
            esp32Characteristic = service?.getCharacteristic(
                UUID.fromString(Constants.ESP32_CHARACTERISTIC_UUID)
            )
            // Enable notifications to receive ACK from ESP32
            esp32Characteristic?.let { char ->
                gatt.setCharacteristicNotification(char, true)
                val descriptor = char.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                )
                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value?.toString(Charsets.UTF_8) ?: return
            scope.launch {
                _incomingData.emit(data)
            }
        }
    }

    fun connect(device: BluetoothDevice): Boolean {
        return try {
            val btDevice = bluetoothAdapter.getRemoteDevice(device.address)
            bluetoothGatt = btDevice.connectGatt(context, false, gattCallback)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun sendData(data: String): Boolean {
        return try {
            esp32Characteristic?.let { char ->
                android.util.Log.d("BleManager", "Sending command: $data")
                char.value = data.toByteArray(Charsets.UTF_8)
                val result = bluetoothGatt?.writeCharacteristic(char) ?: false
                android.util.Log.d("BleManager", "Send result: $result")
                result
            } ?: run {
                android.util.Log.e("BleManager", "Characteristic is NULL — not connected?")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("BleManager", "Send error: ${e.message}")
            false
        }
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = false
    }
}