package com.example.smartcarcontrol.util

object Constants {
    // BLE
    const val ESP32_SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
    const val ESP32_CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
    const val BLE_SCAN_PERIOD = 10000L // 10 seconds

    // Commands sent to ESP32
    const val CMD_UNLOCK_DOOR = "UNLOCK_DOOR"
    const val CMD_LOCK_DOOR = "LOCK_DOOR"
    const val CMD_OPEN_WINDOW = "OPEN_WINDOW"
    const val CMD_CLOSE_WINDOW = "CLOSE_WINDOW"
    const val CMD_OPEN_TRUNK = "OPEN_TRUNK"
    const val CMD_CLOSE_TRUNK = "CLOSE_TRUNK"

    // ACK from ESP32
    const val ACK_SUCCESS = "OK"
    const val ACK_FAILURE = "FAIL"

    // Gemini
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    const val GEMINI_MODEL = "gemini-flash-latest"

    // Test Mode — set to false when ESP32 is ready
    const val TEST_MODE = false

    // Security
    const val AUTH_CHALLENGE_CHARACTERISTIC_UUID = "c8e4c1d2-f3a4-4b5c-8d6e-9f0a1b2c3d4e"
    const val AUTH_RESPONSE_CHARACTERISTIC_UUID  = "d9f5d2e3-a4b5-5c6d-9e7f-0a1b2c3d4e5f"
    const val AUTH_TIMEOUT_MS = 5000L // 5 seconds to authenticate
}