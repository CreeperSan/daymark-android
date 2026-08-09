package com.creepersan.memorialday.module.network.host.account

import com.creepersan.memorialday.module.storage.kv.KVDefinition
import com.creepersan.memorialday.module.storage.kv.KVType
import com.creepersan.memorialday.module.storage.kv.get
import com.creepersan.memorialday.module.storage.kv.set

data class AccountSession(
    val accessToken: String,
    val refreshToken: String? = null,
    val account: String? = null,
    val email: String? = null,
    val userId: Long? = null,
    val expiresAtEpochMillis: Long = 0L
) {
    fun isExpired(nowEpochMillis: Long): Boolean {
        return expiresAtEpochMillis > 0L && expiresAtEpochMillis <= nowEpochMillis
    }
}

interface AccountTokenStorage {
    fun current(): AccountSession?
    fun refreshToken(): String?
    fun accessTokenForRefresh(): String?
    fun update(session: AccountSession)
    fun clear()
}

/**
 * Persistent account session storage built on the project's KVDefinition layer.
 */
class KVAccountTokenStorage(
    private val nowEpochMillis: () -> Long = System::currentTimeMillis
) : AccountTokenStorage {

    @Synchronized
    override fun current(): AccountSession? {
        val accessToken = ACCESS_TOKEN.get()
        if (accessToken.isBlank()) {
            return null
        }
        val session = AccountSession(
            accessToken = accessToken,
            refreshToken = REFRESH_TOKEN.get().ifBlank { null },
            account = ACCOUNT.get().ifBlank { null },
            email = EMAIL.get().ifBlank { null },
            userId = USER_ID.get().takeIf { it > 0L },
            expiresAtEpochMillis = EXPIRES_AT.get()
        )
        if (session.isExpired(nowEpochMillis())) {
            return null
        }
        return session
    }

    @Synchronized
    override fun refreshToken(): String? = REFRESH_TOKEN.get().ifBlank { null }

    @Synchronized
    override fun accessTokenForRefresh(): String? = ACCESS_TOKEN.get().ifBlank { null }

    @Synchronized
    override fun update(session: AccountSession) {
        require(session.accessToken.isNotBlank()) { "Access token must not be blank" }
        ACCESS_TOKEN.set(session.accessToken)
        REFRESH_TOKEN.set(session.refreshToken.orEmpty())
        ACCOUNT.set(session.account.orEmpty())
        EMAIL.set(session.email.orEmpty())
        USER_ID.set(session.userId ?: 0L)
        EXPIRES_AT.set(session.expiresAtEpochMillis)
    }

    @Synchronized
    override fun clear() {
        ACCESS_TOKEN.set("")
        REFRESH_TOKEN.set("")
        ACCOUNT.set("")
        EMAIL.set("")
        USER_ID.set(0L)
        EXPIRES_AT.set(0L)
    }

    private companion object {
        const val MODULE = "account_auth"

        val ACCESS_TOKEN = KVDefinition(MODULE, key = "access_token", defaultValue = "", type = KVType.DataStore)
        val REFRESH_TOKEN = KVDefinition(MODULE, key = "refresh_token", defaultValue = "", type = KVType.DataStore)
        val ACCOUNT = KVDefinition(MODULE, key = "account", defaultValue = "", type = KVType.DataStore)
        val EMAIL = KVDefinition(MODULE, key = "email", defaultValue = "", type = KVType.DataStore)
        val USER_ID = KVDefinition(MODULE, key = "user_id", defaultValue = 0L, type = KVType.DataStore)
        val EXPIRES_AT = KVDefinition(MODULE, key = "expires_at", defaultValue = 0L, type = KVType.DataStore)
    }
}
