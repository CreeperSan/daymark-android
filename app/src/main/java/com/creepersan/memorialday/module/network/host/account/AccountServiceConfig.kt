package com.creepersan.memorialday.module.network.host.account

import com.creepersan.memorialday.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Account service endpoint configuration. A caller can supply a staging or test host without
 * changing the production default.
 */
data class AccountServiceConfig(
    val baseUrl: String = if (BuildConfig.USE_DEBUG_SERVICE) DEBUG_BASE_URL else PRODUCTION_BASE_URL
) {
    init {
        require(baseUrl.toHttpUrlOrNull() != null) { "Account service base URL is invalid" }
    }

    fun withBaseUrl(value: String): AccountServiceConfig = copy(baseUrl = value)

    companion object {
        const val PRODUCTION_BASE_URL = "https://account.creepersan.com/"
        const val DEBUG_BASE_URL = "http://account.test.com:8080/"
    }
}
