package com.creepersan.memorialday.activity.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creepersan.memorialday.common.storage.kv.KVStorageApplication_AgreeServiceAgreementTime
import com.creepersan.memorialday.module.storage.kv.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IntroViewModel : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(IntroUIState(
        hasAgreeServiceAgreement = false,
    ))

    val uiStateFlow = _uiStateFlow.asStateFlow()


    init {

        viewModelScope.launch {
            var hasAgreeServiceAgreement = false
            flow {
                /* 状态获取 */
                val agreeServiceAgreementTime = KVStorageApplication_AgreeServiceAgreementTime.get()
                hasAgreeServiceAgreement = agreeServiceAgreementTime > 0
                emit(Unit)
            }.onEach {
                /* 更新状态 */
                _uiStateFlow.update {
                    it.copy(
                        hasAgreeServiceAgreement = hasAgreeServiceAgreement,
                    )
                }
            }.launchIn(viewModelScope)
        }
    }


    override fun onCleared() {
        super.onCleared()
    }


    fun onTapServiceAgreementUrl() {

    }


    fun onToggleAgreeServiceAgreement(checked: Boolean) {
        _uiStateFlow.update {
            it.copy(
                hasAgreeServiceAgreement = checked,
            )
        }
    }

}