package com.creepersan.memorialday.activity.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.creepersan.memorialday.module.network.host.account.AccountRepositoryFactory
import com.creepersan.memorialday.module.network.host.account.AccountResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
			_uiState.update { it.copy(loading = true, error = null) }
			when (val result = repository.login(state.account, state.password)) {
				is AccountResult.Success -> {
					_uiState.update { it.copy(loading = false) }
					_events.emit(LoginEvent.Succeeded(result.data.account ?: state.account))
				}

				is AccountResult.Failure -> {
					_uiState.update {
						it.copy(loading = false, error = result.error.message)
					}
				}
			}
		}
	}

	private companion object {
		val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
	}

}