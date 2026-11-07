package com.creepersan.memorialday.activity.anniversary

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
            GlassPage {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(getString(if (id > 0) R.string.anniversary_edit else R.string.anniversary_add), fontSize = 28.sp)
                    if (model.loading) CircularProgressIndicator()
                    if (!model.loaded && !model.loading) { model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; GlassButton(getString(R.string.retry), { model.load(id) }) }
                    if (model.loaded) GlassCard {
                        OutlinedTextField(model.name, { model.name = it }, label = { Text(getString(R.string.anniversary_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(model.description, { model.description = it }, label = { Text(getString(R.string.anniversary_description)) }, modifier = Modifier.fillMaxWidth())
                        Text(getString(R.string.anniversary_calendar), modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium)
                        Row { listOf(Anniversary.CALENDAR_SOLAR to R.string.anniversary_solar, Anniversary.CALENDAR_LUNAR to R.string.anniversary_lunar).forEach { (value, label) ->
                            Row(Modifier.weight(1f).clickable { model.changeCalendar(value) }) { RadioButton(model.calendarType == value, onClick = { model.changeCalendar(value) }); Text(getString(label), modifier = Modifier.padding(top = 12.dp)) }
                        } }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(model.year, { model.year = it }, label = { Text(getString(R.string.anniversary_year)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1.3f))
                            OutlinedTextField(model.month, { model.month = it }, label = { Text(getString(R.string.anniversary_month)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(model.day, { model.day = it }, label = { Text(getString(R.string.anniversary_day)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        }
                        if (model.calendarType == Anniversary.CALENDAR_LUNAR) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(getString(R.string.anniversary_leap), modifier = Modifier.padding(top = 12.dp)); Switch(model.leap, { model.leap = it }) }
                        if (model.invalidDate) Text(getString(R.string.anniversary_invalid_date), color = MaterialTheme.colorScheme.error)
                        Text(getString(R.string.anniversary_repeat), modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium)
                        listOf(Anniversary.LOOP_NONE to R.string.anniversary_no_repeat, Anniversary.LOOP_YEARLY to R.string.anniversary_yearly, Anniversary.LOOP_MONTHLY to R.string.anniversary_monthly).forEach { (value, label) ->
                            Row(Modifier.fillMaxWidth().clickable { model.loopType = value }) { RadioButton(model.loopType == value, onClick = { model.loopType = value }); Text(getString(label), modifier = Modifier.padding(top = 12.dp)) }
                        }
                        model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        GlassButton(getString(if (model.saving) R.string.anniversary_saving else R.string.save), { model.save() }, !model.saving && model.name.isNotBlank())
                    }
                }
            }
        }
    }
}
