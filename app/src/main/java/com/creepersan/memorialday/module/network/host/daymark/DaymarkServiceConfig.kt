package com.creepersan.memorialday.module.network.host.daymark

import com.creepersan.memorialday.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Immutable endpoint configuration for the Daymark service.
 */
data class DaymarkServiceConfig(
    val baseUrl: String = if (BuildConfig.USE_DEBUG_SERVICE) DEBUG_BASE_URL else PRODUCTION_BASE_URL
) {
    init {
        require(baseUrl.toHttpUrlOrNull() != null) { "Daymark service base URL is invalid" }
    }

    fun withBaseUrl(value: String): DaymarkServiceConfig = copy(baseUrl = value)

    companion object {
        const val PRODUCTION_BASE_URL = "https://daymark.creepersan.com/"
        const val DEBUG_BASE_URL = "http://daymark.test.com:8080/"
    }
}
