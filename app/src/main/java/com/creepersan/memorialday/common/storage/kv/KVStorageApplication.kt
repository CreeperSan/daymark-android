package com.creepersan.memorialday.common.storage.kv

import com.creepersan.memorialday.common.definitions.AppMode
import com.creepersan.memorialday.module.storage.kv.KVDefinition
import com.creepersan.memorialday.module.storage.kv.KVType


private const val KVStorageApplication_Module = "app"

/* 应用模式 - 经典模式 or 服务器模式 */
val KVStorageApplication_AppMode = KVDefinition(
    module = KVStorageApplication_Module,
    type = KVType.DataStore,
    key = "app_mode",
    defaultValue = AppMode.NotDecide,
)

/* 同意隐私协议时间戳 */
val KVStorageApplication_AgreeServiceAgreementTime = KVDefinition(
    module = KVStorageApplication_Module,
    type = KVType.DataStore,
    key = "agree_service_agreement_time",
    defaultValue = 0,
)

/* 设备唯一ID */
val KVStorageApplication_DeviceID = KVDefinition (
    module = KVStorageApplication_Module,
    type = KVType.DataStore,
    key = "device_id",
    defaultValue = "",
)

