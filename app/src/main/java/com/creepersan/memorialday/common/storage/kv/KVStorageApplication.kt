package com.creepersan.memorialday.common.storage.kv

import com.creepersan.memorialday.common.definitions.AppMode
import com.creepersan.memorialday.module.storage.kv.KVDefinition
import com.creepersan.memorialday.module.storage.kv.KVType


val KVStorageApplication_AppMode = KVDefinition(
    module = "app",
    type = KVType.DataStore,
    key = "app_mode",
    defaultValue = AppMode.NotDecide,
)


val KVStorageApplication_AgreeServiceAgreementTime = KVDefinition(
    module = "app",
    type = KVType.DataStore,
    key = "agree_service_agreement_time",
    defaultValue = 0,
)

