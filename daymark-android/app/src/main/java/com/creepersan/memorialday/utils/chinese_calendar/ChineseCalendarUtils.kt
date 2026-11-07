package com.creepersan.memorialday.utils.chinese_calendar

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.creepersan.memorialday.application.MemorialDayApplication
import com.creepersan.memorialday.constance.MDScheduler
import com.creepersan.memorialday.utils.chinese_calendar.definition.NumberEqual
import com.creepersan.memorialday.utils.chinese_calendar.definition.NumberLessThen
import com.creepersan.memorialday.utils.chinese_calendar.definition.NumberLessThenOrEqual
import com.creepersan.memorialday.utils.chinese_calendar.definition.NumberLimit
import com.creepersan.memorialday.utils.chinese_calendar.definition.NumberMoreThan
import com.creepersan.memorialday.utils.chinese_calendar.definition.NumberMoreThanOrEqual
import com.creepersan.memorialday.utils.chinese_calendar.definition.Sort
import com.creepersan.memorialday.utils.chinese_calendar.model.CalendarMonth
import com.creepersan.memorialday.utils.chinese_calendar.model.CalendarYear
import com.creepersan.memorialday.utils.chinese_calendar.model.ChineseCalendar
import com.creepersan.memorialday.utils.chinese_calendar.model.ChineseCalendarDay
import com.creepersan.memorialday.utils.chinese_calendar.model.ChineseCalendarMonth
import com.creepersan.memorialday.utils.chinese_calendar.model.ChineseCalendarSolarTerm
import com.creepersan.memorialday.utils.chinese_calendar.model.ChineseCalendarYear
import com.creepersan.memorialday.utils.chinese_calendar.model.ChineseCalendarZodiac
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.Function
import java.io.FileOutputStream
import java.util.ArrayList
import java.util.Calendar
import java.util.Optional

object ChineseCalendarUtils {

    private const val PATH_ASSETS_DATABASE = "chinese_calendar/data.db"
    private const val PATH_INNER_DATABASE = "/data/data/com.creepersan.memorialday/databases/chinese_calendar.db"

    private const val TABLE_NAME = "days"
    private const val COL_ID = "_id"
    private const val COL_YEAR = "year"
    private const val COL_MONTH = "month"
    private const val COL_DAY = "day"
    private const val COL_WEEKDAY = "weekday"
    private const val COL_LUNAR_YEAR = "lunar_year"
    private const val COL_LUNAR_MONTH = "lunar_month"
    private const val COL_LUNAR_DAY = "lunar_day"
    private const val COL_IS_LEAP_MONTH = "is_leap_month"
    private const val COL_LUNAR_YEAR_NAME = "lunar_year_name"
    private const val COL_LUNAR_MONTH_NAME = "lunar_month_name"
    private const val COL_LUNAR_DAY_NAME = "lunar_day_name"
    private const val COL_SOLAR_TERM = "solar_term"
    private const val COL_SOLAR_TERM_NAME = "solar_term_name"
    private const val COL_ZODIAC = "zodiac"
    private const val COL_ZODIAC_NAME = "zodiac_name"
    private const val COL_RAW = "raw"

    private const val SQL_SELECTION_CALENDAR = "$COL_ID, $COL_YEAR, $COL_MONTH, $COL_DAY, $COL_WEEKDAY, " +
            "$COL_LUNAR_YEAR, $COL_LUNAR_MONTH, $COL_LUNAR_DAY, $COL_IS_LEAP_MONTH, $COL_LUNAR_YEAR_NAME, " +
            "$COL_SOLAR_TERM, $COL_ZODIAC"

    private var database: SQLiteDatabase

    init {
        /* 先检查数据库文件是否存在，如果不存在，则拷贝到应用内部数据库目录中 */
        try {
            database = SQLiteDatabase.openDatabase(PATH_INNER_DATABASE, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: Exception) {
            // 数据库不存在，拷贝数据库
            e.printStackTrace()
            val assetDatabaseFileInputStream = MemorialDayApplication.instance.assets.open(
                PATH_ASSETS_DATABASE
            )
            val innerDatabaseFileOutputStream = FileOutputStream(PATH_INNER_DATABASE)
            val buffer = ByteArray(1024)
            var length = assetDatabaseFileInputStream.read(buffer)
            while (length > 0) {
                innerDatabaseFileOutputStream.write(buffer, 0, length)
                length = assetDatabaseFileInputStream.read(buffer)
            }
            innerDatabaseFileOutputStream.flush()
            innerDatabaseFileOutputStream.close()
            assetDatabaseFileInputStream.close()
            database = SQLiteDatabase.openDatabase(PATH_INNER_DATABASE, null, SQLiteDatabase.OPEN_READONLY)
        }

        println("Finish!")
    }


    /**
     * 获取可以支持的年份
     * @return 可以支持的年份列表
     */
    fun getAvailableYear(): Observable<ArrayList<CalendarYear>> {
        return Observable.just(Unit).observeOn(MDScheduler.database).map {
            val cursor = database.rawQuery("SELECT DISTINCT $COL_YEAR FROM $TABLE_NAME", null)
            val yearList = ArrayList<CalendarYear>()
            while (cursor.moveToNext()) {
                val cursorYear = cursor.getInt(0)
                yearList.add(CalendarYear(
                    year = cursorYear,
                    name = "${cursorYear}年",
                ))
            }
            cursor.close()
            yearList
        }.observeOn(MDScheduler.ui)
    }


    /**
     * 获取可以支持的年份
     * @return 可以支持的年份列表
     */
    fun getAvailableChineseYear(): Observable<ArrayList<ChineseCalendarYear>> {
        return Observable.just(Unit).observeOn(MDScheduler.database).map {
            val cursor = database.rawQuery("SELECT DISTINCT $COL_LUNAR_YEAR, $COL_LUNAR_YEAR_NAME, $COL_ZODIAC FROM $TABLE_NAME", null)
            val yearList = ArrayList<ChineseCalendarYear>()
            while (cursor.moveToNext()) {
                yearList.add(ChineseCalendarYear(
                    year = cursor.getInt(0),
                    name = cursor.getString(1) ?: "",
                    zodiac = ChineseCalendarZodiac.fromInt(cursor.getInt(2)),
                ))
            }
            cursor.close()
            yearList
        }.observeOn(MDScheduler.ui)
    }


    /**
     * 获取可以支持的公历月份
     * @param year 公历
     * @return 可以支持的月份列表
     */
    fun getAvailableMonth(year: Int): Observable<ArrayList<CalendarMonth>> {
        return Observable.just(ArrayList<CalendarMonth>()).observeOn(MDScheduler.database).map { resultList ->
            database.rawQuery("SELECT DISTINCT $COL_MONTH FROM $TABLE_NAME WHERE $COL_YEAR = ?", arrayOf(year.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    val cursorMonth = cursor.getInt(0)
                    resultList.add(CalendarMonth(
                        year = year,
                        month = cursorMonth,
                        name = "${cursorMonth}月",
                    ))
                }
            }
            resultList
        }.observeOn(MDScheduler.ui)
    }


    /**
     * 获取可以支持的农历月份
     * @param year 农历年份
     * @return 可以支持的月份列表
     */
    fun getAvailableChineseMonth(year: Int): Observable<ArrayList<ChineseCalendarMonth>> {
        return Observable.just(ArrayList<ChineseCalendarMonth>()).observeOn(MDScheduler.database).map { resultList ->
            database.rawQuery("SELECT DISTINCT $COL_LUNAR_MONTH, $COL_LUNAR_MONTH_NAME, $COL_IS_LEAP_MONTH FROM $TABLE_NAME WHERE $COL_LUNAR_YEAR = ?", arrayOf(year.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    resultList.add(ChineseCalendarMonth(
                        year = year,
                        month = cursor.getInt(0),
                        name = cursor.getString(1) ?: "",
                        isLeap = cursor.getInt(2) == 1
                    ))
                }
            }
            resultList
        }.observeOn(MDScheduler.ui)
    }


    /**
     * 获取可以支持的日期
     * @param year 农历年份
     * @param month 农历月份
     * @param isLeapMonth 是否闰月
     */
    fun getAvailableDay(year: Int, month: Int, isLeapMonth: Boolean): Observable<ArrayList<ChineseCalendarDay>> {
        return Observable.just(ArrayList<ChineseCalendarDay>()).observeOn(MDScheduler.database).map { resultList ->
            database.rawQuery("SELECT $COL_DAY, $COL_LUNAR_DAY, $COL_LUNAR_DAY_NAME, $COL_SOLAR_TERM, $COL_SOLAR_TERM_NAME, $COL_ZODIAC, $COL_ZODIAC_NAME FROM $TABLE_NAME WHERE $COL_LUNAR_YEAR = ? AND $COL_LUNAR_MONTH = ? AND $COL_IS_LEAP_MONTH = ?", arrayOf(year.toString(), month.toString(), if (isLeapMonth) "1" else "0")).use { cursor ->
                while (cursor.moveToNext()) {
                    resultList.add(ChineseCalendarDay(
                        year = year,
                        month = month,
                        isLeapMonth = isLeapMonth,
                        day = cursor.getInt(0),
                        name = cursor.getString(2) ?: "",
                        solarTerm = ChineseCalendarSolarTerm.fromInt(cursor.getInt(3)),
                    ))
                }
            }
            resultList
        }.observeOn(MDScheduler.ui)
    }


    /**
     * 公历转农历
     * @param year 公历年
     * @param month 公历月
     * @param day 公历日
     */
    fun getFromCalendar(year: Int, month: Int, day: Int): Observable<Optional<ChineseCalendar>> {
        return Observable.just(Unit).observeOn(MDScheduler.database).map {
            val sql = "SELECT $SQL_SELECTION_CALENDAR " +
                    "FROM $TABLE_NAME " +
                    "WHERE $COL_YEAR = ? AND $COL_MONTH = ? AND $COL_DAY = ? "
            Log.e("日历打印","getFromCalendar: $sql".replace("?", "%s").format(year, month, day))
            database.rawQuery(sql, arrayOf(year.toString(), month.toString(), day.toString())).use { cursor ->
                if (cursor.moveToNext()) {
                    val chineseCalendar = cursor.convertFromSQLSelectionCalendar()
                    Optional.of(chineseCalendar)
                } else {
                    Optional.empty()
                }
            }

        }.observeOn(MDScheduler.ui)
    }


    /**
     * 农历转公历
     * @param lunarYear 农历年
     * @param lunarMonth 农历月
     * @param lunarDay 农历日
     * @param isLeapMonth 是否闰月
     */
    fun getFromChineseCalendar(
        lunarYear: Int,
        lunarMonth: Int,
        lunarDay: Int,
        isLeapMonth: Boolean,
    ): Observable<Optional<ChineseCalendar>> {
        return Observable.just(Unit).observeOn(MDScheduler.database).map {
            val sql = "SELECT $SQL_SELECTION_CALENDAR " +
                    "FROM $TABLE_NAME " +
                    "WHERE $COL_LUNAR_YEAR = ? AND $COL_LUNAR_MONTH = ? AND $COL_LUNAR_DAY = ? AND $COL_IS_LEAP_MONTH = ?"
            database.rawQuery(sql, arrayOf(lunarYear.toString(), lunarMonth.toString(), lunarDay.toString(), if (isLeapMonth) "1" else "0")).use {
                if (it.moveToNext()) {
                    val chineseCalendar = it.convertFromSQLSelectionCalendar()
                    Optional.of(chineseCalendar)
                } else {
                    Optional.empty()
                }
            }
        }.observeOn(MDScheduler.ui)
    }


    /**
     * 获取两个农历日期之间的天数
     */
    fun getDayBetweenChineseCalendar(
        lunarYear: Int,
        lunarMonth: Int,
        lunarDay: Int,
        isLeapMonth: Boolean,
        lunarYear2: Int,
        lunarMonth2: Int,
        lunarDay2: Int,
        isLeapMonth2: Boolean,
    ): Observable<Int> {
        return Observable.just(Unit).observeOn(MDScheduler.database).concatMap {
            return@concatMap Observable.zip(
                getFromChineseCalendar(lunarYear, lunarMonth, lunarDay, isLeapMonth),
                getFromChineseCalendar(lunarYear2, lunarMonth2, lunarDay2, isLeapMonth2)
            ) { calendarObj1, calendarObj2 ->
                if (!calendarObj1.isPresent) {
                    return@zip Int.MIN_VALUE
                }
                if (!calendarObj2.isPresent) {
                    return@zip Int.MIN_VALUE
                }
                val calendar1 = calendarObj1.get()
                val calendar2 = calendarObj2.get()
                return@zip calendar1.id - calendar2.id
            }
        }.observeOn(MDScheduler.ui)
    }


    /**
     * 获取今天和指定农历日期之间的天数
     * @param lunarYear 农历年
     * @param lunarMonth 农历月
     * @param lunarDay 农历日
     * @param isLeapMonth 是否闰月
     */
    fun getDayBetweenTodayAndChineseCalendar(lunarYear: Int, lunarMonth: Int, lunarDay: Int, isLeapMonth: Boolean): Observable<Int> {
        return Observable.just(Unit).observeOn(MDScheduler.database).concatMap {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            return@concatMap getFromCalendar(year, month, day)
        }.zipWith(getFromChineseCalendar(lunarYear, lunarMonth, lunarDay, isLeapMonth)) { calendarTodayObj, calendarChineseOb ->
            if (!calendarTodayObj.isPresent) {
                return@zipWith Int.MIN_VALUE
            }
            if (!calendarChineseOb.isPresent) {
                return@zipWith Int.MIN_VALUE
            }
            val calendarToday = calendarTodayObj.get()
            val calendarChinese = calendarChineseOb.get()
            return@zipWith calendarToday.id - calendarChinese.id
        }
    }


    /**
     * 获取从特定日期开始的后面的指定天数
     * @param year 年份
     * @param month 月份，1 代表 一月
     */
    fun getDaysFromSpecifyWithCount(year: Int, month:Int, day: Int, limitSize: Int): Observable<List<ChineseCalendar>> {
        return Observable.just(Unit).observeOn(MDScheduler.database).map {
            val sql = "SELECT $COL_ID FROM $TABLE_NAME WHERE $COL_YEAR = ? AND $COL_MONTH = ? AND $COL_DAY = ? LIMIT 1"
            val cursor = database.rawQuery(sql, arrayOf(year.toString(), month.toString(), day.toString()))
            var idInDatabase = 0L
            if (cursor.moveToNext()) {
                val colIndex = cursor.getColumnIndex(COL_ID)
                if (colIndex < 0) {
                    throw InterruptedException("找不到对应日期")
                }
                idInDatabase = cursor.getLong(colIndex)
            }
            cursor.close()
            return@map idInDatabase
        }.map { databaseID ->
            val sql = "SELECT $SQL_SELECTION_CALENDAR FROM $TABLE_NAME WHERE $COL_ID >= ? LIMIT $limitSize"
            val calendarList = ArrayList<ChineseCalendar>()
            val cursor = database.rawQuery(sql, arrayOf(databaseID.toString()))
            while (cursor.moveToNext()) {
                val chineseCalendar = cursor.convertFromSQLSelectionCalendar()
                calendarList.add(chineseCalendar)
            }
            cursor.close()
            return@map calendarList.toList()
        }.onErrorResumeNext(Function { error ->
            error.printStackTrace()
             return@Function Observable.just(listOf<ChineseCalendar>())
        }).observeOn(MDScheduler.ui)
    }


    /**
     * 获取指定日期的农历信息，如果不传入对应条件则不进行这个条件的筛选
     */
    fun getChineseCalendarWithParams(
        year: NumberLimit? = null,
        month: NumberLimit? = null,
        day: NumberLimit? = null,
        weekday: NumberLimit? = null,
        lunarYear: NumberLimit? = null,
        lunarMonth: NumberLimit? = null,
        lunarDay: NumberLimit? = null,
        isLeapMonth: Boolean? = null,
        limit: Int? = null,
        offset: Int? = null,
        sort: Sort? = Sort.Asc,

    ): Observable<List<ChineseCalendar>> {
        return Observable.just(Unit)
            .observeOn(MDScheduler.database)
            .map {
                var sql = "SELECT $SQL_SELECTION_CALENDAR FROM $TABLE_NAME"
                if (year != null || month != null || day != null || weekday != null || lunarYear != null || lunarMonth != null || lunarDay != null || isLeapMonth != null) {
                    sql += " WHERE 1 = 1 "
                }
                val numberLimitConvertToSQLPart: ((colName: String, limit: NumberLimit) -> String) = { colName, limit ->
                    when (limit) {
                        is NumberEqual              -> "AND $colName = ${limit.number} "
                        is NumberMoreThan           -> "AND $colName > ${limit.number} "
                        is NumberMoreThanOrEqual    -> "AND $colName >= ${limit.number} "
                        is NumberLessThen           -> "AND $colName < ${limit.number} "
                        is NumberLessThenOrEqual    -> "AND $colName <= ${limit.number} "
                        else -> ""
                    }
                }
                if (year != null) {
                    sql += numberLimitConvertToSQLPart(COL_YEAR, year)
                }
                if (month != null) {
                    sql += numberLimitConvertToSQLPart(COL_MONTH, month)
                }
                if (day != null) {
                    sql += numberLimitConvertToSQLPart(COL_DAY, day)
                }
                if (weekday != null) {
                    sql += numberLimitConvertToSQLPart(COL_WEEKDAY, weekday)
                }
                if (lunarYear != null) {
                    sql += numberLimitConvertToSQLPart(COL_LUNAR_YEAR, lunarYear)
                }
                if (lunarMonth != null) {
                    sql += numberLimitConvertToSQLPart(COL_LUNAR_MONTH, lunarMonth)
                }
                if (lunarDay != null) {
                    sql += numberLimitConvertToSQLPart(COL_LUNAR_DAY, lunarDay)
                }
                if (isLeapMonth != null) {
                    sql += "AND $COL_IS_LEAP_MONTH = ${if (isLeapMonth) 1 else 0} "
                }
                sql += "ORDER BY $COL_ID "
                if (sort != null) {
                    sql += when (sort) {
                        Sort.Asc -> "ASC "
                        Sort.Desc -> "DESC "
                    }
                }
                if (limit != null) {
                    sql += "LIMIT $limit "
                }
                if (offset != null) {
                    sql += "OFFSET $offset "
                }
                return@map sql
            }
            .map { sql ->
                val cursor = database.rawQuery(sql, null)
                val resultList = ArrayList<ChineseCalendar>()
                while (cursor.moveToNext()) {
                    resultList.add(cursor.convertFromSQLSelectionCalendar())
                }
                cursor.close()
                return@map resultList.toList()
            }
            .observeOn(MDScheduler.ui)
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // 以下是一些内部使用的通用代码

    @SuppressLint("Range")
    private fun Cursor.convertFromSQLSelectionCalendar(): ChineseCalendar {
        return ChineseCalendar(
            id = getInt(getColumnIndex(COL_ID)),
            year = getInt(getColumnIndex(COL_YEAR)),
            month = getInt(getColumnIndex(COL_MONTH)),
            day = getInt(getColumnIndex(COL_DAY)),
            weekday = getInt(getColumnIndex(COL_WEEKDAY)),
            lunarYear = getInt(getColumnIndex(COL_LUNAR_YEAR)),
            lunarMonth = getInt(getColumnIndex(COL_LUNAR_MONTH)),
            lunarDay = getInt(getColumnIndex(COL_LUNAR_DAY)),
            isLeapMonth = getInt(getColumnIndex(COL_IS_LEAP_MONTH)) == 1,
            lunarYearName = getString(getColumnIndex(COL_LUNAR_YEAR_NAME)) ?: "",
            solarTerm = ChineseCalendarSolarTerm.fromInt(getInt(getColumnIndex(COL_SOLAR_TERM))),
            zodiac = ChineseCalendarZodiac.fromInt(getInt(getColumnIndex(COL_ZODIAC))),
        )
    }

}