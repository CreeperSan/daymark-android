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
            type = KVType.DataStore,
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
            KVDefinition(type = KVType.DataStore, module = "test", key = "key", keyDynamic = { "dynamic" }, defaultValue = 1)
        }
        assertThrows(KVDefinitionException::class.java) {
            KVDefinition<Int>(type = KVType.DataStore, module = "test", key = "key")
        }
        assertThrows(KVDefinitionException::class.java) {
            KVDefinition(type = KVType.DataStore, module = "test", keyDynamic = { " " }, defaultValue = 1).resolveKey()
        }
    }
}
