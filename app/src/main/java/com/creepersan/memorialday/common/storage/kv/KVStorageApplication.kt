package com.creepersan.memorialday.common.storage.kv

import com.creepersan.memorialday.common.definitions.ApplicationMode
import com.creepersan.memorialday.module.storage.kv.KVDefinition
import com.creepersan.memorialday.module.storage.kv.KVType


val KVStorageApplication_AppMode = KVDefinition(
    module = "app",
    type = KVType.DataStore,
    key = "app_mode",
    defaultValue = ApplicationMode.NotDecide,
)

