package com.creepersan.memorialday.activity.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
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
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(getString(R.string.home_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Box(Modifier.size(38.dp).background(Color.White.copy(alpha = 0.35f), CircleShape).clickable { startActivity(Intent(this@HomeActivity, SettingActivity::class.java)) }, contentAlignment = Alignment.Center) { Text("⚙", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp) }
                }
                if (model.loading) Text(getString(R.string.account_wait), style = MaterialTheme.typography.bodySmall)
                model.error?.let { Text(it, color = MaterialTheme.colorScheme.error); Text(getString(R.string.retry), color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { model.load() }.padding(vertical = 8.dp)) }
                if (model.loaded && model.items.isEmpty()) Text(getString(R.string.anniversary_empty), modifier = Modifier.padding(vertical = 24.dp))
                val sortedItems = remember(model.items, model.today) { AnniversaryCountdown.sort(model.items, model.today, ZoneId.systemDefault()) }
                val groups = listOf(
                    R.string.home_today to sortedItems.filter { it.second == 0L },
                    R.string.home_upcoming to sortedItems.filter { it.second != null && it.second!! > 0 },
                    R.string.home_past to sortedItems.filter { it.second != null && it.second!! < 0 },
                    R.string.home_other to sortedItems.filter { it.second == null }
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                    groups.forEach { (title, entries) ->
                        if (entries.isNotEmpty()) {
                            item { Text(getString(title), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)) }
                            items(entries, key = { it.first.id }) { (item, days) ->
                                GlassCard {
                                    Row(Modifier.fillMaxWidth().clickable { startActivity(Intent(this@HomeActivity, AnniversaryEditActivity::class.java).putExtra(AnniversaryEditActivity.EXTRA_ID, item.id)) }, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            val calendar = getString(if (item.calendarType == Anniversary.CALENDAR_LUNAR) R.string.anniversary_lunar else R.string.anniversary_solar)
                                            val leap = if (item.isLeapMonth == 1) getString(R.string.anniversary_leap_prefix) else ""
                                            Text(getString(R.string.anniversary_date_format, calendar, item.year, leap, item.month, item.day), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                                        }
                                        val countdown = when {
                                            days == null -> getString(R.string.anniversary_countdown_unavailable)
                                            days == 0L -> getString(R.string.anniversary_countdown_today)
                                            days > 0 -> resources.getQuantityString(R.plurals.anniversary_days_left, days.toInt(), days)
                                            else -> resources.getQuantityString(R.plurals.anniversary_days_ago, (-days).toInt(), -days)
                                        }
                                        Text(countdown, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape).padding(horizontal = 10.dp, vertical = 6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Box(Modifier.align(Alignment.BottomEnd).padding(bottom = 12.dp).size(60.dp).background(Brush.linearGradient(listOf(Color(0xFFFF995C), Color(0xFFFF6654))), CircleShape).clickable { startActivity(Intent(this@HomeActivity, AnniversaryEditActivity::class.java)) }, contentAlignment = Alignment.Center) {
                    Text("+", color = Color.White, fontSize = 30.sp)
                }
                }
            }
        }
    }
    override fun onResume() { super.onResume(); model.updateToday(); model.load() }
}
