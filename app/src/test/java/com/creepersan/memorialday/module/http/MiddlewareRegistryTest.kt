package com.creepersan.memorialday.module.http

import com.creepersan.memorialday.module.http.middleware.MiddlewareRegistry
import com.creepersan.memorialday.module.http.middleware.NetworkMiddleware
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 全局中间件注册表测试。
 */
class MiddlewareRegistryTest {
    /**
     * 验证注册顺序和快照隔离。
     */
    @Test
    fun snapshotKeepsRegistrationOrder() {
        val registry = MiddlewareRegistry()
        val first = NetworkMiddleware { _, _, _ -> error("测试中间件不应被执行") }
        val second = NetworkMiddleware { _, _, _ -> error("测试中间件不应被执行") }

        registry.register(first)
        val snapshot = registry.snapshot()
        registry.register(second)

        assertEquals(listOf(first), snapshot)
        assertEquals(listOf(first, second), registry.snapshot())
    }
}
