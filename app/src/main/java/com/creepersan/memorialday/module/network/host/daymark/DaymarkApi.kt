package com.creepersan.memorialday.module.network.host.daymark

import com.creepersan.memorialday.module.network.api.NetworkService
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface DaymarkApi : NetworkService {

    @POST(ADD_ANNIVERSARY_PATH)
    suspend fun addAnniversary(@Body request: AnniversaryCreateRequest): Response<DaymarkEnvelope<AnniversaryMutationData>>

    @POST(EDIT_ANNIVERSARY_PATH)
    suspend fun editAnniversary(@Body request: AnniversaryEditRequest): Response<DaymarkEnvelope<AnniversaryMutationData>>

    @POST(DELETE_ANNIVERSARY_PATH)
    suspend fun deleteAnniversary(@Body request: AnniversaryIdRequest): Response<DaymarkEnvelope<Unit>>

    @POST(GET_ANNIVERSARY_PATH)
    suspend fun getAnniversary(@Body request: AnniversaryIdRequest): Response<DaymarkEnvelope<AnniversaryData>>

    @POST(LIST_ANNIVERSARY_PATH)
    suspend fun listAnniversaries(
        @Query("name") name: String? = null,
        @Query("calendar_type") calendarType: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = DEFAULT_PAGE_SIZE
    ): Response<DaymarkEnvelope<AnniversaryListData>>

    @POST(SORT_ANNIVERSARY_PATH)
    suspend fun sortAnniversaries(@Body request: AnniversarySortRequest): Response<DaymarkEnvelope<Unit>>

    @POST(REFRESH_ANNIVERSARY_PATH)
    suspend fun refreshAnniversaries(@Body request: AnniversaryRefreshRequest): Response<DaymarkEnvelope<Unit>>

    companion object {
        const val ADD_ANNIVERSARY_PATH = "api/anniversary/add"
        const val EDIT_ANNIVERSARY_PATH = "api/anniversary/edit"
        const val DELETE_ANNIVERSARY_PATH = "api/anniversary/delete"
        const val GET_ANNIVERSARY_PATH = "api/anniversary/get"
        const val LIST_ANNIVERSARY_PATH = "api/anniversary/list"
        const val SORT_ANNIVERSARY_PATH = "api/anniversary/sort"
        const val REFRESH_ANNIVERSARY_PATH = "api/anniversary/refresh"
        const val DEFAULT_PAGE_SIZE = 20
    }
}

data class DaymarkEnvelope<T>(
    val code: Int? = null,
    @SerializedName(value = "message", alternate = ["msg", "error", "detail"])
    val message: String? = null,
    val data: T? = null
)

data class AnniversaryCreateRequest(
    val name: String,
    val description: String = "",
    @SerializedName("calendar_type")
    val calendarType: String,
    val year: Int,
    val month: Int,
    val day: Int,
    @SerializedName("is_leap_month")
    val isLeapMonth: Int = 0,
    @SerializedName("loop_type")
    val loopType: String
)

data class AnniversaryEditRequest(
    val id: Long,
    val name: String? = null,
    val description: String? = null,
    @SerializedName("calendar_type")
    val calendarType: String? = null,
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    @SerializedName("is_leap_month")
    val isLeapMonth: Int? = null,
    @SerializedName("loop_type")
    val loopType: String? = null
)

data class AnniversaryIdRequest(val id: Long)

data class AnniversarySortRequest(val ids: List<Long>)

data class AnniversaryRefreshRequest(val force: Int = 0)

data class AnniversaryMutationData(
    @SerializedName(value = "date", alternate = ["anniversary"])
    val anniversary: AnniversaryData? = null
)

data class AnniversaryListData(
    val list: List<AnniversaryData>? = null
)

data class AnniversaryData(
    val id: Long? = null,
    @SerializedName("user_id")
    val userId: Long? = null,
    val name: String? = null,
    val description: String? = null,
    @SerializedName("calendar_type")
    val calendarType: String? = null,
    @SerializedName("loop_type")
    val loopType: String? = null,
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    @SerializedName("is_leap_month")
    val isLeapMonth: Int? = null,
    val timestamp: Long? = null,
    @SerializedName("next_timestamp")
    val nextTimestamp: Long? = null,
    val sort: Int? = null,
    @SerializedName("create_time")
    val createTime: Long? = null,
    @SerializedName("update_time")
    val updateTime: Long? = null
)
