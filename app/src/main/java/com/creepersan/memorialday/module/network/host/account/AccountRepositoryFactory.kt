package com.creepersan.memorialday.module.network.host.account

import android.content.Context

object AccountRepositoryFactory {

    fun create(context: Context): AccountRepository {
        val storage = KVAccountTokenStorage()
        return AccountRepository(
            serviceFactory = AccountServiceFactory(
                config = AccountServiceConfig(),
                deviceInfoProvider = AndroidAccountDeviceInfoProvider(context.applicationContext),
                tokenStorage = storage
            ),
            tokenStorage = storage
        )
    }
}
