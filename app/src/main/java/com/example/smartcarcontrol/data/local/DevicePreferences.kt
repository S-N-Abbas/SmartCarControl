package com.example.smartcarcontrol.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "device_prefs")

@Singleton
class DevicePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_DEVICE_NAME = stringPreferencesKey("last_device_name")
        private val KEY_DEVICE_ADDRESS = stringPreferencesKey("last_device_address")
    }

    val savedDeviceName: Flow<String?> = context.dataStore.data
        .map { it[KEY_DEVICE_NAME] }

    val savedDeviceAddress: Flow<String?> = context.dataStore.data
        .map { it[KEY_DEVICE_ADDRESS] }

    suspend fun saveDevice(name: String, address: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEVICE_NAME] = name
            prefs[KEY_DEVICE_ADDRESS] = address
        }
        android.util.Log.d("DevicePrefs", "Saved device: $name — $address")
    }

    suspend fun clearDevice() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_DEVICE_NAME)
            prefs.remove(KEY_DEVICE_ADDRESS)
        }
        android.util.Log.d("DevicePrefs", "Cleared saved device")
    }
}