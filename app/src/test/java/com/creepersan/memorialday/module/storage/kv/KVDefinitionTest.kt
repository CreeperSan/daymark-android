package com.creepersan.memorialday.module.storage.kv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class KVDefinitionTest {

    @Test
    fun `dynamic key and default are resolved when requested`() {
        var key = "first"
        var defaultValue = 1
        val definition = KVDefinition(
            module = "test",
            keyDynamic = { key },
            defaultValueDynamic = { defaultValue }
        )

        assertEquals("first", definition.resolveKey())
        assertEquals(1, definition.resolveDefault())

        key = "second"
        defaultValue = 2
        assertEquals("second", definition.resolveKey())
        assertEquals(2, definition.resolveDefault())
    }

    @Test
    fun `definition rejects invalid sources`() {
        assertThrows(KVDefinitionException::class.java) {
            KVDefinition(module = "test", key = "key", keyDynamic = { "dynamic" }, defaultValue = 1)
        }
        assertThrows(KVDefinitionException::class.java) {
            KVDefinition<Int>(module = "test", key = "key")
        }
        assertThrows(KVDefinitionException::class.java) {
            KVDefinition(module = "test", keyDynamic = { " " }, defaultValue = 1).resolveKey()
        }
    }
}
