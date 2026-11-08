package com.creepersan.memorialday.activity.anniversary

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.creepersan.memorialday.R
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.module.anniversary.*
import com.creepersan.memorialday.ui.glass.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.time.LocalDate

/** 编辑表单状态和网络操作。 */
class AnniversaryEditViewModel : ViewModel() {
    var name by mutableStateOf(""); var description by mutableStateOf("")
    var calendarType by mutableStateOf(Anniversary.CALENDAR_SOLAR); private set
    var loopType by mutableStateOf(Anniversary.LOOP_YEARLY)
    var year by mutableStateOf(LocalDate.now().year.toString()); var month by mutableStateOf(LocalDate.now().monthValue.toString()); var day by mutableStateOf(LocalDate.now().dayOfMonth.toString())
    var leap by mutableStateOf(false)
    var loading by mutableStateOf(false); var saving by mutableStateOf(false); var loaded by mutableStateOf(false); var success by mutableStateOf(false)
    var error by mutableStateOf<String?>(null); var invalidDate by mutableStateOf(false)
    private var id = 0L
    private val repository = AnniversaryRepository()
    private var request: Disposable? = null

    /** 编辑前读取服务端详情；加载失败时可以重试。 */
    fun load(value: Long) {
        id = value
        if (value <= 0) { loaded = true; return }
        loading = true; error = null
        request?.dispose()
        request = repository.get(value).observeOn(AndroidSchedulers.mainThread()).subscribe({ item ->
            name = item.name; description = item.description; calendarType = item.calendarType; loopType = item.loopType
            year = item.year.toString(); month = item.month.toString(); day = item.day.toString(); leap = item.isLeapMonth == 1
            loaded = true; loading = false
        }, { loading = false; error = it.message ?: "Request failed" })
    }

    /** 在公历和农历之间切换时，尽量保留当前日期对应的同一天。 */
    fun changeCalendar(value: String) {
        if (value == calendarType) return
        val y = year.toIntOrNull(); val m = month.toIntOrNull(); val d = day.toIntOrNull()
        if (y != null && m != null && d != null && AnniversaryCalendar.isValid(y, m, d, calendarType == Anniversary.CALENDAR_LUNAR, leap)) {
            val date = try {
                AnniversaryCalendar.convert(y, m, d, calendarType == Anniversary.CALENDAR_LUNAR, leap)
            } catch (_: Exception) {
                // 个别边界日期无法换算时，保留原始输入并提示用户。
                invalidDate = true
                return
            }
            year = date.year.toString(); month = date.month.toString(); day = date.day.toString(); leap = date.leap
        } else { leap = false }
        calendarType = value; invalidDate = false
    }

    /** 保存日期弹窗中已经校验过的日期和历法。 */
    fun setDate(yearValue: Int, monthValue: Int, dayValue: Int, calendar: String, isLeap: Boolean) {
        year = yearValue.toString(); month = monthValue.toString(); day = dayValue.toString()
        calendarType = calendar; leap = isLeap; invalidDate = false
    }

    /** 校验日期后提交；提交期间防止重复请求。 */
    fun save() {
        if (saving || !loaded || name.isBlank()) return
        val y = year.toIntOrNull(); val m = month.toIntOrNull(); val d = day.toIntOrNull()
        val lunar = calendarType == Anniversary.CALENDAR_LUNAR
        if (y == null || m == null || d == null || !AnniversaryCalendar.isValid(y, m, d, lunar, leap)) { invalidDate = true; return }
        invalidDate = false; saving = true; error = null
        val item = Anniversary(id, name.trim(), description, calendarType, loopType, y, m, d, if (lunar && leap) 1 else 0)
        request = repository.save(item).observeOn(AndroidSchedulers.mainThread()).subscribe({ saving = false; success = true }, { saving = false; error = it.message ?: "Request failed" })
    }
    override fun onCleared() { request?.dispose() }
}

/** 新版纪念日新建和编辑页面。 */
@OptIn(ExperimentalMaterial3Api::class)
class AnniversaryEditActivity : BaseComposeActivity() {
    companion object { const val EXTRA_ID = "anniversary_id" }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[AnniversaryEditViewModel::class.java]
        val id = intent.getLongExtra(EXTRA_ID, 0)
        if (!model.loaded && !model.loading) model.load(id)
        setContent {
            if (model.success) LaunchedEffect(Unit) { setResult(RESULT_OK); finish() }
            var dateDialog by remember { mutableStateOf(false) }
            GlassPage {
                GlassNavigationBar(getString(if (id > 0) R.string.anniversary_edit else R.string.anniversary_add), { finish() }, action = {
                    Text(getString(R.string.save), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(enabled = model.loaded && !model.saving && model.name.isNotBlank()) { model.save() })
                })
                if (model.loading) Text(getString(R.string.account_wait))
                if (!model.loaded && !model.loading) { model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; GlassButton(getString(R.string.retry), { model.load(id) }) }
                if (model.loaded) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Text(getString(R.string.anniversary_name), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
                        GlassTextField(model.name, { model.name = it }, getString(R.string.anniversary_name))
                        Text(getString(R.string.anniversary_description), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        GlassTextField(model.description, { model.description = it }, getString(R.string.anniversary_description), modifier = Modifier.heightIn(min = 92.dp), singleLine = false)
                        Text(getString(R.string.anniversary_calendar), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        GlassSegments(listOf(Anniversary.CALENDAR_SOLAR to getString(R.string.anniversary_solar), Anniversary.CALENDAR_LUNAR to getString(R.string.anniversary_lunar)), model.calendarType) { model.changeCalendar(it) }
                        Text(getString(R.string.anniversary_date), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        val dateShape = RoundedCornerShape(14.dp)
                        Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White.copy(alpha = if (isGlassDark()) 0.10f else 0.55f), dateShape).border(1.dp, Color.White.copy(alpha = 0.45f), dateShape).clickable { dateDialog = true }.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${model.year}.${model.month}.${model.day}${if (model.leap) " · ${getString(R.string.anniversary_leap)}" else ""}", fontSize = 14.sp)
                            Text("›", fontSize = 20.sp, color = glassSecondaryColor())
                        }
                        if (model.invalidDate) Text(getString(R.string.anniversary_invalid_date), color = MaterialTheme.colorScheme.error)
                        Text(getString(R.string.anniversary_repeat), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Anniversary.LOOP_NONE to R.string.anniversary_no_repeat, Anniversary.LOOP_MONTHLY to R.string.anniversary_monthly, Anniversary.LOOP_YEARLY to R.string.anniversary_yearly).forEach { (value, label) ->
                                GlassChoice(getString(label), model.loopType == value, { model.loopType = value }, Modifier.weight(1f).height(64.dp))
                            }
                        }
                        model.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
                    }
                    GlassButton(getString(if (model.saving) R.string.anniversary_saving else R.string.save), { model.save() }, !model.saving && model.name.isNotBlank())
                }
                if (dateDialog) {
                    val yearState = rememberLazyListState((model.year.toIntOrNull() ?: LocalDate.now().year).coerceIn(1, 9999) - 1)
                    val monthState = rememberLazyListState((model.month.toIntOrNull() ?: 1).coerceIn(1, 12) - 1)
                    val dayState = rememberLazyListState((model.day.toIntOrNull() ?: 1).coerceIn(1, 31) - 1)
                    var leap by remember { mutableStateOf(model.leap) }
                    var selectedCalendar by remember { mutableStateOf(model.calendarType) }
                    val pickerScope = rememberCoroutineScope()
                    var invalid by remember { mutableStateOf(false) }
                    Dialog(onDismissRequest = { dateDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                        Box(Modifier.fillMaxSize().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 24.dp), contentAlignment = Alignment.BottomCenter) {
                            GlassCard(solid = true) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(getString(R.string.cancel), color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { dateDialog = false })
                                    Text(getString(R.string.anniversary_date), fontWeight = FontWeight.SemiBold)
                                    Text(getString(R.string.confirm), color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(enabled = !yearState.isScrollInProgress && !monthState.isScrollInProgress && !dayState.isScrollInProgress) {
                                        val y = yearState.firstVisibleItemIndex + 1; val m = monthState.firstVisibleItemIndex + 1; val d = dayState.firstVisibleItemIndex + 1
                                        if (AnniversaryCalendar.isValid(y, m, d, selectedCalendar == Anniversary.CALENDAR_LUNAR, leap)) {
                                            model.setDate(y, m, d, selectedCalendar, leap); dateDialog = false
                                        } else invalid = true
                                    })
                                }
                                Spacer(Modifier.height(16.dp))
                                GlassSegments(listOf(Anniversary.CALENDAR_SOLAR to getString(R.string.anniversary_solar), Anniversary.CALENDAR_LUNAR to getString(R.string.anniversary_lunar)), selectedCalendar) { target ->
                                    if (target != selectedCalendar && !yearState.isScrollInProgress && !monthState.isScrollInProgress && !dayState.isScrollInProgress) {
                                        val y = yearState.firstVisibleItemIndex + 1; val m = monthState.firstVisibleItemIndex + 1; val d = dayState.firstVisibleItemIndex + 1
                                        try {
                                            val converted = AnniversaryCalendar.convert(y, m, d, selectedCalendar == Anniversary.CALENDAR_LUNAR, leap)
                                            selectedCalendar = target; leap = converted.leap; invalid = false
                                            pickerScope.launch {
                                                yearState.scrollToItem(converted.year - 1)
                                                monthState.scrollToItem(converted.month - 1)
                                                dayState.scrollToItem(converted.day - 1)
                                            }
                                        } catch (_: Exception) { invalid = true }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                if (selectedCalendar == Anniversary.CALENDAR_LUNAR) {
                                    GlassSwitchRow(getString(R.string.anniversary_leap_record), leap) { leap = it }
                                    Spacer(Modifier.height(12.dp))
                                }
                                Row(Modifier.fillMaxWidth()) {
                                    GlassWheelPicker(1, 9999, yearState, Modifier.weight(1f))
                                    val chineseLunar = selectedCalendar == Anniversary.CALENDAR_LUNAR && resources.configuration.locales[0].language == "zh"
                                    GlassWheelPicker(1, 12, monthState, Modifier.weight(1f)) { if (chineseLunar) LunarDateLabels.month(it) else it.toString() }
                                    GlassWheelPicker(1, 31, dayState, Modifier.weight(1f)) { if (chineseLunar) LunarDateLabels.day(it) else it.toString() }
                                }
                                if (invalid) Text(getString(R.string.anniversary_invalid_date), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
