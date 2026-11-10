package com.creepersan.memorialday.module.anniversary

import com.google.gson.JsonParser
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** 验证服务端列表分页及闰月创建、编辑协议。 */
class AnniversaryRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: AnniversaryRepository
    @Before fun setUp() { server = MockWebServer().apply { start() }; repository = AnniversaryRepository { server.url("/").toString() } }
    @After fun tearDown() { server.shutdown() }

    @Test fun refreshesAndReadsAllPages() {
        val page = (1..100).joinToString(",") { """{"id":$it,"name":"test","year":2023,"month":2,"day":1,"calendar_type":"lunar","is_leap_month":1,"loop_type":"yearly"}""" }
        server.enqueue(MockResponse().setBody("""{"code":200,"msg":"ok"}"""))
        server.enqueue(MockResponse().setBody("""{"code":200,"data":{"list":[$page]}}"""))
        server.enqueue(MockResponse().setBody("""{"code":200,"data":{"list":[]}}"""))
        val items = repository.list().blockingGet()
        assertEquals(100, items.size)
        assertEquals(1, items.first().isLeapMonth)
        assertEquals("/api/anniversary/refresh", server.takeRequest().path)
        val first = server.takeRequest().requestUrl!!
        val second = server.takeRequest().requestUrl!!
        assertEquals("1", first.queryParameter("page"))
        assertEquals("2", second.queryParameter("page"))
    }

    @Test fun sendsLunarLeapMonthForCreateAndEdit() {
        val response = """{"code":200,"data":{"date":{"id":7,"name":"test","year":2023,"month":2,"day":1,"calendar_type":"lunar","is_leap_month":1,"loop_type":"yearly"}}}"""
        val item = Anniversary(name = "test", calendarType = Anniversary.CALENDAR_LUNAR, loopType = Anniversary.LOOP_YEARLY, year = 2023, month = 2, day = 1, isLeapMonth = 1)
        // 保存和导入共用此接口；首页应直接收到服务端数据，无需再次拉取列表。
        val updates = AnniversaryUpdates.observe().test()
        server.enqueue(MockResponse().setBody(response))
        assertEquals(7L, repository.save(item).blockingGet().id)
        val add = server.takeRequest()
        assertEquals("/api/anniversary/add", add.path)
        val addBody = JsonParser.parseString(add.body.readUtf8()).asJsonObject
        assertEquals(2, addBody.get("month").asInt)
        assertEquals(1, addBody.get("is_leap_month").asInt)
        assertFalse(addBody.has("id"))
        server.enqueue(MockResponse().setBody(response))
        repository.save(item.copy(id = 7)).blockingGet()
        val edit = server.takeRequest()
        assertEquals("/api/anniversary/edit", edit.path)
        assertEquals(7L, JsonParser.parseString(edit.body.readUtf8()).asJsonObject.get("id").asLong)
        updates.assertValueCount(2).assertValueAt(0) { it.id == 7L && it.isLeapMonth == 1 }
        updates.dispose()
    }
    @Test fun deletesOnlyAfterServerConfirms() {
        val updates = AnniversaryUpdates.observeDeletions().test()
        server.enqueue(MockResponse().setBody("""{"code":500,"msg":"failed"}"""))
        assertThrows(IllegalStateException::class.java) { repository.delete(7).blockingGet() }
        updates.assertNoValues()
        server.takeRequest()
        server.enqueue(MockResponse().setBody("""{"code":200,"msg":"ok"}"""))
        repository.delete(7).blockingGet()
        val request = server.takeRequest()
        assertEquals("/api/anniversary/delete", request.path)
        assertEquals(7L, JsonParser.parseString(request.body.readUtf8()).asJsonObject.get("id").asLong)
        updates.assertValue(7L).dispose()
    }

}
