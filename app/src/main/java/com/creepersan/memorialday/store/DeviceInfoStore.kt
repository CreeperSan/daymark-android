package com.creepersan.memorialday.store

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.creepersan.memorialday.common.storage.kv.KVStorageApplication_DeviceID
import com.creepersan.memorialday.module.storage.kv.get
import com.creepersan.memorialday.module.storage.kv.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class DeviceInfoStore(context: Context) : BaseStore() {

    private val applicationContext = context.applicationContext

    private val _deviceCode = MutableStateFlow("")
    private val deviceCode = _deviceCode.asStateFlow()

    private val _deviceName = MutableStateFlow("")
    private val deviceName = _deviceName.asStateFlow()

    private val _devicePlatform = MutableStateFlow("")
    private val devicePlatform = _devicePlatform.asStateFlow()


    override suspend fun onInitialize() {
        // 初始化平台
        _devicePlatform.update {
            "Android"
        }
        // 初始化设备名称
        _deviceName.update {
            buildDeviceName()
        }
        // 初始化设备唯一标识码
        _deviceCode.update {
            getDeviceCode()
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

    private fun getDeviceCode(): String {
        val cacheDeviceID = KVStorageApplication_DeviceID.get()
        if (!cacheDeviceID.isBlank()) {
            return cacheDeviceID
        }

        val deviceID = UUID.randomUUID().toString()
        KVStorageApplication_DeviceID.set(deviceID)

        return deviceID
    }

}