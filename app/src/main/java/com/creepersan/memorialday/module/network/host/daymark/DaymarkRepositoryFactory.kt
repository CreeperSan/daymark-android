package com.creepersan.memorialday.module.network.host.daymark

import android.content.Context
import com.creepersan.memorialday.module.network.host.account.AndroidAccountDeviceInfoProvider
import com.creepersan.memorialday.module.network.host.account.KVAccountTokenStorage

object DaymarkRepositoryFactory {

    fun create(context: Context): DaymarkRepository {
        return DaymarkRepository(
            DaymarkServiceFactory(
                config = DaymarkServiceConfig(),
                deviceInfoProvider = AndroidAccountDeviceInfoProvider(context.applicationContext),
                tokenStorage = KVAccountTokenStorage()
            )
        )
    }
}
