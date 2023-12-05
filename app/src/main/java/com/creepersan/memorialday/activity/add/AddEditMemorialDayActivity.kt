package com.creepersan.memorialday.activity.add

import android.os.Bundle
import android.view.MenuItem
import androidx.core.widget.addTextChangedListener
import com.creepersan.memorialday.R
import com.creepersan.memorialday.application.MemorialDayApplication
import com.creepersan.memorialday.base.activity.BaseViewBindingActivity
import com.creepersan.memorialday.constance.MDScheduler
import com.creepersan.memorialday.database.model.MemorialDay
import com.creepersan.memorialday.databinding.ActivityAddEditMemorialDayBinding
import com.creepersan.memorialday.extension.disposeIfNeed
import com.creepersan.memorialday.extension.setVisible
import com.creepersan.memorialday.ui.dialog.DialogManager
import com.creepersan.memorialday.ui.system.StatusBarManager
import com.creepersan.memorialday.ui.toast.ToastManager
import com.creepersan.memorialday.ui.dialog.date_picker.DatePickerDialog
import com.creepersan.memorialday.utils.chinese_calendar.ChineseCalendarUtils
import com.creepersan.memorialday.utils.chinese_calendar.model.lunarDayToString
import com.creepersan.memorialday.utils.chinese_calendar.model.lunarMonthToString
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.lang.NullPointerException
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddEditMemorialDayActivity: BaseViewBindingActivity<ActivityAddEditMemorialDayBinding>() {

    companion object {
        const val INTENT_TYPE = "type"
        const val INTENT_TYPE_ADD = "add"
        const val INTENT_TYPE_EDIT = "edit"

        const val INTENT_MEMORIAL_DAY_ID = "memorial_day_id"
    }

    private var intentType = INTENT_TYPE_ADD
    private var intentMemorialID = 0L

    private var isEdit = false
    private lateinit var memorialDayModel: MemorialDay
    private var databaseDisposable: Disposable? = null
    private val dateFormatter = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* 初始化系统状态栏颜色 */
        StatusBarManager.setSystemStatusBarContentColor(window, StatusBarManager.ContentColor.DARK)

        initIntent()
        initUI()
        initData()
    }

    private fun initIntent() {
        intentType = intent.getStringExtra(INTENT_TYPE) ?: intentType
        intentMemorialID = intent.getLongExtra(INTENT_MEMORIAL_DAY_ID, intentMemorialID)

        isEdit = intentType == INTENT_TYPE_EDIT
    }

    private fun initUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        binding.pageTitle.text = if (isEdit) "编辑纪念日" else "添加纪念日"
        binding.delete.setVisible(isEdit)

        binding.title.addTextChangedListener { text ->
            if (!this::memorialDayModel.isInitialized) return@addTextChangedListener
            memorialDayModel = memorialDayModel.copy(
                title = text.toString()
            )
        }

        binding.sectionTimePickGregorian.setOnClickListener {

            val memorialDayCalendar = Calendar.getInstance()
            memorialDayCalendar.time = Time(memorialDayModel.time)

            DatePickerDialog(
                context = this,
                selectionYear =  memorialDayCalendar.get(Calendar.YEAR),
                selectionMonth = memorialDayCalendar.get(Calendar.MONTH) + 1,
                selectionDay = memorialDayCalendar.get(Calendar.DAY_OF_MONTH),
                showClearButton = false,
                useLunar = false,
                onDayConfirmListener = { chineseCalendar, useLunar ->
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.YEAR, chineseCalendar.year)
                    calendar.set(Calendar.MONDAY, chineseCalendar.month - 1)
                    calendar.set(Calendar.DAY_OF_MONTH, chineseCalendar.day)
                    calendar.set(Calendar.HOUR, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    memorialDayModel = memorialDayModel.copy(
                        time = calendar.time.time,
                        timeType = MemorialDay.TIME_TYPE_GREGORIAN,
                    )
                    refreshTimeText()
                }
            ).show()
            return@setOnClickListener
        }

        binding.sectionTimePickLunar.setOnClickListener {

            val memorialDayCalendar = Calendar.getInstance()
            memorialDayCalendar.time = Time(memorialDayModel.time)

            DatePickerDialog(
                context = this,
                selectionYear =  memorialDayCalendar.get(Calendar.YEAR),
                selectionMonth = memorialDayCalendar.get(Calendar.MONTH) + 1,
                selectionDay = memorialDayCalendar.get(Calendar.DAY_OF_MONTH),
                showClearButton = false,
                useLunar = true,
                onDayConfirmListener = { chineseCalendar, useLunar ->
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.YEAR, chineseCalendar.year)
                    calendar.set(Calendar.MONDAY, chineseCalendar.month - 1)
                    calendar.set(Calendar.DAY_OF_MONTH, chineseCalendar.day)
                    calendar.set(Calendar.HOUR, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    memorialDayModel = memorialDayModel.copy(
                        time = calendar.time.time,
                        timeType = MemorialDay.TIME_TYPE_LUNAR,
                    )
                    refreshTimeText()
                }
            ).show()
            return@setOnClickListener
        }

        binding.save.setOnClickListener {
            if (memorialDayModel.title.trim().isEmpty()) {
                ToastManager.message(this, "请输入名称")
                return@setOnClickListener
            }

            if (memorialDayModel.time <= 0) {
                ToastManager.message(this, "请选择时间")
                return@setOnClickListener
            }

            databaseDisposable.disposeIfNeed()
            databaseDisposable = Observable.just(Unit).observeOn(MDScheduler.database).map {
                MemorialDayApplication.instance.databaseManager.memorialDatabase.memorialDayDao()
            }.map { dao ->
                if (isEdit) {
                    dao.update(memorialDayModel)
                } else {
                    dao.insert(memorialDayModel)
                }
            }.observeOn(MDScheduler.ui).subscribe({
                setResult(RESULT_OK)
                finish()
            }, { error ->
                error.printStackTrace()
            })

            MemorialDayApplication.instance.databaseManager.memorialDatabase.memorialDayDao()
        }

        binding.delete.setOnClickListener {
            databaseDisposable.disposeIfNeed()
            databaseDisposable = DialogManager.confirm(
                context = this,
                title = "确定要删除吗？",
                message = "删除后将无法恢复",
            ).map { confirm ->
                if (!confirm) {
                    throw InterruptedException("用户取消")
                }
            }.observeOn(MDScheduler.database).map {
                MemorialDayApplication.instance.databaseManager.memorialDatabase.memorialDayDao()
            }.map { dao ->
                dao.delete(memorialDayModel)
            }.observeOn(MDScheduler.ui).subscribe({
                setResult(RESULT_OK)
                finish()
            }, { error ->
                error.printStackTrace()
            })
        }

        binding.sectionLoopOnce.setOnClickListener {
            memorialDayModel = memorialDayModel.copy(
                loop = MemorialDay.LOOP_ONCE
            )
            refreshLoopMode()
        }
        binding.sectionLoopYear.setOnClickListener {
            memorialDayModel = memorialDayModel.copy(
                loop = MemorialDay.LOOP_YEARLY
            )
            refreshLoopMode()
        }
    }

    private fun initData() {
        databaseDisposable = Observable.just(Unit).observeOn(MDScheduler.database).map {
            MemorialDayApplication.instance.databaseManager.memorialDatabase.memorialDayDao()
        }.map { dao ->
            if (isEdit) {
                dao.findMemorialDay(intentMemorialID) ?: throw NullPointerException("纪念日不存在 id=$intentMemorialID")
            } else {
                MemorialDay.empty()
            }
        }.observeOn(MDScheduler.ui).subscribe({ model ->
            memorialDayModel = model
            initUiAfterDataLoaded()
        }, { error ->
            error.printStackTrace()
            setResult(RESULT_CANCELED)
            finish()
        })
    }

    private fun initUiAfterDataLoaded() {
        binding.title.setText(memorialDayModel.title)

        refreshTimeText()
        refreshLoopMode()
    }


    private fun refreshLoopMode() {
        when (memorialDayModel.loop) {
            MemorialDay.LOOP_YEARLY -> {
                binding.sectionLoopYear.setBackgroundResource(R.drawable.bg_md_selection_active)
                binding.sectionLoopOnce.setBackgroundResource(R.drawable.bg_md_selection_inactive)
            }
            MemorialDay.LOOP_ONCE -> {
                binding.sectionLoopYear.setBackgroundResource(R.drawable.bg_md_selection_inactive)
                binding.sectionLoopOnce.setBackgroundResource(R.drawable.bg_md_selection_active)
            }
        }
    }


    private fun refreshTimeText() {
        when (memorialDayModel.timeType) {
            MemorialDay.TIME_TYPE_GREGORIAN -> {
                binding.sectionTimeText.text = dateFormatter.format(Date(memorialDayModel.time))
            }
            MemorialDay.TIME_TYPE_LUNAR -> {
                val tmpCalendar = Calendar.getInstance()
                tmpCalendar.time = Time(memorialDayModel.time)
                val tmpYear = tmpCalendar.get(Calendar.YEAR)
                val tmpMonth = tmpCalendar.get(Calendar.MONTH) + 1
                val tmpDay = tmpCalendar.get(Calendar.DAY_OF_MONTH)
                val disposable = ChineseCalendarUtils.getFromCalendar(year = tmpYear, month = tmpMonth, day = tmpDay).subscribe({ calendarOptional ->
                    if (calendarOptional.isPresent) {
                        val calendar = calendarOptional.get()
                        var displayStr = ""
                        displayStr += "${calendar.lunarYearName}年"
                        displayStr += "${calendar.lunarMonthToString()}"
                        displayStr += "${calendar.lunarDayToString()}"
                        binding.sectionTimeText.text = displayStr
                    }
                }, { error ->
                    error.printStackTrace()
                })
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                setResult(RESULT_CANCELED)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

}