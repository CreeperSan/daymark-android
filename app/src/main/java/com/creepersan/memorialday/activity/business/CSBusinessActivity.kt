package com.creepersan.memorialday.activity.business

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.module.network.host.daymark.Anniversary
import com.creepersan.memorialday.module.network.host.daymark.AnniversaryCreateRequest
import com.creepersan.memorialday.module.network.host.daymark.AnniversaryEditRequest
import com.creepersan.memorialday.module.network.host.daymark.DaymarkRepositoryFactory
import com.creepersan.memorialday.module.network.host.daymark.DaymarkResult
import com.creepersan.memorialday.module.route.navigator.CSBusinessDestination
import com.creepersan.memorialday.module.route.navigator.CSBusinessPageEnter
import com.creepersan.memorialday.module.route.navigator.CSNavigator
import com.creepersan.memorialday.module.route.navigator.CSPageOutcome
import com.creepersan.memorialday.ui.theme.MemorialDayTheme
import com.creepersan.memorialday.widget.composable.CSButton
import com.creepersan.memorialday.widget.composable.CSButtonLoading
import com.creepersan.memorialday.widget.composable.CSButtonSecondary
import com.creepersan.memorialday.widget.composable.CSButtonText
import com.creepersan.memorialday.widget.composable.CSCard
import com.creepersan.memorialday.widget.composable.CSEmptyState
import com.creepersan.memorialday.widget.composable.CSInput
import com.creepersan.memorialday.widget.composable.CSLoading
import com.creepersan.memorialday.widget.composable.CSModal
import com.creepersan.memorialday.widget.composable.CSNavigationBar
import com.creepersan.memorialday.widget.composable.CSPillNavigation
import com.creepersan.memorialday.widget.composable.CSPillNavigationItem
import com.creepersan.memorialday.widget.composable.CSRadioButton
import com.creepersan.memorialday.widget.composable.CSSwitch
import com.creepersan.memorialday.widget.composable.CSTextBody
import com.creepersan.memorialday.widget.composable.CSTextBodySmall
import com.creepersan.memorialday.widget.composable.CSTextCardTitle
import com.creepersan.memorialday.widget.composable.CSTextDisplay
import com.creepersan.memorialday.widget.composable.CSTextEmphasis
import com.creepersan.memorialday.widget.composable.CSTextHint
import com.creepersan.memorialday.widget.composable.CSTextSectionTitle
import com.creepersan.memorialday.widget.composable.CSTextTitle
import com.creepersan.memorialday.widget.composable.token.CSTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class CSBusinessActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entry = CSNavigator.businessPageEnter(intent)
        setContent {
            MemorialDayTheme {
                CSBusinessPage(entry = entry, activity = this)
            }
        }
    }
}

@Composable
private fun CSBusinessPage(entry: CSBusinessPageEnter, activity: Activity) {
    when (entry.destination) {
        CSBusinessDestination.Home -> CSHomePage(activity)
        CSBusinessDestination.AnniversaryEditor -> CSAnniversaryEditorPage(activity, entry.anniversaryId)
        CSBusinessDestination.AnniversaryDetail -> CSAnniversaryDetailPage(activity, entry.anniversaryId)
        CSBusinessDestination.Settings -> CSSettingsPage(activity)
        CSBusinessDestination.CategoryManagement -> CSCategoryManagementPage(activity)
        CSBusinessDestination.ReminderManagement -> CSReminderManagementPage(activity)
        CSBusinessDestination.Timeline -> CSTimelinePage(activity, entry.anniversaryId)
        CSBusinessDestination.TimelineEventEditor -> CSTimelineEventEditorPage(
            activity,
            entry.anniversaryId,
            entry.timelineEventId
        )
        CSBusinessDestination.MarkEditor -> CSMarkEditorPage(activity, entry.anniversaryId)
        CSBusinessDestination.PermissionExplanation -> CSPermissionExplanationPage(activity)
        CSBusinessDestination.VersionSwitch -> CSVersionSwitchPage(activity)
        CSBusinessDestination.CategoryEditor -> CSCategoryEditorPage(activity, entry.categoryId)
    }
}

@Composable
private fun CSBusinessScaffold(
    title: String,
    activity: Activity,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    action: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(CSTheme.colors.warmGradientStart.copy(alpha = 0.20f), CSTheme.colors.background)
                )
            )
    ) {
        CSNavigationBar(
            background = com.creepersan.memorialday.widget.composable.token.CSNavigationBackground.Transparent,
            leftContent = {
                if (showBack) {
                    CSButtonText(text = "返回", small = true, onClick = { CSNavigator.close(activity) })
                }
            },
            centerContent = { CSTextCardTitle(title) },
            rightContent = { action?.invoke() }
        )
        content(PaddingValues(horizontal = 20.dp, vertical = 12.dp))
    }
}

@Composable
private fun CSHomePage(activity: Activity) {
    val context = LocalContext.current
    val repository = remember { DaymarkRepositoryFactory.create(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var anniversaries by remember { mutableStateOf<List<Anniversary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load(force: Boolean = false) {
        scope.launch {
            loading = true
            error = null
            if (force) {
                when (val refreshed = repository.refreshAnniversaries(force = true)) {
                    is DaymarkResult.Failure -> error = refreshed.error.message
                    is DaymarkResult.Success -> Unit
                }
            }
            when (val result = repository.listAnniversaries()) {
                is DaymarkResult.Success -> anniversaries = result.data
                is DaymarkResult.Failure -> error = result.error.message
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }

    CSBusinessScaffold(
        title = "Daymark",
        activity = activity,
        showBack = false,
        action = {
            CSButtonText("设置", small = true, onClick = { CSNavigator.toSettings(activity).subscribe() })
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading && anniversaries.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { CSLoading() }
                anniversaries.isEmpty() -> CSEmptyState(
                    title = "还没有纪念日",
                    description = error ?: "记录一个重要的日子，让时间替你珍藏。",
                    modifier = Modifier.align(Alignment.Center),
                    action = { CSButton("新建纪念日", onClick = { CSNavigator.toAnniversaryEditor(activity).subscribe() }) }
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        val closest = anniversaries.minByOrNull { it.nextTimestamp }
                        CSHeroCard(closest)
                        Spacer(Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CSTextSectionTitle("即将到来")
                            Spacer(Modifier.weight(1f))
                            CSButtonText("刷新", small = true, onClick = { load(force = true) })
                        }
                    }
                    items(anniversaries, key = { it.id }) { anniversary ->
                        CSCard(onClick = { CSNavigator.toAnniversaryDetail(activity, anniversary.id).subscribe() }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    CSTextCardTitle(anniversary.name)
                                    CSTextBodySmall(
                                        "${anniversary.year}.${anniversary.month.toString().padStart(2, '0')}."
                                            + anniversary.day.toString().padStart(2, '0')
                                    )
                                }
                                CSTextEmphasis("${daysUntil(anniversary)} 天")
                            }
                        }
                    }
                    error?.let { message ->
                        item { CSTextHint(message, color = CSTheme.colors.danger) }
                    }
                }
            }
            CSPillNavigation(
                items = listOf(
                    CSPillNavigationItem("home", "首页") { CSTextHint("⌂") },
                    CSPillNavigationItem("timeline", "时间线") { CSTextHint("◷") },
                    CSPillNavigationItem("reminder", "提醒") { CSTextHint("!") },
                    CSPillNavigationItem("settings", "我的") { CSTextHint("○") }
                ),
                selectedIndex = 0,
                onItemSelected = { index ->
                    when (index) {
                        1 -> anniversaries.firstOrNull()?.let { CSNavigator.toTimeline(activity, it.id).subscribe() }
                        2 -> CSNavigator.toReminderManagement(activity).subscribe()
                        3 -> CSNavigator.toSettings(activity).subscribe()
                    }
                },
                centerAction = { CSTextBody("+", color = Color.White) },
                onCenterAction = { CSNavigator.toAnniversaryEditor(activity).subscribe() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp)
            )
        }
    }
}

@Composable
private fun CSHeroCard(anniversary: Anniversary?) {
    CSCard(elevation = 8.dp) {
        CSTextHint("下一次重要时刻")
        Spacer(Modifier.height(8.dp))
        CSTextTitle(anniversary?.name ?: "值得纪念的日子")
        Spacer(Modifier.height(16.dp))
        CSTextDisplay("${anniversary?.let(::daysUntil) ?: 0}")
        CSTextBodySmall("天后到来", color = CSTheme.colors.textSecondary)
    }
}

@Composable
private fun CSAnniversaryEditorPage(activity: Activity, anniversaryId: Long?) {
    val context = LocalContext.current
    val repository = remember { DaymarkRepositoryFactory.create(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var year by remember { mutableStateOf(LocalDate.now().year.toString()) }
    var month by remember { mutableStateOf(LocalDate.now().monthValue.toString()) }
    var day by remember { mutableStateOf(LocalDate.now().dayOfMonth.toString()) }
    var calendarType by remember { mutableStateOf("solar") }
    var loopType by remember { mutableStateOf("yearly") }
    var reminderEnabled by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(anniversaryId != null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(anniversaryId) {
        anniversaryId ?: return@LaunchedEffect
        when (val result = repository.getAnniversary(anniversaryId)) {
            is DaymarkResult.Success -> {
                val item = result.data
                name = item.name
                description = item.description
                year = item.year.toString()
                month = item.month.toString()
                day = item.day.toString()
                calendarType = item.calendarType
                loopType = item.loopType
            }
            is DaymarkResult.Failure -> error = result.error.message
        }
        loading = false
    }

    fun save() {
        val parsedYear = year.toIntOrNull()
        val parsedMonth = month.toIntOrNull()
        val parsedDay = day.toIntOrNull()
        if (parsedYear == null || parsedMonth == null || parsedDay == null) {
            error = "请输入正确的日期"
            return
        }
        scope.launch {
            saving = true
            error = null
            val result = if (anniversaryId == null) {
                repository.createAnniversary(
                    AnniversaryCreateRequest(
                        name = name.trim(),
                        description = description.trim(),
                        calendarType = calendarType,
                        year = parsedYear,
                        month = parsedMonth,
                        day = parsedDay,
                        loopType = loopType
                    )
                )
            } else {
                repository.editAnniversary(
                    AnniversaryEditRequest(
                        id = anniversaryId,
                        name = name.trim(),
                        description = description.trim(),
                        calendarType = calendarType,
                        year = parsedYear,
                        month = parsedMonth,
                        day = parsedDay,
                        loopType = loopType
                    )
                )
            }
            when (result) {
                is DaymarkResult.Success -> CSNavigator.close(
                    activity,
                    CSNavigator.businessPageResult(CSPageOutcome.Completed, anniversaryId = result.data.id)
                )
                is DaymarkResult.Failure -> {
                    error = result.error.message
                    saving = false
                }
            }
        }
    }

    CSBusinessScaffold(
        title = if (anniversaryId == null) "新建纪念日" else "编辑纪念日",
        activity = activity,
        action = {
            if (saving) CSButtonLoading("保存中", small = true) else CSButtonText("保存", small = true, onClick = ::save)
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CSLoading() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CSInput(name, { name = it; error = null }, label = "名称", placeholder = "例如：恋爱纪念日", isError = error != null)
                CSInput(description, { description = it }, label = "备注", placeholder = "写下这一天的意义")
                CSTextSectionTitle("日期")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CSInput(year, { year = it }, modifier = Modifier.weight(1f), label = "年")
                    CSInput(month, { month = it }, modifier = Modifier.weight(1f), label = "月")
                    CSInput(day, { day = it }, modifier = Modifier.weight(1f), label = "日")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CSRadioButton(calendarType == "solar", { calendarType = "solar" }, label = "公历")
                    CSRadioButton(calendarType == "lunar", { calendarType = "lunar" }, label = "农历")
                }
                CSTextSectionTitle("重复")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf("none" to "不重复", "yearly" to "每年", "monthly" to "每月").forEach { (value, label) ->
                        CSRadioButton(loopType == value, { loopType = value }, label = label)
                    }
                }
                CSCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            CSTextCardTitle("提醒")
                            CSTextBodySmall("提醒同步将在服务端接口就绪后启用")
                        }
                        CSSwitch(reminderEnabled, { reminderEnabled = it }, contentDescription = "纪念日提醒")
                    }
                }
                error?.let { CSTextHint(it, color = CSTheme.colors.danger) }
                if (!saving) CSButton(if (anniversaryId == null) "创建纪念日" else "保存修改", fullWidth = true, onClick = ::save)
            }
        }
    }
}

@Composable
private fun CSAnniversaryDetailPage(activity: Activity, anniversaryId: Long?) {
    val context = LocalContext.current
    val repository = remember { DaymarkRepositoryFactory.create(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var item by remember { mutableStateOf<Anniversary?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    LaunchedEffect(anniversaryId) {
        if (anniversaryId == null) {
            error = "纪念日不存在"
        } else {
            when (val result = repository.getAnniversary(anniversaryId)) {
                is DaymarkResult.Success -> item = result.data
                is DaymarkResult.Failure -> error = result.error.message
            }
        }
    }
    CSBusinessScaffold(
        title = "纪念日详情",
        activity = activity,
        action = { CSButtonText("编辑", small = true, onClick = { anniversaryId?.let { CSNavigator.toAnniversaryEditor(activity, it).subscribe() } }) }
    ) { padding ->
        val anniversary = item
        when {
            anniversary == null && error == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CSLoading() }
            anniversary == null -> CSEmptyState("无法加载纪念日", error, modifier = Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CSHeroCard(anniversary)
                CSCard {
                    CSTextSectionTitle("关于这一天")
                    CSTextBody(if (anniversary.description.isBlank()) "还没有留下备注。" else anniversary.description)
                    Spacer(Modifier.height(8.dp))
                    CSTextHint("${anniversary.year}.${anniversary.month}.${anniversary.day} · ${anniversary.loopType}")
                }
                CSCard(onClick = { CSNavigator.toTimeline(activity, anniversary.id).subscribe() }) {
                    CSTextCardTitle("时间线")
                    CSTextBodySmall("记录属于这一天的文字、照片和心情")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CSButtonSecondary("分享", modifier = Modifier.weight(1f), onClick = {
                        activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${anniversary.name} · 还有 ${daysUntil(anniversary)} 天")
                        }, "分享纪念日"))
                    })
                    CSButtonText("删除", modifier = Modifier.weight(1f), onClick = { showDelete = true })
                }
            }
        }
    }
    CSModal(
        visible = showDelete,
        onDismissRequest = { if (!deleting) showDelete = false },
        title = "删除纪念日？",
        description = "删除后无法恢复。",
        confirmButton = {
            if (deleting) CSButtonLoading("删除中", fullWidth = true) else CSButton("确认删除", fullWidth = true, onClick = {
                val id = item?.id ?: return@CSButton
                deleting = true
                scope.launch {
                    when (val result = repository.deleteAnniversary(id)) {
                        is DaymarkResult.Success -> CSNavigator.close(
                            activity,
                            CSNavigator.businessPageResult(CSPageOutcome.Completed, anniversaryId = id)
                        )
                        is DaymarkResult.Failure -> {
                            error = result.error.message
                            deleting = false
                            showDelete = false
                        }
                    }
                }
            })
        },
        dismissButton = { CSButtonText("取消", fullWidth = true, enabled = !deleting, onClick = { showDelete = false }) }
    )
}

@Composable
private fun CSSettingsPage(activity: Activity) {
    CSBusinessScaffold(title = "设置", activity = activity) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CSSettingsRow("分类管理", "整理纪念日分类") { CSNavigator.toCategoryManagement(activity).subscribe() }
            CSSettingsRow("提醒", "管理提醒方式和时间") { CSNavigator.toReminderManagement(activity).subscribe() }
            CSSettingsRow("权限说明", "了解通知与媒体权限") { CSNavigator.toPermissionExplanation(activity).subscribe() }
            CSSettingsRow("新旧版本切换", "选择你熟悉的使用方式") { CSNavigator.toVersionSwitch(activity).subscribe() }
        }
    }
}

@Composable
private fun CSSettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    CSCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                CSTextCardTitle(title)
                CSTextBodySmall(subtitle)
            }
            CSTextHint("›")
        }
    }
}

@Composable
private fun CSCategoryManagementPage(activity: Activity) {
    val categories = remember { mutableStateListOf("纪念日", "生日", "旅行") }
    CSBusinessScaffold(
        title = "纪念日分类",
        activity = activity,
        action = { CSButtonText("新建", small = true, onClick = { CSNavigator.toCategoryEditor(activity).subscribe() }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CSTextHint("分类排序仅保存在当前设备，等待分类接口开放后同步。")
            categories.forEachIndexed { index, category ->
                CSCard(onClick = { CSNavigator.toCategoryEditor(activity, category).subscribe() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CSTextCardTitle(category, Modifier.weight(1f))
                        CSButtonText("上移", small = true, enabled = index > 0, onClick = {
                            categories[index] = categories[index - 1].also { categories[index - 1] = categories[index] }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun CSCategoryEditorPage(activity: Activity, categoryId: String?) {
    var name by remember { mutableStateOf(categoryId.orEmpty()) }
    var color by remember { mutableStateOf("暖橙") }
    var icon by remember { mutableStateOf("●") }
    var reminder by remember { mutableStateOf(true) }
    CSBusinessScaffold(title = if (categoryId == null) "新建分类" else "编辑分类", activity = activity) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CSInput(name, { name = it }, label = "分类名称", placeholder = "例如：生日")
            CSInput(color, { color = it }, label = "颜色", placeholder = "选择分类颜色")
            CSInput(icon, { icon = it }, label = "图标", placeholder = "选择图标")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    CSTextCardTitle("默认提醒")
                    CSTextBodySmall("分类接口尚未开放，同步功能暂不可用。")
                }
                CSSwitch(reminder, { reminder = it })
            }
            CSButton("保存本地草稿", fullWidth = true, enabled = name.isNotBlank(), onClick = {
                CSNavigator.close(activity, CSNavigator.businessPageResult(CSPageOutcome.Unavailable, categoryId = name))
            })
        }
    }
}

@Composable
private fun CSReminderManagementPage(activity: Activity) {
    var notification by remember { mutableStateOf(true) }
    var morning by remember { mutableStateOf(false) }
    CSBusinessScaffold(title = "提醒", activity = activity) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CSTextHint("提醒服务接口尚未开放，以下配置不会同步到服务器。")
            CSCard {
                CSSettingSwitch("开启通知", "在重要日子到来前提醒你", notification) { notification = it }
                CSSettingSwitch("当天早晨提醒", "在当天上午发送通知", morning) { morning = it }
            }
        }
    }
}

@Composable
private fun CSSettingSwitch(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            CSTextCardTitle(title)
            CSTextBodySmall(description)
        }
        CSSwitch(checked, onCheckedChange)
    }
}

@Composable
private fun CSTimelinePage(activity: Activity, anniversaryId: Long?) {
    var selected by remember { mutableStateOf("全部") }
    CSBusinessScaffold(
        title = "时间线",
        activity = activity,
        action = {
            CSButtonText("新建", small = true, onClick = {
                anniversaryId?.let { CSNavigator.toTimelineEventEditor(activity, it).subscribe() }
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("全部", "文字", "图片", "心情").forEach { type ->
                    if (selected == type) CSButton(type, small = true, onClick = { selected = type })
                    else CSButtonSecondary(type, small = true, onClick = { selected = type })
                }
            }
            CSEmptyState(
                title = "还没有时间线",
                description = "时间线服务接口尚未开放，创建入口已准备就绪。",
                action = {
                    CSButton("记录第一条", onClick = {
                        anniversaryId?.let { CSNavigator.toTimelineEventEditor(activity, it).subscribe() }
                    })
                }
            )
        }
    }
}

@Composable
private fun CSTimelineEventEditorPage(activity: Activity, anniversaryId: Long?, eventId: Long?) {
    var content by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("开心") }
    val mediaDrafts = remember { mutableStateListOf<String>() }
    CSBusinessScaffold(title = if (eventId == null) "新建时间线事件" else "编辑时间线事件", activity = activity) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CSInput(content, { content = it }, label = "记录内容", placeholder = "写下这一刻")
            CSInput(mood, { mood = it }, label = "心情", placeholder = "选择当前心情")
            CSCard {
                CSTextCardTitle("图片、视频和语音")
                CSTextBodySmall("媒体仅作为本地草稿，等待时间线接口开放后上传。")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("图片", "视频", "语音").forEach { mediaType ->
                        CSButtonSecondary(mediaType, small = true, onClick = {
                            if (mediaType !in mediaDrafts) {
                                mediaDrafts += mediaType
                            }
                        })
                    }
                }
                if (mediaDrafts.isNotEmpty()) {
                    CSTextHint("已添加本地草稿：${mediaDrafts.joinToString("、")}")
                }
            }
            CSButton("保存本地草稿", fullWidth = true, enabled = content.isNotBlank(), onClick = {
                CSNavigator.close(
                    activity,
                    CSNavigator.businessPageResult(
                        CSPageOutcome.Unavailable,
                        anniversaryId = anniversaryId,
                        timelineEventId = eventId
                    )
                )
            })
        }
    }
}

@Composable
private fun CSMarkEditorPage(activity: Activity, anniversaryId: Long?) {
    var text by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("平静") }
    val mediaDrafts = remember { mutableStateListOf<String>() }
    CSBusinessScaffold(title = "记录 Mark", activity = activity) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CSInput(text, { text = it }, label = "这一刻", placeholder = "写下你想留下的内容")
            CSInput(mood, { mood = it }, label = "心情标签", placeholder = "例如：开心")
            CSCard {
                CSTextCardTitle("图片 · 视频 · 语音")
                CSTextBodySmall("Mark 服务接口尚未开放，媒体会作为本地草稿保留在当前页面。")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("图片", "视频", "语音").forEach { mediaType ->
                        CSButtonSecondary(mediaType, small = true, onClick = {
                            if (mediaType !in mediaDrafts) {
                                mediaDrafts += mediaType
                            }
                        })
                    }
                }
                if (mediaDrafts.isNotEmpty()) {
                    CSTextHint("已添加本地草稿：${mediaDrafts.joinToString("、")}")
                }
            }
            CSButton("保存本地草稿", fullWidth = true, enabled = text.isNotBlank(), onClick = {
                CSNavigator.close(activity, CSNavigator.businessPageResult(CSPageOutcome.Unavailable, anniversaryId))
            })
        }
    }
}

@Composable
private fun CSPermissionExplanationPage(activity: Activity) {
    val permissions = listOf(
        "通知权限" to "用于在重要日子到来前提醒你",
        "日历权限" to "用于同步到系统日历",
        "相机权限" to "用于为 Mark 添加照片",
        "媒体权限" to "用于选择照片、视频和语音",
        "存储权限" to "用于导出你的纪念日数据"
    )
    CSBusinessScaffold(title = "权限说明", activity = activity) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            permissions.forEach { (title, description) ->
                CSCard {
                    CSTextCardTitle(title)
                    CSTextBodySmall(description)
                }
            }
            CSButtonSecondary("前往系统设置", fullWidth = true, onClick = {
                activity.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null))
                )
            })
        }
    }
}

@Composable
private fun CSVersionSwitchPage(activity: Activity) {
    var useNewVersion by remember { mutableStateOf(true) }
    CSBusinessScaffold(title = "新旧版本切换", activity = activity) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CSCard(onClick = { useNewVersion = true }) {
                CSTextCardTitle("新版 Daymark")
                CSTextBodySmall("使用新的时间线、浮岛导航和沉浸式纪念日卡片。")
                if (useNewVersion) CSTextHint("当前选择", color = CSTheme.colors.primary)
            }
            CSCard(onClick = { useNewVersion = false }) {
                CSTextCardTitle("经典版")
                CSTextBodySmall("保持熟悉的纪念日列表体验。")
                if (!useNewVersion) CSTextHint("当前选择", color = CSTheme.colors.primary)
            }
            CSTextHint("版本偏好目前只保存在本地，服务端迁移尚未开放。")
        }
    }
}

private fun daysUntil(anniversary: Anniversary): Long {
    if (anniversary.nextTimestamp <= 0L) {
        return 0L
    }
    val next = java.time.Instant.ofEpochMilli(anniversary.nextTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return ChronoUnit.DAYS.between(LocalDate.now(), next).coerceAtLeast(0L)
}
