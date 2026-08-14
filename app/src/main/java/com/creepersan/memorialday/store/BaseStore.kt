package com.creepersan.memorialday.store

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

abstract class BaseStore {

    protected val tag by lazy { this.javaClass.simpleName }


    private val initializeMutex = Mutex()
    private var initialized = false


    /**
     * 返回一个冷流，仅外部 collect 才会执行
     */
    fun initialize(): Flow<StoreInitializationState> = flow {
        emit(StoreInitializationState.Initializing)

        try {
            initializeMutex.withLock {
                if (!initialized) {
                    withContext(Dispatchers.IO) {
                        onInitialize()
                    }
                    initialized = true
                }
            }
            emit(StoreInitializationState.Succeeded)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            emit(StoreInitializationState.Failed(e))
        }
    }

    /**
     * 初始化状态
     */
    sealed interface StoreInitializationState {
        data object Initializing : StoreInitializationState
        data object Succeeded : StoreInitializationState
        data class Failed(val error: Throwable) : StoreInitializationState
    }

    /**
     * 初始化抽象函数
     */
    protected abstract suspend fun onInitialize()
}