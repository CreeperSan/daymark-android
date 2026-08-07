## Context

The KV package contains the public shape of a definition, operator interface, SharedPreferences and DataStore backends, a serializable contract, and definition extensions. The app exposes a synchronous `get(): T` / `set(value)` API, while DataStore is asynchronous. The backend manager therefore needs to register multiple operators while preserving the existing synchronous caller API.

## Goals / Non-Goals

**Goals:**

- Preserve the existing package and definition/operator abstractions.
- Make the direct definition API safe for static and dynamic keys/defaults.
- Persist all specified primitive types and `KVSerializable` values.
- Keep unsupported types and malformed stored values explicit and diagnosable.
- Document the public usage pattern in project memory.

**Non-Goals:**

- Introducing a reactive or suspend-based storage API.
- Adding migration of existing application keys.
- Supporting arbitrary collections, maps, Parcelable values, or reflection-based object serialization.
- Exposing a suspend or Flow-based API.

## Decisions

### Register one operator per KV type

`KVStorageManager.initialize(context)` registers every built-in `KVType` and is called from `MemorialDayApplication.onCreate()`. It does not expose per-type or custom operator configuration. `KVDefinition` carries a `type` field, defaulting to SharedPreferences, and extensions resolve the operator by that type. This prevents one backend initialization from overwriting another while keeping business usage configuration-free.

Each backend owns its own module-scoped storage namespace. Tests should use the concrete backend with an isolated module namespace.

### Bridge DataStore to the existing synchronous API

DataStore uses `runBlocking(Dispatchers.IO)` internally for the existing synchronous `get()` and `set()` contract. This keeps the public API stable while allowing both backends to coexist. The memory documentation warns callers to keep values small and avoid frequent main-thread DataStore access.

### Validate definition sources at construction and resolution time

The definition constructor will reject invalid static configuration immediately. Dynamic key and default providers will be resolved per operation, then validated so runtime changes cannot create an empty storage key or an unusable default silently.

### Store native primitives natively and non-native primitives as text

Boolean, Int, Long, Float, and String will use their corresponding SharedPreferences methods. Char, Byte, Short, and Double will use a canonical string representation with strict parsing on read. This preserves the synchronous API and avoids lossy numeric coercion.

### Use the default value as the deserialization strategy

For a `KVSerializable` definition, the resolved default value supplies the `deserialize` implementation. Writes call `serialize()` on the value and store the resulting String. Reads pass the stored String back to the default instance's `deserialize` method. A serializable definition without a usable deserializer instance will fail explicitly instead of guessing a constructor or using reflection.

### Fail with dedicated runtime errors

Invalid definitions, unsupported value types, and conversion failures will use clear exception types/messages so callers and tests can distinguish configuration errors from unsupported values and corrupt storage.

### Keep business usage backend-agnostic

The documented usage will define reusable `KVDefinition` constants or providers and call `definition.get()` / `definition.set(value)`. Module and key details remain inside definitions; business code will not access `SharedPreferences` directly.

## Risks / Trade-offs

- [Risk] SharedPreferences writes are synchronous and can block if abused with large values. → Keep values small, use `apply()` for writes, and document that this abstraction is for small KV data rather than large payloads.
- [Risk] Dynamic defaults can change between reads. → Resolve them only when needed and document that defaults are fallback values, not persisted state.
- [Risk] `KVSerializable` deserialization depends on a default instance. → Validate that contract and throw a descriptive error when it is unavailable.
- [Risk] Blocking a caller while DataStore completes I/O can contribute to UI stalls. → Keep the API for small values, document the cost, and introduce a suspend API before using DataStore for frequent UI-path reads.

## Migration Plan

1. Implement and test the definition/operator behavior without changing existing business call sites.
2. Add the required application context initialization and serialization/backend wiring.
3. Add project memory documentation and usage examples.
4. Roll back by removing the new KV wiring/dependency and restoring the placeholder package if validation exposes an integration issue; no data migration is required because this change introduces no existing-key migration.
