package com.creepersan.memorialday.application

import android.app.Application
import android.util.Log
import com.creepersan.memorialday.database.DatabaseManager
import com.creepersan.memorialday.module.storage.kv.KVStorageManager
import com.creepersan.memorialday.store.AppStateStore
import com.creepersan.memorialday.store.BaseStore
import com.creepersan.memorialday.store.DeviceInfoStore
import com.creepersan.memorialday.store.UserStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DaymarkApplication : Application() {

    companion object {
        lateinit var instance: DaymarkApplication
    }

    @Deprecated("老版本的纪念日管理")
    lateinit var databaseManager: DatabaseManager

    /* 全局初始化状态 */
    private val _initializeState = MutableStateFlow<InitializeState>(InitializeState.Idle)
    val initializeState: StateFlow<InitializeState> = _initializeState.asStateFlow()

    /* 用户状态 */
    lateinit var userStore: UserStore

    /* 应用状态 */
    lateinit var appStateStore: AppStateStore

    /* 设备信息状态 */
    lateinit var deviceInfoStore: DeviceInfoStore

    val applicationScope = lazy {
        CoroutineScope(
            SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { context, throwable ->
                Log.e("ApplicationScope", "发生异常， err=$throwable")
            }
        )
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        // 全局状态
        appStateStore = AppStateStore()
        userStore = UserStore()
        deviceInfoStore = DeviceInfoStore(this)

        initializeApplication()
    }

    private fun initializeApplication() {
        _initializeState.value = InitializeState.Initializing

        applicationScope.value.launch {
            try {
                // 先初始化 appStateStore
                appStateStore.awaitInitialization()
                // 后同时初始化 userStore
                coroutineScope {
                    listOf(
                        async { userStore.awaitInitialization() },
                        async { deviceInfoStore.awaitInitialization() }
                    ).awaitAll()
                }
                // 后同时初始化剩余的内容
                coroutineScope {
                    listOf(
                        async(Dispatchers.IO) {
                            KVStorageManager.initialize(this@DaymarkApplication)
                        },
                        async(Dispatchers.IO) {
                            initDatabase()
                        }
                    ).awaitAll()
                }
                // 修改状态
                _initializeState.value = InitializeState.Success(
                    timestamp = System.currentTimeMillis()
                )
            } catch (throwable: Throwable) {
                // 异常处理
                _initializeState.value = InitializeState.Failed(
                    error = listOf(throwable)
                )
            }
        }
    }

    private suspend fun BaseStore.awaitInitialization() {
        initialize().collect { state ->
            when (state) {
                BaseStore.StoreInitializationState.Initializing -> Unit
                BaseStore.StoreInitializationState.Succeeded -> Unit
                is BaseStore.StoreInitializationState.Failed -> throw state.error
                else -> Unit
            }
        }
    }

    private fun initDatabase() {
        databaseManager = DatabaseManager()
    }


    /**
     * 初始化状态
     */
    sealed interface InitializeState {

        data object Idle : InitializeState

        data object Initializing : InitializeState

        data class Success(val timestamp: Long): InitializeState

        data class Failed(val error: List<Throwable>): InitializeState

    }

}
