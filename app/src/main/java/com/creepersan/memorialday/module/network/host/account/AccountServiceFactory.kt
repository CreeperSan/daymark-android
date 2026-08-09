package com.creepersan.memorialday.module.network.host.account

import com.creepersan.memorialday.module.network.core.NetworkClientBuilder
import com.creepersan.memorialday.module.network.core.NetworkLogger
import okhttp3.OkHttpClient

/**
 * Creates account Retrofit services with the current device and authentication headers.
 *
 * A new service is intentionally created for each repository operation so a refreshed token is
 * used immediately and no stale credential is retained by a shared HTTP client.
 */
interface AccountApiProvider {
    fun create(): AccountApi

    /**
     * Refresh is the sole operation permitted to use an otherwise expired access token.
     */
    fun createForRefresh(): AccountApi = create()
}

class AccountServiceFactory(
    private val config: AccountServiceConfig,
    private val deviceInfoProvider: AccountDeviceInfoProvider,
    private val tokenStorage: AccountTokenStorage
) : AccountApiProvider {

    override fun create(): AccountApi {
        return create(includeExpiredToken = false)
    }

    override fun createForRefresh(): AccountApi {
        return create(includeExpiredToken = true)
    }

    private fun create(includeExpiredToken: Boolean): AccountApi {
        return NetworkClientBuilder()
            .baseUrl(config.baseUrl)
            .logger(NetworkLogger.LOGCAT)
            .okHttpClient(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder().apply {
                            headers(includeExpiredToken).forEach { (name, value) ->
                                header(name, value)
                            }
                        }.build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .build()
            .createService(AccountApi::class.java)
    }

    private fun headers(includeExpiredToken: Boolean): Map<String, String> {
        return buildMap {
            put("CS-App", deviceInfoProvider.appName())
            put("CS-Device-Platform", deviceInfoProvider.platform())
            put("CS-Device-Code", deviceInfoProvider.deviceCode())
            put("CS-Device-Name", deviceInfoProvider.deviceName())
            put("CS-Version", deviceInfoProvider.versionCode())
            val token = if (includeExpiredToken) {
                tokenStorage.accessTokenForRefresh()
            } else {
                tokenStorage.current()?.accessToken
            }
            token?.let { put("CS-Token", it) }
        }
    }
}
