package com.creepersan.memorialday.activity.login

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.creepersan.memorialday.R
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.constance.LEGAL_DOCUMENT_URL
import com.creepersan.memorialday.module.user.UserManager
import com.creepersan.memorialday.ui.glass.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.delay

private const val MIN_PASSWORD_LENGTH = 8
private const val RESEND_INTERVAL_MS = 60_000L
private const val SECOND_MS = 1_000L

/** 注册或重置密码时保留表单与请求状态。 */
class AccountViewModel : ViewModel() {
    var account by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var code by mutableStateOf("")
    var agreed by mutableStateOf(false)
    var verification by mutableStateOf(false)
    var codeSentAt by mutableStateOf(0L); private set
    var loading by mutableStateOf(false)
    var done by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    private var request: Disposable? = null

    /** 请求验证码，成功后进入确认页面。 */
    fun sendCode(register: Boolean) {
        if (loading || email.isBlank() || (register && (account.isBlank() || password.length < MIN_PASSWORD_LENGTH || !agreed))) return
        loading = true; error = null
        request = UserManager.requestCode(email.trim(), register).observeOn(AndroidSchedulers.mainThread()).subscribe({
            loading = false; codeSentAt = SystemClock.elapsedRealtime(); verification = true
        }, { loading = false; error = it.message })
    }

    /** 将验证码和表单一起交给服务端验证。 */
    fun submit(register: Boolean) {
        if (loading || code.isBlank() || password.length < MIN_PASSWORD_LENGTH) return
        loading = true; error = null
        val action = if (register) UserManager.register(account.trim(), email.trim(), password, code.trim()) else UserManager.resetPassword(email.trim(), password, code.trim())
        request = action.observeOn(AndroidSchedulers.mainThread()).subscribe({
            loading = false; password = ""; code = ""; done = true
        }, { loading = false; error = it.message })
    }

    override fun onCleared() { request?.dispose() }
}

/** 注册、找回密码及验证码确认页面。 */
class AccountActivity : BaseComposeActivity() {
    companion object { const val EXTRA_REGISTER = "register" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val register = intent.getBooleanExtra(EXTRA_REGISTER, false)
        val model = ViewModelProvider(this)[AccountViewModel::class.java]
        setContent {
            var resendSeconds by remember { mutableStateOf(0L) }
            LaunchedEffect(model.codeSentAt) {
                do {
                    resendSeconds = ((RESEND_INTERVAL_MS - (SystemClock.elapsedRealtime() - model.codeSentAt)).coerceAtLeast(0L) + SECOND_MS - 1) / SECOND_MS
                    if (resendSeconds > 0) delay(SECOND_MS)
                } while (resendSeconds > 0)
            }
            BackHandler(enabled = model.verification && !model.done) { model.verification = false }
            GlassPage {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text("‹", fontSize = 32.sp, modifier = Modifier.clickable { if (model.verification && !model.done) model.verification = false else finish() })
                    Spacer(Modifier.height(28.dp))
                    Text(if (model.done) getString(R.string.account_done) else if (model.verification) getString(R.string.account_enter_code) else if (register) getString(R.string.account_create) else getString(R.string.account_reset), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(if (model.done) getString(R.string.account_done_description) else if (model.verification) getString(R.string.account_code_sent, model.email) else if (register) getString(R.string.account_register_description) else getString(R.string.account_reset_description), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Spacer(Modifier.height(30.dp))
                    if (model.done) {
                        GlassButton(getString(R.string.account_back_login), { finish() })
                    } else {
                        GlassCard {
                            if (model.verification) {
                                GlassTextField(model.code, { model.code = it }, getString(R.string.account_email_code))
                                if (!register) {
                                    Spacer(Modifier.height(12.dp))
                                    GlassTextField(model.password, { model.password = it }, getString(R.string.account_new_password), password = true)
                                }
                                Spacer(Modifier.height(12.dp))
                                GlassButton(if (model.loading) getString(R.string.account_wait) else getString(R.string.account_confirm), { model.submit(register) }, !model.loading && model.code.isNotBlank() && model.password.length >= MIN_PASSWORD_LENGTH)
                                Text(if (resendSeconds > 0) getString(R.string.account_resend_wait, resendSeconds) else getString(R.string.account_resend), color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(enabled = !model.loading && resendSeconds == 0L) { model.sendCode(register) }.padding(top = 12.dp))
                            } else {
                                if (register) {
                                    GlassTextField(model.account, { model.account = it }, getString(R.string.account))
                                    Spacer(Modifier.height(12.dp))
                                }
                                GlassTextField(model.email, { model.email = it }, getString(R.string.account_email))
                                if (register) {
                                    Spacer(Modifier.height(12.dp))
                                    GlassTextField(model.password, { model.password = it }, getString(R.string.account_set_password), password = true)
                                }
                                if (register) {
                                    Spacer(Modifier.height(12.dp))
                                    Row(Modifier.fillMaxWidth()) {
                                        Text(if (model.agreed) "☑" else "□", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { model.agreed = !model.agreed }.padding(end = 6.dp))
                                        val accent = MaterialTheme.colorScheme.primary
                                        val agreement = buildAnnotatedString {
                                            append(getString(R.string.agreement_prefix))
                                            pushStringAnnotation("legal", LEGAL_DOCUMENT_URL)
                                            withStyle(SpanStyle(color = accent)) { append(getString(R.string.agreement_terms)) }
                                            pop()
                                            append(getString(R.string.agreement_and))
                                            pushStringAnnotation("legal", LEGAL_DOCUMENT_URL)
                                            withStyle(SpanStyle(color = accent)) { append(getString(R.string.agreement_privacy)) }
                                            pop()
                                        }
                                        ClickableText(agreement, modifier = Modifier.weight(1f), style = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface)) { offset ->
                                            agreement.getStringAnnotations("legal", offset, offset).firstOrNull()?.let { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item))) }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                GlassButton(if (model.loading) getString(R.string.account_wait) else getString(R.string.account_send_code), { model.sendCode(register) }, !model.loading && model.email.isNotBlank() && (!register || model.account.isNotBlank() && model.password.length >= MIN_PASSWORD_LENGTH && model.agreed))
                            }
                            model.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
                        }
                    }
                }
            }
        }
    }
}
