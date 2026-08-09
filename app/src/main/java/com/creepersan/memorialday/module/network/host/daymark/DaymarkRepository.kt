package com.creepersan.memorialday.module.network.host.daymark

import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.EOFException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

sealed interface DaymarkResult<out T> {
    data class Success<T>(val data: T) : DaymarkResult<T>
    data class Failure(val error: DaymarkError) : DaymarkResult<Nothing>
}

sealed interface DaymarkError {
    val message: String

    data class Validation(override val message: String) : DaymarkError
    data class DuplicateSubmission(override val message: String = "请求正在处理中") : DaymarkError
    data class Parameter(override val message: String) : DaymarkError
    data class Authentication(override val message: String) : DaymarkError
    data class Server(override val message: String) : DaymarkError
    data class Network(override val message: String) : DaymarkError
    data class MalformedResponse(override val message: String = "服务器响应格式错误") : DaymarkError
    data class Cancelled(override val message: String = "请求已取消") : DaymarkError
}

data class Anniversary(
    val id: Long,
    val name: String,
    val description: String,
    val calendarType: String,
    val loopType: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean,
    val timestamp: Long,
    val nextTimestamp: Long,
    val sort: Int
)

class DaymarkRepository(
    private val serviceFactory: DaymarkApiProvider
) {
    private val submissions = ConcurrentHashMap<Operation, AtomicBoolean>().apply {
        Operation.entries.forEach { put(it, AtomicBoolean(false)) }
    }

    suspend fun listAnniversaries(
        name: String? = null,
        calendarType: String? = null,
        page: Int = 1,
        pageSize: Int = DaymarkApi.DEFAULT_PAGE_SIZE
    ): DaymarkResult<List<Anniversary>> {
        if (page <= 0 || pageSize !in 1..100) {
            return DaymarkResult.Failure(DaymarkError.Validation("分页参数无效"))
        }
        return submit(Operation.LIST) {
            when (val result = decode(serviceFactory.create().listAnniversaries(name, calendarType, page, pageSize))) {
                is DaymarkResult.Failure -> result
                is DaymarkResult.Success -> {
                    val list = result.data.list
                        ?: return@submit DaymarkResult.Failure(DaymarkError.MalformedResponse("服务器响应缺少列表"))
                    list.map(::toAnniversary).fold<Anniversary?, DaymarkResult<List<Anniversary>>>(
                        DaymarkResult.Success(emptyList())
                    ) { accumulator, item ->
                        when (accumulator) {
                            is DaymarkResult.Failure -> accumulator
                            is DaymarkResult.Success -> item?.let {
                                DaymarkResult.Success(accumulator.data + it)
                            } ?: DaymarkResult.Failure(DaymarkError.MalformedResponse("纪念日数据不完整"))
                        }
                    }
                }
            }
        }
    }

    suspend fun getAnniversary(id: Long): DaymarkResult<Anniversary> {
        validateId(id)?.let { return DaymarkResult.Failure(it) }
        return submit(Operation.GET) {
            decode(serviceFactory.create().getAnniversary(AnniversaryIdRequest(id))).map(::toAnniversary)
        }
    }

    suspend fun createAnniversary(request: AnniversaryCreateRequest): DaymarkResult<Anniversary> {
        validateCreate(request)?.let { return DaymarkResult.Failure(it) }
        return submit(Operation.CREATE) {
            decode(serviceFactory.create().addAnniversary(request))
                .map { it.anniversary }
                .map(::toAnniversary)
        }
    }

    suspend fun editAnniversary(request: AnniversaryEditRequest): DaymarkResult<Anniversary> {
        validateId(request.id)?.let { return DaymarkResult.Failure(it) }
        if (request.name != null && request.name.isBlank()) {
            return DaymarkResult.Failure(DaymarkError.Validation("纪念日名称不能为空"))
        }
        return submit(Operation.EDIT) {
            decode(serviceFactory.create().editAnniversary(request))
                .map { it.anniversary }
                .map(::toAnniversary)
        }
    }

    suspend fun deleteAnniversary(id: Long): DaymarkResult<Unit> {
        validateId(id)?.let { return DaymarkResult.Failure(it) }
        return submit(Operation.DELETE) {
            decode(serviceFactory.create().deleteAnniversary(AnniversaryIdRequest(id)), allowEmptyData = true)
        }
    }

    suspend fun sortAnniversaries(ids: List<Long>): DaymarkResult<Unit> {
        if (ids.isEmpty() || ids.any { it <= 0L } || ids.distinct().size != ids.size) {
            return DaymarkResult.Failure(DaymarkError.Validation("纪念日排序数据无效"))
        }
        return submit(Operation.SORT) {
            decode(serviceFactory.create().sortAnniversaries(AnniversarySortRequest(ids)), allowEmptyData = true)
        }
    }

    suspend fun refreshAnniversaries(force: Boolean = false): DaymarkResult<Unit> {
        return submit(Operation.REFRESH) {
            decode(
                serviceFactory.create().refreshAnniversaries(AnniversaryRefreshRequest(if (force) 1 else 0)),
                allowEmptyData = true
            )
        }
    }

    private suspend fun <T> submit(
        operation: Operation,
        request: suspend () -> DaymarkResult<T>
    ): DaymarkResult<T> {
        val pending = submissions.getValue(operation)
        if (!pending.compareAndSet(false, true)) {
            return DaymarkResult.Failure(DaymarkError.DuplicateSubmission())
        }
        return try {
            request()
        } catch (_: CancellationException) {
            DaymarkResult.Failure(DaymarkError.Cancelled())
        } catch (_: JsonParseException) {
            DaymarkResult.Failure(DaymarkError.MalformedResponse())
        } catch (_: EOFException) {
            DaymarkResult.Failure(DaymarkError.MalformedResponse())
        } catch (_: IOException) {
            DaymarkResult.Failure(DaymarkError.Network("网络连接失败，请检查网络后重试"))
        } catch (_: IllegalStateException) {
            DaymarkResult.Failure(DaymarkError.MalformedResponse())
        } finally {
            pending.set(false)
        }
    }

    private fun <T> decode(
        response: Response<DaymarkEnvelope<T>>,
        allowEmptyData: Boolean = false
    ): DaymarkResult<T> {
        if (!response.isSuccessful) {
            return DaymarkResult.Failure(errorFromHttp(response.code(), response.message()))
        }
        val envelope = response.body()
            ?: return DaymarkResult.Failure(DaymarkError.MalformedResponse())
        if (envelope.code != 0 && envelope.code !in 200..299) {
            return DaymarkResult.Failure(errorFromCode(envelope.code, envelope.message))
        }
        val data = envelope.data
        if (data == null && !allowEmptyData) {
            return DaymarkResult.Failure(DaymarkError.MalformedResponse("服务器响应缺少数据"))
        }
        @Suppress("UNCHECKED_CAST")
        return DaymarkResult.Success(data as T)
    }

    private fun <T, R : Any> DaymarkResult<T>.map(transform: (T) -> R?): DaymarkResult<R> {
        return when (this) {
            is DaymarkResult.Failure -> DaymarkResult.Failure(error)
            is DaymarkResult.Success -> transform(data)?.let { DaymarkResult.Success(it) }
                ?: DaymarkResult.Failure(DaymarkError.MalformedResponse("服务器响应数据不完整"))
        }
    }

    private fun toAnniversary(data: AnniversaryData): Anniversary? {
        val id = data.id ?: return null
        val name = data.name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val calendarType = data.calendarType ?: return null
        val loopType = data.loopType ?: return null
        val year = data.year ?: return null
        val month = data.month ?: return null
        val day = data.day ?: return null
        return Anniversary(
            id = id,
            name = name,
            description = data.description.orEmpty(),
            calendarType = calendarType,
            loopType = loopType,
            year = year,
            month = month,
            day = day,
            isLeapMonth = data.isLeapMonth == 1,
            timestamp = data.timestamp ?: 0L,
            nextTimestamp = data.nextTimestamp ?: 0L,
            sort = data.sort ?: 0
        )
    }

    private fun validateCreate(request: AnniversaryCreateRequest): DaymarkError.Validation? {
        return when {
            request.name.isBlank() -> DaymarkError.Validation("纪念日名称不能为空")
            request.name.length > MAX_NAME_LENGTH -> DaymarkError.Validation("纪念日名称不能超过${MAX_NAME_LENGTH}个字符")
            request.calendarType !in CALENDAR_TYPES -> DaymarkError.Validation("不支持的日历类型")
            request.loopType !in LOOP_TYPES -> DaymarkError.Validation("不支持的循环类型")
            request.month !in 1..12 || request.day !in 1..31 -> DaymarkError.Validation("日期无效")
            else -> null
        }
    }

    private fun validateId(id: Long): DaymarkError.Validation? =
        if (id <= 0L) DaymarkError.Validation("纪念日不存在") else null

    private fun errorFromHttp(code: Int, message: String): DaymarkError = when (code) {
        400, 404, 409, 422 -> DaymarkError.Parameter(message.ifBlank { "请求参数错误" })
        401, 403 -> DaymarkError.Authentication(message.ifBlank { "登录已失效，请重新登录" })
        in 500..599 -> DaymarkError.Server(message.ifBlank { "服务器暂时不可用" })
        else -> DaymarkError.Network(message.ifBlank { "请求失败" })
    }

    private fun errorFromCode(code: Int?, message: String?): DaymarkError {
        val resolvedMessage = message?.takeIf { it.isNotBlank() } ?: "请求失败"
        return when (code) {
            400, 404, 409, 422, 1001, 1002 -> DaymarkError.Parameter(resolvedMessage)
            401, 403, 1003, 1004 -> DaymarkError.Authentication(resolvedMessage)
            null -> DaymarkError.MalformedResponse()
            else -> DaymarkError.Server(resolvedMessage)
        }
    }

    private enum class Operation { LIST, GET, CREATE, EDIT, DELETE, SORT, REFRESH }

    private companion object {
        const val MAX_NAME_LENGTH = 64
        val CALENDAR_TYPES = setOf("solar", "lunar")
        val LOOP_TYPES = setOf("none", "yearly", "monthly")
    }
}
