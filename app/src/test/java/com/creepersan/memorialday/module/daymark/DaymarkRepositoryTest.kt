package com.creepersan.memorialday.module.daymark

import com.creepersan.memorialday.module.network.host.account.AccountDeviceInfoProvider
import com.creepersan.memorialday.module.network.host.account.AccountSession
import com.creepersan.memorialday.module.network.host.account.AccountTokenStorage
import com.creepersan.memorialday.module.network.host.daymark.AnniversaryCreateRequest
import com.creepersan.memorialday.module.network.host.daymark.AnniversaryData
import com.creepersan.memorialday.module.network.host.daymark.AnniversaryEditRequest
import com.creepersan.memorialday.module.network.host.daymark.AnniversaryIdRequest
import com.creepersan.memorialday.module.network.host.daymark.AnniversaryListData
import com.creepersan.memorialday.module.network.host.daymark.AnniversaryMutationData
import com.creepersan.memorialday.module.network.host.daymark.AnniversaryRefreshRequest
import com.creepersan.memorialday.module.network.host.daymark.AnniversarySortRequest
import com.creepersan.memorialday.module.network.host.daymark.DaymarkApi
import com.creepersan.memorialday.module.network.host.daymark.DaymarkApiProvider
import com.creepersan.memorialday.module.network.host.daymark.DaymarkEnvelope
import com.creepersan.memorialday.module.network.host.daymark.DaymarkError
import com.creepersan.memorialday.module.network.host.daymark.DaymarkRepository
import com.creepersan.memorialday.module.network.host.daymark.DaymarkResult
import com.creepersan.memorialday.module.network.host.daymark.DaymarkServiceConfig
import com.creepersan.memorialday.module.network.host.daymark.DaymarkServiceFactory
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class DaymarkRepositoryTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `configured host receives authenticated list request`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"code":0,"data":{"list":[{"id":7,"name":"恋爱纪念日","description":"","calendar_type":"solar","loop_type":"yearly","year":2020,"month":8,"day":9,"is_leap_month":0,"next_timestamp":1786233600000,"sort":0}]}}"""
            )
        )
        val repository = DaymarkRepository(
            DaymarkServiceFactory(
                config = DaymarkServiceConfig(server.url("/").toString()),
                deviceInfoProvider = TestDeviceInfo,
                tokenStorage = InMemoryTokenStorage(AccountSession(accessToken = "access"))
            )
        )

        val result = repository.listAnniversaries(name = "恋爱", calendarType = "solar", page = 2)

        assertTrue(result is DaymarkResult.Success)
        assertEquals(7L, (result as DaymarkResult.Success).data.single().id)
        val request = server.takeRequest()
        assertEquals("/api/anniversary/list?name=%E6%81%8B%E7%88%B1&calendar_type=solar&page=2&page_size=20", request.path)
        assertEquals("access", request.getHeader("CS-Token"))
        assertEquals("Daymark", request.getHeader("CS-App"))
    }

    @Test
    fun `partial edit preserves omitted fields in request body`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"code":0,"data":{"date":{"id":7,"name":"新名称","description":"原备注","calendar_type":"solar","loop_type":"yearly","year":2020,"month":8,"day":9,"is_leap_month":0,"next_timestamp":1786233600000,"sort":0}}}"""
            )
        )
        val repository = DaymarkRepository(
            DaymarkServiceFactory(
                DaymarkServiceConfig(server.url("/").toString()),
                TestDeviceInfo,
                InMemoryTokenStorage(AccountSession(accessToken = "access"))
            )
        )

        val result = repository.editAnniversary(AnniversaryEditRequest(id = 7, name = "新名称"))

        assertTrue(result is DaymarkResult.Success)
        val request = server.takeRequest()
        assertEquals("/api/anniversary/edit", request.path)
        assertEquals("""{"id":7,"name":"新名称"}""", request.body.readUtf8())
    }

    @Test
    fun `malformed anniversary data is a structured failure`() = runBlocking {
        val repository = DaymarkRepository(
            FakeDaymarkApiProvider(
                FakeDaymarkApi(
                    listResponse = Response.success(
                        DaymarkEnvelope(
                            code = 0,
                            data = AnniversaryListData(list = listOf(AnniversaryData(id = 1)))
                        )
                    )
                )
            )
        )

        val result = repository.listAnniversaries()

        assertTrue(result is DaymarkResult.Failure)
        assertTrue((result as DaymarkResult.Failure).error is DaymarkError.MalformedResponse)
    }

    private object TestDeviceInfo : AccountDeviceInfoProvider {
        override fun appName(): String = "Daymark"
        override fun platform(): String = "Android"
        override fun deviceCode(): String = "device-code"
        override fun deviceName(): String = "Pixel Test"
        override fun versionCode(): String = "30100"
    }

    private class InMemoryTokenStorage(
        private val session: AccountSession?
    ) : AccountTokenStorage {
        override fun current(): AccountSession? = session
        override fun refreshToken(): String? = session?.refreshToken
        override fun accessTokenForRefresh(): String? = session?.accessToken
        override fun update(session: AccountSession) = Unit
        override fun clear() = Unit
    }

    private class FakeDaymarkApiProvider(
        private val api: DaymarkApi
    ) : DaymarkApiProvider {
        override fun create(): DaymarkApi = api
    }

    private class FakeDaymarkApi(
        private val listResponse: Response<DaymarkEnvelope<AnniversaryListData>> = Response.success(
            DaymarkEnvelope(code = 0, data = AnniversaryListData(emptyList()))
        )
    ) : DaymarkApi {
        override suspend fun addAnniversary(
            request: AnniversaryCreateRequest
        ): Response<DaymarkEnvelope<AnniversaryMutationData>> =
            Response.success(DaymarkEnvelope(code = 0, data = AnniversaryMutationData()))

        override suspend fun editAnniversary(
            request: AnniversaryEditRequest
        ): Response<DaymarkEnvelope<AnniversaryMutationData>> =
            Response.success(DaymarkEnvelope(code = 0, data = AnniversaryMutationData()))

        override suspend fun deleteAnniversary(
            request: AnniversaryIdRequest
        ): Response<DaymarkEnvelope<Unit>> = Response.success(
            DaymarkEnvelope(
                code = 0,
                data = Unit
            )
        )

        override suspend fun getAnniversary(
            request: AnniversaryIdRequest
        ): Response<DaymarkEnvelope<AnniversaryData>> =
            Response.success(DaymarkEnvelope(code = 0, data = AnniversaryData()))

        override suspend fun listAnniversaries(
            name: String?,
            calendarType: String?,
            page: Int,
            pageSize: Int
        ): Response<DaymarkEnvelope<AnniversaryListData>> = listResponse

        override suspend fun sortAnniversaries(
            request: AnniversarySortRequest
        ): Response<DaymarkEnvelope<Unit>> = Response.success(
            DaymarkEnvelope(
                code = 0,
                data = Unit
            )
        )

        override suspend fun refreshAnniversaries(
            request: AnniversaryRefreshRequest
        ): Response<DaymarkEnvelope<Unit>> = Response.success(
            DaymarkEnvelope(
                code = 0,
                data = Unit
            )
        )
    }
}
