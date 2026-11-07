package com.creepersan.memorialday.activity.dispatch

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.creepersan.memorialday.R
import com.creepersan.memorialday.activity.home.HomeActivity
import com.creepersan.memorialday.activity.login.LoginActivity
import com.creepersan.memorialday.activity.main.MainActivity
import com.creepersan.memorialday.activity.select_mode.SelectModeActivity
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.constance.*
import com.creepersan.memorialday.module.kv.KV
import com.creepersan.memorialday.module.user.UserManager
import com.creepersan.memorialday.ui.glass.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable

/** 启动时根据已保存模式和会话分流。 */
class DispatchActivity : BaseComposeActivity() {
    private var request: Disposable? = null
    private var error by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GlassPage { if (error) { Text(getString(R.string.connection_error)); GlassButton(getString(R.string.retry), { dispatch() }) } } }
        dispatch()
    }

    private fun dispatch() {
        error = false
        when {
            !KV.has(KV_APPLICATION_MODE) -> open(SelectModeActivity::class.java)
            KV.get(KV_APPLICATION_MODE) == AppMode.Classic -> open(MainActivity::class.java)
            UserManager.session() == null -> open(LoginActivity::class.java)
            else -> {
                request?.dispose()
                request = UserManager.validSession().observeOn(AndroidSchedulers.mainThread()).subscribe({ open(HomeActivity::class.java) }, {
                    if (UserManager.session() == null) open(LoginActivity::class.java) else error = true
                })
            }
        }
    }
    private fun open(target: Class<*>) { startActivity(Intent(this, target)); finish() }
    override fun onDestroy() { request?.dispose(); super.onDestroy() }
}
