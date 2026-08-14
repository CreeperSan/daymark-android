package com.creepersan.memorialday.store

import android.content.Context
import android.os.Build
import com.creepersan.memorialday.common.storage.kv.KVStorageApplication_DeviceID
import com.creepersan.memorialday.module.storage.kv.get
import com.creepersan.memorialday.module.storage.kv.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class DeviceInfoStore(context: Context) : BaseStore() {

    private val _deviceCodeStateFlow = MutableStateFlow("")
    val deviceCodeStateFlow = _deviceCodeStateFlow.asStateFlow()
    val deviceCode get() = deviceCodeStateFlow.value

    private val _deviceNameStateFlow = MutableStateFlow("")
    val deviceNameStateFlow = _deviceNameStateFlow.asStateFlow()
    val deviceName get() = deviceNameStateFlow.value

    private val _devicePlatformStateFlow = MutableStateFlow("")
    val devicePlatformStateFlow = _devicePlatformStateFlow.asStateFlow()
    val devicePlatform get() = devicePlatformStateFlow.value


    override suspend fun onInitialize() {
        // 初始化平台
        _devicePlatformStateFlow.update {
            "Android"
        }
        // 初始化设备名称
        _deviceNameStateFlow.update {
            buildDeviceName()
        }
        // 初始化设备唯一标识码
        _deviceCodeStateFlow.update {
            buildDeviceCode()
        }
    }

    private fun buildDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.trim()
        val model = Build.MODEL.trim().ifBlank { Build.DEVICE.trim() }

        return when {
            manufacturer.isBlank() -> model
            model.isBlank() -> manufacturer
            manufacturer.equals(model, ignoreCase = true) -> model
            else -> "$manufacturer $model"
        }
    }

    private fun buildDeviceCode(): String {
        val cacheDeviceID = KVStorageApplication_DeviceID.get()
        if (!cacheDeviceID.isBlank()) {
            return cacheDeviceID
        }

        val deviceID = UUID.randomUUID().toString()
        KVStorageApplication_DeviceID.set(deviceID)

        return deviceID
    }

}