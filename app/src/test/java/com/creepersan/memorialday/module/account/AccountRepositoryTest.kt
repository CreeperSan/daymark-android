package com.creepersan.memorialday.module.account

import com.creepersan.memorialday.module.network.host.account.AccountApi
import com.creepersan.memorialday.module.network.host.account.AccountApiProvider
import com.creepersan.memorialday.module.network.host.account.AccountAuthCodeData
import com.creepersan.memorialday.module.network.host.account.AccountAvailabilityData
import com.creepersan.memorialday.module.network.host.account.AccountDeviceInfoProvider
import com.creepersan.memorialday.module.network.host.account.AccountEnvelope
import com.creepersan.memorialday.module.network.host.account.AccountError
import com.creepersan.memorialday.module.network.host.account.AccountLoginData
import com.creepersan.memorialday.module.network.host.account.AccountRegistrationData
import com.creepersan.memorialday.module.network.host.account.AccountRepository
import com.creepersan.memorialday.module.network.host.account.AccountResult
import com.creepersan.memorialday.module.network.host.account.AccountServiceConfig
import com.creepersan.memorialday.module.network.host.account.AccountServiceFactory
import com.creepersan.memorialday.module.network.host.account.AccountSession
import com.creepersan.memorialday.module.network.host.account.AccountTokenData
import com.creepersan.memorialday.module.network.host.account.AccountTokenStorage
import com.creepersan.memorialday.module.network.host.account.CheckUserExistsRequest
import com.creepersan.memorialday.module.network.host.account.LoginRequest
import com.creepersan.memorialday.module.network.host.account.RefreshTokenRequest
import com.creepersan.memorialday.module.network.host.account.RegisterRequest
import com.creepersan.memorialday.module.network.host.account.RequestAuthCodeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AccountRepositoryTest {
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
    fun `login sends required headers and payload then persists the session`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"code":200,"data":{"user":{"id":42,"account":"alice","email":"alice@example.com"},"token":{"token":"access","refresh_token":"refresh","token_expire_time":3600}}}"""
            )
        )
        val storage = InMemoryTokenStorage()
        val repository = AccountRepository(
            AccountServiceFactory(
                config = AccountServiceConfig(server.url("/").toString()),
                deviceInfoProvider = TestDeviceInfo,
                tokenStorage = storage
            ),
            storage,
            nowEpochMillis = { 1_000L }
        )

        val result = repository.login("alice", "secret")

        assertTrue(result is AccountResult.Success)
        val session = (result as AccountResult.Success).data
        assertEquals("access", session.accessToken)
        assertEquals("refresh", storage.current()?.refreshToken)
        val request = server.takeRequest()
        assertEquals("/api/login", request.path)
        assertEquals("Daymark", request.getHeader("CS-App"))
        assertEquals("Android", request.getHeader("CS-Device-Platform"))
        assertEquals("test-android-id", request.getHeader("CS-Device-Code"))
        assertEquals("Pixel Test", request.getHeader("CS-Device-Name"))
        assertEquals("30100", request.getHeader("CS-Version"))
        assertEquals(null, request.getHeader("CS-Token"))
        assertEquals(
            """{"account":"alice","email":"","password":"secret"}""",
            request.body.readUtf8()
        )
    }

    @Test
    fun `refresh uses the persisted token header and refresh token payload`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"code":0,"data":{"access_token":"new-access","refresh_token":"new-refresh"}}"""
            )
        )
        val storage = InMemoryTokenStorage(
            AccountSession(accessToken = "old-access", refreshToken = "old-refresh")
        )
        val repository = AccountRepository(
            AccountServiceFactory(
                AccountServiceConfig(server.url("/").toString()),
                TestDeviceInfo,
                storage
            ),
            storage
        )

        val result = repository.refreshToken()

        assertTrue(result is AccountResult.Success)
        val request = server.takeRequest()
        assertEquals("/api/token/refresh", request.path)
        assertEquals("old-access", request.getHeader("CS-Token"))
        assertEquals("""{"refresh_token":"old-refresh"}""", request.body.readUtf8())
        assertEquals("new-access", storage.current()?.accessToken)
    }

    @Test
    fun `server parameter and malformed errors map to safe domain errors`() = runBlocking {
        val storage = InMemoryTokenStorage()
        val parameterApi = FakeAccountApi(
            loginResponse = Response.success(AccountEnvelope(code = 400, message = "邮箱格式错误"))
        )
        val parameterResult = AccountRepository(FakeAccountApiProvider(parameterApi), storage)
            .login("alice", "secret")
        assertEquals(
            AccountError.Parameter("邮箱格式错误"),
            (parameterResult as AccountResult.Failure).error
        )

        val malformedApi = FakeAccountApi(
            loginFailure = com.google.gson.JsonSyntaxException("bad JSON")
        )
        val malformedResult = AccountRepository(FakeAccountApiProvider(malformedApi), storage)
            .login("alice", "secret")
        assertTrue((malformedResult as AccountResult.Failure).error is AccountError.MalformedResponse)
    }

    @Test
    fun `cancellation is exposed without persisting credentials`() = runBlocking {
        val storage = InMemoryTokenStorage()
        val result = AccountRepository(
            FakeAccountApiProvider(FakeAccountApi(loginFailure = CancellationException())),
            storage
        ).login("alice", "secret")

        assertTrue((result as AccountResult.Failure).error is AccountError.Cancelled)
        assertFalse(storage.current() != null)
    }

    private object TestDeviceInfo : AccountDeviceInfoProvider {
        override fun appName(): String = "Daymark"
        override fun platform(): String = "Android"
        override fun deviceCode(): String = "test-android-id"
        override fun deviceName(): String = "Pixel Test"
        override fun versionCode(): String = "30100"
    }

    private class InMemoryTokenStorage(
        private var session: AccountSession? = null
    ) : AccountTokenStorage {
        override fun current(): AccountSession? = session
        override fun refreshToken(): String? = session?.refreshToken
        override fun accessTokenForRefresh(): String? = session?.accessToken
        override fun update(session: AccountSession) {
            this.session = session
        }
        override fun clear() {
            session = null
        }
    }

    private class FakeAccountApi(
        private val loginResponse: Response<AccountEnvelope<AccountLoginData>> = Response.success(
            AccountEnvelope(
                code = 0,
                data = AccountLoginData(token = AccountTokenData(accessToken = "token"))
            )
        ),
        private val loginFailure: Throwable? = null
    ) : AccountApi {
        override suspend fun login(request: LoginRequest): Response<AccountEnvelope<AccountLoginData>> {
            loginFailure?.let { throw it }
            return loginResponse
        }

        override suspend fun checkUserExists(
            request: CheckUserExistsRequest
        ): Response<AccountEnvelope<AccountAvailabilityData>> = Response.success(
            AccountEnvelope(code = 0, data = AccountAvailabilityData(exists = false))
        )

        override suspend fun requestAuthCode(
            request: RequestAuthCodeRequest
        ): Response<AccountEnvelope<AccountAuthCodeData>> = Response.success(
            AccountEnvelope(code = 0, data = AccountAuthCodeData())
        )

        override suspend fun register(
            request: RegisterRequest
        ): Response<AccountEnvelope<AccountRegistrationData>> = Response.success(
            AccountEnvelope(code = 0, data = AccountRegistrationData(account = request.account))
        )

        override suspend fun refreshToken(
            request: RefreshTokenRequest
        ): Response<AccountEnvelope<AccountTokenData>> = Response.success(
            AccountEnvelope(code = 0, data = AccountTokenData(accessToken = "token"))
        )

        override suspend fun renewToken(): Response<AccountEnvelope<AccountTokenData>> =
            Response.success(
                AccountEnvelope(
                    code = 0,
                    data = AccountTokenData(accessToken = "token")
                )
            )
    }

    private class FakeAccountApiProvider(
        private val api: AccountApi
    ) : AccountApiProvider {
        override fun create(): AccountApi = api
    }
}
