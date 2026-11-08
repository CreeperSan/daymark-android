package com.creepersan.memorialday.activity.import_legacy

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.creepersan.memorialday.R
import com.creepersan.memorialday.application.MemorialDayApplication
import com.creepersan.memorialday.base.activity.BaseComposeActivity
import com.creepersan.memorialday.database.model.MemorialDay
import com.creepersan.memorialday.module.anniversary.AnniversaryRepository
import com.creepersan.memorialday.module.user.UserManager
import com.creepersan.memorialday.ui.glass.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

private const val IMPORT_PREFERENCES = "legacy_import"

/** 经典数据加载、逐条导入以及已成功条目的记录。 */
class ImportLegacyViewModel : ViewModel() {
    var items by mutableStateOf<List<MemorialDay>>(emptyList()); private set
    var selected by mutableStateOf<Set<Long>>(emptySet()); private set
    var loading by mutableStateOf(false); private set
    var importing by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var processed by mutableStateOf(0); private set
    var total by mutableStateOf(0); private set
    private val repository = AnniversaryRepository()
    private var request: Disposable? = null
    private val preferences = MemorialDayApplication.instance.getSharedPreferences(IMPORT_PREFERENCES, Context.MODE_PRIVATE)
    private val userId get() = UserManager.session()?.id ?: 0L

    /** 已成功上传的条目按用户分别记录，重试时不再重复提交。 */
    private fun key(item: MemorialDay) = "${userId}_${item.id}"
    fun load() {
        loading = true; error = null
        request = Single.fromCallable { MemorialDayApplication.instance.databaseManager.memorialDatabase.memorialDayDao().getAll() }
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ all ->
                items = all.filterNot { preferences.getBoolean(key(it), false) }
                selected = items.map { it.id }.toSet(); loading = false
            }, { loading = false; error = it.message })
    }
    fun toggle(id: Long) { selected = if (id in selected) selected - id else selected + id }
    fun selectAll() { selected = if (selected.size == items.size) emptySet() else items.map { it.id }.toSet() }

    /** 逐条上传；成功后立即记录，失败时保留未导入项供重试。 */
    fun importSelected() {
        if (importing || selected.isEmpty() || userId <= 0) return
        val pending = items.filter { it.id in selected }
        val importUserId = userId
        total = pending.size; processed = 0; importing = true; error = null
        request = Observable.fromIterable(pending).concatMapSingle { item ->
            val anniversary = LegacyAnniversaryConverter.convert(item)
            repository.save(anniversary).map {
                // 下一条请求开始前同步落盘，缩小成功后重复导入的窗口。
                check(preferences.edit().putBoolean("${importUserId}_${item.id}", true).commit()) { "Could not save import progress" }
                item
            }
        }.observeOn(AndroidSchedulers.mainThread()).subscribe({ item ->
            items = items.filterNot { it.id == item.id }; selected = selected - item.id; processed++
        }, { importing = false; error = it.message }, { importing = false })
    }
    override fun onCleared() { request?.dispose() }
}

/** 选择经典版条目并导入当前账号。 */
class ImportLegacyActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[ImportLegacyViewModel::class.java]
        if (!model.loading && model.items.isEmpty() && model.error == null) model.load()
        setContent {
            BackHandler(enabled = model.importing) { }
            GlassPage {
                Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("‹", fontSize = 28.sp, modifier = Modifier.clickable(enabled = !model.importing) { finish() })
                    Text(getString(R.string.import_legacy), fontSize = 20.sp, modifier = Modifier.padding(top = 6.dp))
                }
                Text(getString(R.string.import_hint), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(getString(R.string.import_selected, model.selected.size, model.items.size))
                    Text(getString(R.string.import_select_all), color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(enabled = !model.importing) { model.selectAll() })
                }
                if (model.loading) Text(getString(R.string.account_wait))
                model.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    if (model.total > 0) Text(getString(R.string.import_uncertain), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    else Text(getString(R.string.retry), color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { model.load() }.padding(top = 8.dp))
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(model.items, key = { it.id }) { item ->
                        GlassCard {
                            Row(Modifier.fillMaxWidth().clickable(enabled = !model.importing) { model.toggle(item.id) }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(if (item.id in model.selected) "●" else "○", color = MaterialTheme.colorScheme.primary)
                                Text(item.title)
                            }
                        }
                    }
                }
                if (model.importing) Text(getString(R.string.import_progress, model.processed, model.total))
                GlassButton(getString(R.string.import_count, model.selected.size), { model.importSelected() }, !model.importing && model.selected.isNotEmpty() && (model.error == null || model.total == 0))
            }
        }
    }
}
