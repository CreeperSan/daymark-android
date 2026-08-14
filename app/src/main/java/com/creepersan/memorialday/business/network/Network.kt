package com.creepersan.memorialday.business.network

import com.creepersan.memorialday.BuildConfig
import com.creepersan.memorialday.business.network.middleware.HttpHeaderInfoMiddleware
import com.creepersan.memorialday.business.network.middleware.HttpTokenMiddleware
import com.creepersan.memorialday.module.http.core.NetworkClientConfig
import com.creepersan.memorialday.module.http.core.NetworkManager
import com.creepersan.memorialday.module.http.core.RequestCall
import com.creepersan.memorialday.module.http.request.NetworkRequest
import com.creepersan.memorialday.module.http.transport.OkHttpTransportEngine
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import kotlin.collections.mapOf


object Network {

    val gson by lazy { Gson() }

    val HostDaymark get() = if (BuildConfig.DEBUG) "http://daymark.test.com:8080/" else "https://daymark.creepersan.com/"
    val HostAccount get() = if (BuildConfig.DEBUG) "http://account.test.com:8080/" else "https://account.creepersan.com/"


    private val networkManager by lazy {
        NetworkManager(
            config = NetworkClientConfig(
                defaultHost = HostDaymark,
                defaultHeaders = mapOf(),
            ),
            transport = OkHttpTransportEngine(
                client = OkHttpClient.Builder().build(),
                baseHost = HostDaymark,
            ),
        ).apply {
            registerMiddleware(HttpHeaderInfoMiddleware)
            registerMiddleware(HttpTokenMiddleware)
        }
    }

    fun <T> request(request: NetworkRequest<T>) = networkManager.request(request)

}

fun <T> NetworkRequest<T>.request(): Flow<T> {
    return Network.request(this).result
}
