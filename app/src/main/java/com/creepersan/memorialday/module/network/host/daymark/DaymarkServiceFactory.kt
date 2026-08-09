package com.creepersan.memorialday.module.network.host.daymark

import com.creepersan.memorialday.module.network.host.account.AccountDeviceInfoProvider
import com.creepersan.memorialday.module.network.host.account.AccountTokenStorage
import com.creepersan.memorialday.module.network.core.NetworkClientBuilder
import com.creepersan.memorialday.module.network.core.NetworkLogger
import okhttp3.OkHttpClient

interface DaymarkApiProvider {
    fun create(): DaymarkApi
}

class DaymarkServiceFactory(
    private val config: DaymarkServiceConfig,
    private val deviceInfoProvider: AccountDeviceInfoProvider,
    private val tokenStorage: AccountTokenStorage
) : DaymarkApiProvider {

    override fun create(): DaymarkApi {
        return NetworkClientBuilder()
            .baseUrl(config.baseUrl)
            .logger(NetworkLogger.LOGCAT)
            .okHttpClient(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder().apply {
                            headers().forEach { (name, value) -> header(name, value) }
                        }.build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .build()
            .createService(DaymarkApi::class.java)
    }

    private fun headers(): Map<String, String> = buildMap {
        put("CS-App", deviceInfoProvider.appName())
        put("CS-Device-Platform", deviceInfoProvider.platform())
        put("CS-Device-Code", deviceInfoProvider.deviceCode())
        put("CS-Device-Name", deviceInfoProvider.deviceName())
        put("CS-Version", deviceInfoProvider.versionCode())
        tokenStorage.current()?.accessToken?.let { put("CS-Token", it) }
    }
}
