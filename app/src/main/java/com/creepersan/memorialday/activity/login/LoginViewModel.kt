package com.creepersan.memorialday.activity.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.creepersan.memorialday.application.DaymarkApplication
import com.creepersan.memorialday.business.network.Network
import com.creepersan.memorialday.business.network.prefab.login
import com.creepersan.memorialday.module.log.Logger
import com.creepersan.memorialday.module.network.host.account.AccountRepositoryFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
	val account: String = "",
	val password: String = "",
	val loginMethod: LoginMethod = LoginMethod.Account,
	val error: String? = null,
	val loading: Boolean = false,
)

enum class LoginMethod {
	Account,
	Email,
}

sealed interface LoginEvent {
	data class Succeeded(val account: String) : LoginEvent
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

	private val repository = AccountRepositoryFactory.create(application.applicationContext)
	private val _uiState = MutableStateFlow(LoginUiState())
	val uiState = _uiState.asStateFlow()

	private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = 1)
	val events = _events.asSharedFlow()

	fun initializeAccount(account: String) {
		if (_uiState.value.account.isEmpty() && account.isNotEmpty()) {
			onAccountChanged(account)
		}
	}

	fun onAccountChanged(account: String) {
		_uiState.update {
			it.copy(
				account = account,
				loginMethod = account.trim().takeIf(EMAIL_REGEX::matches)
					?.let { LoginMethod.Email } ?: LoginMethod.Account,
				error = null,
			)
		}
	}

	fun onPasswordChanged(password: String) {
		_uiState.update { it.copy(password = password, error = null) }
	}

	fun login() {
		val state = _uiState.value
		if (state.loading) return

		viewModelScope.launch {
			Logger.info("LoginViewModel", "开始登录")
			DaymarkApplication.instance.userStore.actionLogin(
				account = if (state.loginMethod == LoginMethod.Account) state.account else "",
				email = if (state.loginMethod == LoginMethod.Email) state.account else "",
				password = state.password,
			).map { response ->
				_events.emit(LoginEvent.Succeeded(response.user.account))
				Logger.info("LoginViewModel", "登录成功，账号：${response.user.account}")
			}.catch {
				Logger.info("LoginViewModel", "登录失败: ${it.message}")
			}.collect{

			}
		}
	}

	private companion object {
		val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
	}

}