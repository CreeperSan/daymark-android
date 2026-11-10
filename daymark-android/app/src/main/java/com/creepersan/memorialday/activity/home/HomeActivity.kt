package com.creepersan.memorialday.activity.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import com.creepersan.memorialday.module.anniversary.AnniversaryCalendar
import com.creepersan.memorialday.module.anniversary.LunarDateLabels
import com.creepersan.memorialday.module.anniversary.AnniversaryCountdown
import com.creepersan.memorialday.module.anniversary.AnniversaryUpdates
import com.creepersan.memorialday.module.user.UserManager
import com.creepersan.memorialday.ui.glass.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.io.IOException
import javax.net.ssl.SSLException

private val HomeCardRadius = 20.dp
private val HomeFloatingButtonRadius = 30.dp
private val HomeSettingsIconStroke = 1.6.dp

/** 首页列表状态；刷新失败时保留已显示的数据。 */
class HomeViewModel : ViewModel() {
    val user get() = UserManager.session()
    var items by mutableStateOf<List<Anniversary>>(emptyList()); private set
    var today by mutableStateOf(LocalDate.now()); private set
    var loading by mutableStateOf(false); private set
    var loaded by mutableStateOf(false); private set
    var error by mutableStateOf<Throwable?>(null); private set
    private val repository = AnniversaryRepository()
    private var request: Disposable? = null
    private var dayRefreshPending = false
    private val changesDuringLoad = mutableMapOf<Long, Anniversary>()
    private val deletedDuringLoad = mutableSetOf<Long>()
    private val deletions = AnniversaryUpdates.observeDeletions().observeOn(AndroidSchedulers.mainThread()).subscribe { id ->
        items = items.filterNot { it.id == id }
        if (loading) { changesDuringLoad.remove(id); deletedDuringLoad.add(id) }
    }
    private val updates = AnniversaryUpdates.observe().observeOn(AndroidSchedulers.mainThread()).subscribe { item ->
        // 保存或导入成功后直接合并服务端结果，返回首页无需重新请求。
        items = items.filterNot { it.id == item.id } + item
        if (loading) changesDuringLoad[item.id] = item
    }

    /** 刷新服务端循环日期并读取全部列表。 */
    fun load() {
        if (loading) return
        loading = true; error = null
        changesDuringLoad.clear(); deletedDuringLoad.clear()
        request = repository.list().observeOn(AndroidSchedulers.mainThread()).subscribe({
            // 请求期间保存的数据优先于请求开始时的列表，避免旧响应覆盖新编辑。
            items = it.filterNot { item -> item.id in changesDuringLoad || item.id in deletedDuringLoad } + changesDuringLoad.values
            changesDuringLoad.clear(); loaded = true; loading = false
            if (dayRefreshPending) { dayRefreshPending = false; load() }
        }, {
            error = it; loading = false
            changesDuringLoad.clear()
            if (dayRefreshPending) { dayRefreshPending = false; load() }
        })
    }
    /** 下拉时刷新日期和云端列表；已有请求完成后再执行一次，避免丢失刷新操作。 */
    fun refresh() {
        if (loading) return
        com.creepersan.memorialday.module.log.Log.info("下拉刷新纪念日列表", "Home")
        updateToday()
        load()
    }
    /** 只有本地日期变化时才自动刷新，普通页面进出不请求列表。 */
    fun refreshIfDayChanged() {
        val current = LocalDate.now()
        if (current != today) {
            today = current
            if (loading) dayRefreshPending = true else load()
        }
    }
    /** 手动刷新前更新日期，保证倒计时使用当前日历日。 */
    fun updateToday() { today = LocalDate.now() }
    fun logout() { request?.dispose(); UserManager.clear() }
    override fun onCleared() { request?.dispose(); updates.dispose(); deletions.dispose() }
}

/** 新版首页：展示纪念日并进入创建、编辑和设置。 */
@OptIn(ExperimentalMaterialApi::class)
class HomeActivity : BaseComposeActivity() {
    private lateinit var model: HomeViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[HomeViewModel::class.java]
        // 仅首次创建时加载；从其他页面返回不重新请求。
        if (!model.loaded && !model.loading && model.error == null) model.load()
        setContent {
            // 仅在页面可见时等待下一次本地零点，跨天后刷新循环纪念日。
            LaunchedEffect(Unit) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (true) {
                        model.refreshIfDayChanged()
                        val now = ZonedDateTime.now()
                        val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
                        delay(Duration.between(now, nextDay).toMillis().coerceAtLeast(1L))
                        model.refreshIfDayChanged()
                    }
                }
            }
            val scrollState = rememberLazyListState()
            val pullRefreshState = rememberPullRefreshState(refreshing = model.loading, onRefresh = { model.refresh() })
            GlassScrollingPage(scrollState, header = {
                GlassNavigationBar(getString(R.string.home_title), action = {
                    val iconColor = MaterialTheme.colorScheme.onBackground
                    GlassNavigationIconButton(getString(R.string.settings), onClick = { startActivity(Intent(this@HomeActivity, SettingActivity::class.java)) }) { iconModifier ->
                        // 用线条绘制设置图标，避免不同字体的字符外观不一致。
                        Canvas(iconModifier) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val stroke = HomeSettingsIconStroke.toPx()
                            drawCircle(iconColor, radius = size.minDimension * 0.26f, center = center, style = Stroke(stroke))
                            drawCircle(iconColor, radius = size.minDimension * 0.07f, center = center)
                            for (i in 0 until 8) {
                                val angle = i * Math.PI / 4
                                val dx = kotlin.math.cos(angle).toFloat(); val dy = kotlin.math.sin(angle).toFloat()
                                drawLine(iconColor, center + Offset(dx * size.minDimension * 0.28f, dy * size.minDimension * 0.28f), center + Offset(dx * size.minDimension * 0.4f, dy * size.minDimension * 0.4f), strokeWidth = stroke)
                            }
                        }
                    }
                })
            }) { insets ->
                val dark = isGlassDark()
                val sortedItems = remember(model.items, model.today) { AnniversaryCountdown.sort(model.items, model.today, ZoneId.systemDefault()) }
                val groups = listOf(
                    R.string.home_today to sortedItems.filter { it.second == 0L },
                    R.string.home_upcoming to sortedItems.filter { it.second != null && it.second!! > 0 },
                    R.string.home_past to sortedItems.filter { it.second != null && it.second!! < 0 },
                    R.string.home_other to sortedItems.filter { it.second == null }
                )
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.fillMaxSize().glassBounce().pullRefresh(pullRefreshState), state = scrollState, contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = insets.calculateTopPadding(), bottom = insets.calculateBottomPadding() + 80.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (model.error == null) groups.forEach { (title, entries) ->
                            if (entries.isNotEmpty()) {
                                item { Text(getString(title), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = glassSecondaryColor()) }
                                items(entries, key = { it.first.id }) { (item, days) ->
                                    Row(Modifier.fillMaxWidth().glassClickable { startActivity(Intent(this@HomeActivity, AnniversaryEditActivity::class.java).putExtra(AnniversaryEditActivity.EXTRA_ID, item.id)) }.glassSurface(HomeCardRadius, dark, highlighted = days == 0L).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(item.name, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = if (days != null && days < 0) FontWeight.Medium else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                                            Text(formatDate(item), fontSize = 12.sp, lineHeight = 18.sp, color = glassSecondaryColor(), modifier = Modifier.padding(top = 3.dp))
                                        }
                                        val countdown = when {
                                            days == null -> getString(R.string.anniversary_countdown_unavailable)
                                            days == 0L -> getString(R.string.anniversary_countdown_today)
                                            days > 0 -> resources.getQuantityString(R.plurals.anniversary_days_left, days.toInt(), days)
                                            else -> resources.getQuantityString(R.plurals.anniversary_days_ago, (-days).toInt(), -days)
                                        }
                                        val pillColor = if (days == 0L) Color(0xFFFF6A45) else if (days != null && days < 0) Color(0xFFD7AF96).copy(alpha = 0.3f) else Color(0xFFFF6A45).copy(alpha = 0.12f)
                                        val textColor = if (days == 0L) Color.White else if (days != null && days < 0) glassSecondaryColor() else MaterialTheme.colorScheme.primary
                                        Text(countdown, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp).background(pillColor, CircleShape).padding(horizontal = 10.dp, vertical = 6.dp))
                                    }
                                }
                            }
                        }
                    }
                    if (!model.loading) {
                        val error = model.error
                        val kind = if (error != null) {
                            val causes = generateSequence(error) { it.cause }.toList()
                            val offline = causes.none { it is SSLException } && causes.any { it is IOException }
                            if (offline) GlassPageStateKind.Offline else GlassPageStateKind.Failed
                        } else if (model.loaded && model.items.isEmpty()) GlassPageStateKind.Empty else null
                        if (kind != null) Box(Modifier.fillMaxSize().padding(top = insets.calculateTopPadding(), bottom = insets.calculateBottomPadding()), contentAlignment = Alignment.Center) {
                            GlassPageState(kind, onRetry = if (error != null) ({ model.load() }) else null)
                        }
                    }
                    PullRefreshIndicator(model.loading, pullRefreshState, Modifier.align(Alignment.TopCenter).padding(top = insets.calculateTopPadding()), backgroundColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary)
                    Box(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = insets.calculateBottomPadding()).size(HomeFloatingButtonRadius * 2).glassClickable(immediatePress = true) { startActivity(Intent(this@HomeActivity, AnniversaryEditActivity::class.java)) }.glassSurface(HomeFloatingButtonRadius, dark, highlighted = true, accented = true), contentAlignment = Alignment.Center) {
                        Text("+", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        }
    }
    /** 按当前语言显示公历日期；农历条目同时保留其原始农历日期。 */
    private fun formatDate(item: Anniversary): String {
        return try {
            val lunar = item.calendarType == Anniversary.CALENDAR_LUNAR
            val converted = if (lunar) AnniversaryCalendar.convert(item.year, item.month, item.day, true, item.isLeapMonth == 1) else null
            val solar = if (converted != null) LocalDate.of(converted.year, converted.month, converted.day) else LocalDate.of(item.year, item.month, item.day)
            val locale = resources.configuration.locales[0]
            val date = solar.format(DateTimeFormatter.ofPattern("yyyy.MM.dd", locale))
            if (lunar) {
                val leap = if (item.isLeapMonth == 1) getString(R.string.anniversary_leap_prefix) else ""
                val lunarDate = if (locale.language == "zh") "$leap${LunarDateLabels.month(item.month)}${LunarDateLabels.day(item.day)}" else "$leap${item.month}/${item.day}"
                "$date · ${getString(R.string.anniversary_lunar)}$lunarDate"
            } else "$date · ${solar.format(DateTimeFormatter.ofPattern("EEE", locale))}"
        } catch (_: Exception) {
            getString(R.string.anniversary_countdown_unavailable)
        }
    }
}
