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
import com.example.smartcarcontrol.BuildConfig
import com.example.smartcarcontrol.domain.model.BluetoothDevice
import com.example.smartcarcontrol.util.Constants
import com.example.smartcarcontrol.util.HmacHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("MissingPermission")
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) {
    private var bluetoothGatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var authResponseCharacteristic: BluetoothGattCharacteristic? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow(false)
    val connectionState = _connectionState.asStateFlow()

    private val _authState = MutableStateFlow(AuthState.UNAUTHENTICATED)
    val authState = _authState.asStateFlow()

    private val _incomingData = MutableSharedFlow<String>()
    val incomingData = _incomingData.asSharedFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices = _scannedDevices.asStateFlow()

    private var isScanning = false

    enum class AuthState {
        UNAUTHENTICATED,
        AUTHENTICATING,
        AUTHENTICATED,
        FAILED
    }

    // ─── Scan Callback ────────────────────────────────────────────
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

    // ─── GATT Callback ────────────────────────────────────────────
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    android.util.Log.d("BleManager", "BLE Connected. Discovering services...")
                    _authState.value = AuthState.UNAUTHENTICATED
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    android.util.Log.d("BleManager", "BLE Disconnected.")
                    _connectionState.value = false
                    _authState.value = AuthState.UNAUTHENTICATED
                    commandCharacteristic = null
                    authResponseCharacteristic = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(UUID.fromString(Constants.ESP32_SERVICE_UUID))
            if (service == null) {
                android.util.Log.e("BleManager", "Service not found! Wrong device?")
                gatt.disconnect()
                return
            }

            // Get command characteristic
            commandCharacteristic = service.getCharacteristic(
                UUID.fromString(Constants.ESP32_CHARACTERISTIC_UUID)
            )

            // Get auth challenge characteristic and enable notifications
            val challengeChar = service.getCharacteristic(
                UUID.fromString(Constants.AUTH_CHALLENGE_CHARACTERISTIC_UUID)
            )

            // Get auth response characteristic
            authResponseCharacteristic = service.getCharacteristic(
                UUID.fromString(Constants.AUTH_RESPONSE_CHARACTERISTIC_UUID)
            )

            if (challengeChar == null || authResponseCharacteristic == null || commandCharacteristic == null) {
                android.util.Log.e("BleManager", "Auth characteristics not found!")
                gatt.disconnect()
                return
            }

            // Enable notifications on challenge characteristic
            gatt.setCharacteristicNotification(challengeChar, true)
            val descriptor = challengeChar.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            )
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)

            android.util.Log.d("BleManager", "Services discovered. Waiting for auth challenge...")
            _authState.value = AuthState.AUTHENTICATING
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value?.toString(Charsets.UTF_8) ?: return

            when (characteristic.uuid.toString()) {

                // ESP32 sent us a challenge — sign it and respond
                Constants.AUTH_CHALLENGE_CHARACTERISTIC_UUID -> {
                    android.util.Log.d("BleManager", "Received auth challenge: $data")
                    handleAuthChallenge(gatt, data)
                }

                // ESP32 sent command ACK
                Constants.ESP32_CHARACTERISTIC_UUID -> {
                    android.util.Log.d("BleManager", "Received ACK: $data")
                    scope.launch { _incomingData.emit(data) }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            when (characteristic.uuid.toString()) {
                Constants.AUTH_RESPONSE_CHARACTERISTIC_UUID -> {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        android.util.Log.d("BleManager", "Auth response sent successfully")
                    } else {
                        android.util.Log.e("BleManager", "Auth response write failed: $status")
                        _authState.value = AuthState.FAILED
                    }
                }
            }
        }
    }

    // ─── Handle Auth Challenge ────────────────────────────────────
    private fun handleAuthChallenge(gatt: BluetoothGatt, challenge: String) {
        try {
            // Sign the challenge with our secret key
            val signature = HmacHelper.sign(challenge, BuildConfig.AUTH_SECRET_KEY)
            android.util.Log.d("BleManager", "Sending auth response: $signature")

            // Send signature back to ESP32
            authResponseCharacteristic?.let { char ->
                char.value = signature.toByteArray(Charsets.UTF_8)
                gatt.writeCharacteristic(char)
            }
        } catch (e: Exception) {
            android.util.Log.e("BleManager", "Auth signing failed: ${e.message}")
            _authState.value = AuthState.FAILED
            gatt.disconnect()
        }
    }

    // ─── Handle Auth Result from ESP32 ───────────────────────────
    fun onAuthResult(success: Boolean) {
        if (success) {
            android.util.Log.d("BleManager", "Authentication SUCCESSFUL")
            _authState.value = AuthState.AUTHENTICATED
            _connectionState.value = true
        } else {
            android.util.Log.e("BleManager", "Authentication FAILED")
            _authState.value = AuthState.FAILED
            _connectionState.value = false
            bluetoothGatt?.disconnect()
        }
    }

    // ─── Scan ─────────────────────────────────────────────────────
    fun startScan() {
        _scannedDevices.value = emptyList()
        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            android.util.Log.e("BleManager", "BLE Scanner is NULL")
            return
        }
        if (isScanning) {
            scanner.stopScan(scanCallback)
            isScanning = false
        }
        android.util.Log.d("BleManager", "Starting BLE scan...")
        scanner.startScan(scanCallback)
        isScanning = true
    }

    fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        isScanning = false
    }

    // ─── Connect ──────────────────────────────────────────────────
    fun connect(device: BluetoothDevice): Boolean {
        return try {
            val btDevice = bluetoothAdapter.getRemoteDevice(device.address)
            bluetoothGatt = btDevice.connectGatt(context, false, gattCallback)
            true
        } catch (e: Exception) {
            android.util.Log.e("BleManager", "Connect error: ${e.message}")
            false
        }
    }

    // ─── Send Command (only if authenticated) ────────────────────
    fun sendData(data: String): Boolean {
        if (_authState.value != AuthState.AUTHENTICATED) {
            android.util.Log.e("BleManager", "Cannot send — not authenticated!")
            return false
        }
        return try {
            commandCharacteristic?.let { char ->
                android.util.Log.d("BleManager", "Sending command: $data")
                char.value = data.toByteArray(Charsets.UTF_8)
                val result = bluetoothGatt?.writeCharacteristic(char) ?: false
                android.util.Log.d("BleManager", "Send result: $result")
                result
            } ?: run {
                android.util.Log.e("BleManager", "Command characteristic is NULL")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("BleManager", "Send error: ${e.message}")
            false
        }
    }

    // ─── Disconnect ───────────────────────────────────────────────
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = false
        _authState.value = AuthState.UNAUTHENTICATED
    }
}