package com.creepersan.memorialday.module.anniversary

import com.creepersan.memorialday.constance.KV_APPLICATION_DEV_MODE
import com.creepersan.memorialday.constance.KV_APPLICATION_PROXY_SERVER
import com.creepersan.memorialday.module.kv.KV
import com.creepersan.memorialday.module.network.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.reactivex.rxjava3.core.Single

/** 访问纪念日服务；请求头和令牌刷新由网络层处理。 */
class AnniversaryRepository(private val hostProvider: () -> String = ::defaultHost) {
    companion object {
        private const val HOST = "https://daymark.creepersan.com"
        private const val LIST_PATH = "/api/anniversary/list"
        private const val REFRESH_PATH = "/api/anniversary/refresh"
        private const val GET_PATH = "/api/anniversary/get"
        private const val ADD_PATH = "/api/anniversary/add"
        private const val EDIT_PATH = "/api/anniversary/edit"
        private const val DELETE_PATH = "/api/anniversary/delete"
        private const val PAGE_SIZE = 100

        /** 开发者模式启用时允许覆盖纪念日服务地址。 */
        private fun defaultHost(): String = if (KV.get(KV_APPLICATION_DEV_MODE)) KV.get(KV_APPLICATION_PROXY_SERVER).ifBlank { HOST } else HOST
    }

    private val gson = Gson()

    /** 先推进服务端循环日期，再按页取回全部纪念日。 */
    fun list(): Single<List<Anniversary>> = post(REFRESH_PATH, mapOf("force" to 0)).flatMap { listPage(1, emptyList()) }

    private fun listPage(page: Int, previous: List<Anniversary>): Single<List<Anniversary>> {
        return post(LIST_PATH, emptyMap(), mapOf("page" to page, "page_size" to PAGE_SIZE)).flatMap { data ->
            val array = data.getAsJsonArray("list") ?: throw IllegalStateException("Invalid list response")
            val items = array.map { gson.fromJson(it, Anniversary::class.java) }
            val all = previous + items
            if (items.size < PAGE_SIZE) Single.just(all) else listPage(page + 1, all)
        }
    }

    /** 获取最新详情，编辑时避免使用过期的列表数据。 */
    fun get(id: Long): Single<Anniversary> = post(GET_PATH, mapOf("id" to id)).map { gson.fromJson(it, Anniversary::class.java) }

    /** 创建或更新纪念日，日期字段均为用户所选日历的原始值。 */
    fun save(item: Anniversary): Single<Anniversary> {
        val body = mutableMapOf<String, Any>("name" to item.name, "description" to item.description, "calendar_type" to item.calendarType, "loop_type" to item.loopType, "year" to item.year, "month" to item.month, "day" to item.day, "is_leap_month" to item.isLeapMonth)
        if (item.id > 0) body["id"] = item.id
        return post(if (item.id > 0) EDIT_PATH else ADD_PATH, body).map { data ->
            val result = data.getAsJsonObject("date") ?: throw IllegalStateException("Invalid save response")
            gson.fromJson(result, Anniversary::class.java)
        }.doOnSuccess { AnniversaryUpdates.publish(it) }
    }

    /** 删除成功后通知首页移除对应记录。 */
    fun delete(id: Long): Single<JsonObject> = post(DELETE_PATH, mapOf("id" to id)).doOnSuccess { AnniversaryUpdates.publishDeletion(id) }

    private fun post(path: String, body: Map<String, Any>, query: Map<String, Any>? = null): Single<JsonObject> {
        val host = hostProvider()
        return NetworkManager.request(NetworkRequest(host, path, NetworkRequestMethod.Post, query = query, data = NetworkRequestBody.Json(body), headers = DevelopmentHost.headers(host, HOST))).firstOrError().map { response ->
            if (response.responseCode !in 200..299) throw IllegalStateException("HTTP ${response.responseCode}")
            val result = JsonParser.parseString(response.bodyString()).asJsonObject
            if (result.get("code")?.asInt != 200) throw IllegalStateException(result.get("msg")?.asString ?: "Request failed")
            result.getAsJsonObject("data") ?: JsonObject()
        }
    }
}
