package com.creepersan.memorialday.activity.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.creepersan.memorialday.R
import com.creepersan.memorialday.activity.anniversary.AnniversaryEditActivity
import com.creepersan.memorialday.activity.login.LoginActivity
import com.creepersan.memorialday.activity.setting.SettingActivity
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.module.anniversary.Anniversary
import com.creepersan.memorialday.module.anniversary.AnniversaryRepository
import com.creepersan.memorialday.module.anniversary.AnniversaryCountdown
import com.creepersan.memorialday.module.user.UserManager
import com.creepersan.memorialday.ui.glass.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneId

/** 首页列表状态；刷新失败时保留已显示的数据。 */
class HomeViewModel : ViewModel() {
    val user get() = UserManager.session()
    var items by mutableStateOf<List<Anniversary>>(emptyList()); private set
    var today by mutableStateOf(LocalDate.now()); private set
    var loading by mutableStateOf(false); private set
    var loaded by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    private val repository = AnniversaryRepository()
    private var request: Disposable? = null
    private var reloadPending = false

    /** 刷新服务端循环日期并读取全部列表。 */
    fun load() {
        // 编辑返回时若前一次加载尚未结束，完成后再刷新一次，避免显示旧数据。
        if (loading) { reloadPending = true; return }
        loading = true; error = null
        request = repository.list().observeOn(AndroidSchedulers.mainThread()).subscribe({
            items = it; loaded = true; loading = false
            if (reloadPending) { reloadPending = false; load() }
        }, {
            error = it.message ?: "Request failed"; loading = false
            if (reloadPending) { reloadPending = false; load() }
        })
    }
    /** 页面恢复或跨天时更新今天；下次加载会更新服务端循环日期。 */
    fun updateToday() { today = LocalDate.now() }
    fun logout() { request?.dispose(); UserManager.clear() }
    override fun onCleared() { request?.dispose() }
}

/** 新版首页：展示纪念日并进入创建、编辑和设置。 */
class HomeActivity : BaseComposeActivity() {
    private lateinit var model: HomeViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[HomeViewModel::class.java]
        setContent {
            // 仅在页面可见时等待下一次本地零点，跨天后刷新循环纪念日。
            LaunchedEffect(Unit) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (true) {
                        model.updateToday()
                        val now = ZonedDateTime.now()
                        val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
                        delay(Duration.between(now, nextDay).toMillis().coerceAtLeast(1L))
                        model.updateToday()
                        model.load()
                    }
                }
            }
            GlassPage {
                Text(getString(R.string.home), fontSize = 32.sp)
                Text(getString(R.string.welcome, model.user?.name.orEmpty()))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { startActivity(Intent(this@HomeActivity, AnniversaryEditActivity::class.java)) }) { Text(getString(R.string.anniversary_add)) }
                    TextButton(onClick = { model.load() }, enabled = !model.loading) { Text(getString(R.string.anniversary_refresh)) }
                    TextButton(onClick = { startActivity(Intent(this@HomeActivity, SettingActivity::class.java)) }) { Text(getString(R.string.settings)) }
                }
                if (model.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                model.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { model.load() }) { Text(getString(R.string.retry)) } }
                if (model.loaded && model.items.isEmpty()) Text(getString(R.string.anniversary_empty))
                val sortedItems = remember(model.items, model.today) { AnniversaryCountdown.sort(model.items, model.today, ZoneId.systemDefault()) }
                LazyColumn(Modifier.weight(1f)) {
                    items(sortedItems, key = { it.first.id }) { (item, days) ->
                        GlassCard {
                            Column(Modifier.fillMaxWidth().clickable { startActivity(Intent(this@HomeActivity, AnniversaryEditActivity::class.java).putExtra(AnniversaryEditActivity.EXTRA_ID, item.id)) }) {
                                Text(item.name, style = MaterialTheme.typography.titleLarge)
                                val calendar = getString(if (item.calendarType == Anniversary.CALENDAR_LUNAR) R.string.anniversary_lunar else R.string.anniversary_solar)
                                val leap = if (item.isLeapMonth == 1) getString(R.string.anniversary_leap_prefix) else ""
                                Text(getString(R.string.anniversary_date_format, calendar, item.year, leap, item.month, item.day))
                                if (item.description.isNotBlank()) Text(item.description)
                                // 使用复数资源，兼容不同语言的单复数规则。
                                val countdown = when {
                                    days == null -> getString(R.string.anniversary_countdown_unavailable)
                                    days == 0L -> getString(R.string.anniversary_countdown_today)
                                    days > 0 -> resources.getQuantityString(R.plurals.anniversary_days_left, days.toInt(), days)
                                    else -> resources.getQuantityString(R.plurals.anniversary_days_ago, (-days).toInt(), -days)
                                }
                                Text(countdown, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                TextButton(onClick = { model.logout(); startActivity(Intent(this@HomeActivity, LoginActivity::class.java)); finish() }) { Text(getString(R.string.logout)) }
            }
        }
    }
    override fun onResume() { super.onResume(); model.updateToday(); model.load() }
}
