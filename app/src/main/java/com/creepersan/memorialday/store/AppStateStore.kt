package com.creepersan.memorialday.store

import com.creepersan.memorialday.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppStateStore : BaseStore() {

    private val _appStateFlow = MutableStateFlow("")
    val appStateFlow = _appStateFlow.asStateFlow()
    val app get() = appStateFlow.value

    private val _versionCodeStateFlow = MutableStateFlow(0)
    val versionCodeStateFlow = _versionCodeStateFlow.asStateFlow()
    val versionCode get() = versionCodeStateFlow.value

    private val _versionNameStateFlow = MutableStateFlow("")
    val versionNameStateFlow = _versionNameStateFlow.asStateFlow()
    val versionName get() = versionNameStateFlow.value

    override suspend fun onInitialize() {
        _appStateFlow.update { "Daymark" }
        _versionCodeStateFlow.update { BuildConfig.VERSION_CODE }
        _versionNameStateFlow.update { BuildConfig.VERSION_NAME }
    }

}