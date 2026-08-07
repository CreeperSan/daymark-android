package com.creepersan.memorialday.module.storage.kv

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KVSharedPreferencesOperatorTest {

    private lateinit var operator: KVSharedPreferencesOperator

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        operator = KVSharedPreferencesOperator(context)
        context.getSharedPreferences("kv_kv_test", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun primitiveValuesRoundTrip() {
        assertRoundTrip("string", "value")
        assertRoundTrip("boolean", true)
        assertRoundTrip("char", 'x')
        assertRoundTrip("byte", 7.toByte())
        assertRoundTrip("short", 8.toShort())
        assertRoundTrip("int", 9)
        assertRoundTrip("long", 10L)
        assertRoundTrip("float", 11.5F)
        assertRoundTrip("double", 12.5)
    }

    @Test
    fun missingValueReturnsDefaultAndSerializableValuesRoundTrip() {
        val defaultDefinition = definition("missing", "fallback")
        assertEquals("fallback", operator.get(defaultDefinition))

        val serializableDefinition = definition("serializable", SerializableValue("default"))
        val stored = SerializableValue("stored")
        operator.set(serializableDefinition, stored)
        assertEquals(stored, operator.get(serializableDefinition))
    }

    @Test
    fun unsupportedAndMalformedValuesFailExplicitly() {
        val unsupportedDefinition = definition("unsupported", UnsupportedValue)
        assertThrows(KVUnsupportedTypeException::class.java) {
            operator.get(unsupportedDefinition)
        }

        val malformedDefinition = definition("malformed", 1.toByte())
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("kv_kv_test", Context.MODE_PRIVATE)
            .edit()
            .putString("malformed", "not-a-byte")
            .commit()
        assertThrows(KVConversionException::class.java) {
            operator.get(malformedDefinition)
        }
    }

    @Test
    fun definitionExtensionsUseInitializedOperator() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        KVStorageManager.initialize(context)
        val definition = definition("extension", 0)

        definition.set(42)

        assertEquals(42, definition.get())
    }

    @Test
    fun multipleKvTypesCanBeUsedTogether() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        KVStorageManager.initialize(context)
        val suffix = System.nanoTime()
        val sharedPreferencesDefinition = KVDefinition(
            module = "multi_shared_$suffix",
            key = "value",
            defaultValue = 0,
            type = KVType.SharedPreferences
        )
        val dataStoreDefinition = KVDefinition(
            module = "multi_datastore_$suffix",
            key = "value",
            defaultValue = 0,
            type = KVType.DataStore
        )

        sharedPreferencesDefinition.set(1)
        dataStoreDefinition.set(2)

        assertEquals(1, sharedPreferencesDefinition.get())
        assertEquals(2, dataStoreDefinition.get())
    }

    private fun <T> assertRoundTrip(key: String, value: T) {
        val definition = definition(key, value)
        operator.set(definition, value)
        assertEquals(value, operator.get(definition))
    }

    private fun <T> definition(key: String, defaultValue: T): KVDefinition<T> {
        return KVDefinition(
            module = "kv_test",
            key = key,
            defaultValue = defaultValue
        )
    }

    private data class SerializableValue(
        val value: String,
    ) : KVSerializable<SerializableValue> {
        override fun serialize(): String = value

        override fun deserialize(storeValue: Any): SerializableValue {
            return SerializableValue(storeValue as String)
        }
    }

    private data object UnsupportedValue
}
