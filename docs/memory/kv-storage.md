# Typed KV Storage

## Overview

The app exposes a definition-based synchronous KV API. Business code defines a `KVDefinition<T>` once and uses the `get()` and `set(value)` extensions without accessing `SharedPreferences` directly.

Implementation evidence:

- `app/src/main/java/com/creepersan/memorialday/module/storage/kv/KVDefinition.kt`
- `app/src/main/java/com/creepersan/memorialday/module/storage/kv/KVDefinitionExtensions.kt`
- `app/src/main/java/com/creepersan/memorialday/module/storage/kv/KVStorageManager.kt`
- `app/src/main/java/com/creepersan/memorialday/application/MemorialDayApplication.kt`

## Definition Rules

A definition requires:

- A non-blank `module`.
- Exactly one of `key` or `keyDynamic`.
- Exactly one of `defaultValue` or `defaultValueDynamic`.
- An optional `type`, defaulting to `KVType.SharedPreferences`.

Dynamic key and default providers are evaluated when the operation needs them. A blank dynamic key or null resolved default throws `KVDefinitionException`.

```kotlin
val lastLogin = KVDefinition(
    module = "account",
    key = "last_login",
    defaultValue = ""
)

lastLogin.set("2026-08-07")
val value = lastLogin.get()
```

## Supported Types

The SharedPreferences backend supports:

- `String`
- `Boolean`
- `Char`
- `Byte`
- `Short`
- `Int`
- `Long`
- `Float`
- `Double`
- Values implementing `KVSerializable<T>`

`Char`, `Byte`, `Short`, and `Double` are stored as strings and parsed strictly when read. Unsupported types throw `KVUnsupportedTypeException`; malformed stored values throw `KVConversionException`.

## Serializable Values

`KVSerializable<T>` provides `serialize()` for writes and `deserialize(storeValue)` for reads. The definition's default serializable instance supplies the deserializer:

```kotlin
data class UserSettings(val theme: String) : KVSerializable<UserSettings> {
    override fun serialize(): String = theme

    override fun deserialize(storeValue: Any): UserSettings {
        return UserSettings(storeValue as String)
    }
}

val settings = KVDefinition(
    module = "account",
    key = "settings",
    defaultValue = UserSettings("system")
)
```

Keep serialized values small. This abstraction is intended for simple application KV data, not large documents or collections.

## Backend and Initialization

`MemorialDayApplication.onCreate()` initializes `KVStorageManager` with all registered `KVType` backends. Each definition selects its backend independently:

```kotlin
val sharedValue = KVDefinition(
    module = "account",
    key = "shared_value",
    defaultValue = 0,
    type = KVType.SharedPreferences
)

val dataStoreValue = KVDefinition(
    module = "account",
    key = "data_store_value",
    defaultValue = 0,
    type = KVType.DataStore
)
```

The storage files are backend-specific and module-scoped. SharedPreferences uses `kv_<module>` and DataStore uses `kv_<module>`.

`KVStorageManager.initialize(context)` registers every built-in `KVType`:

```kotlin
KVStorageManager.initialize(context)
```

`MemorialDayApplication.onCreate()` is the only application initialization point. Business code does not need to call `initialize()` or configure individual backends. Built-in backends are registered automatically; `KVDefinition.type` only selects the backend used by that definition.

DataStore is internally bridged to the synchronous definition API. Use small values and avoid frequent main-thread calls because DataStore access waits for persistence.

## Evidence

- `app/src/main/java/com/creepersan/memorialday/module/storage/kv/KVSharedPreferencesOperator.kt`
- `app/src/main/java/com/creepersan/memorialday/module/storage/kv/KVDataStoreOperator.kt`
- `app/src/main/java/com/creepersan/memorialday/module/storage/kv/KVSerializable.kt`
