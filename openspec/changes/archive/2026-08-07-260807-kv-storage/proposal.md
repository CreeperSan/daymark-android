## Why

The existing KV abstraction defines the intended API surface but does not yet persist values, resolve dynamic definitions, or provide working extension syntax. Implementing it now gives business code a single typed definition-based API instead of coupling callers to storage backends, while making unsupported value types fail explicitly.

## What Changes

- Implement storage-backed `KVDefinition<T>` resolution for module, key, and default values.
- Complete `KVBaseOperator` behavior and provide concrete operators that can be registered together.
- Support Boolean, numeric primitives, String, and `KVSerializable` values; reject unsupported types with an explicit exception.
- Implement `get()` and `set()` extension functions so callers can use a `KVDefinition` directly.
- Add the required DataStore dependency and let each definition select its backend without exposing backend details to business callers.
- Document the design, supported types, failure behavior, and usage examples in the project memory documentation.

## Capabilities

### New Capabilities

- `typed-kv-storage`: Definition-based typed key-value access with defaults, serialization support, extension syntax, and explicit unsupported-type failures.

### Modified Capabilities

<!-- No existing OpenSpec capability has requirements for this KV abstraction. -->

## Impact

- `app/src/main/java/com/creepersan/memorialday/module/storage/kv/`
- App Gradle dependencies and storage initialization/configuration.
- New or updated unit tests for definition validation, primitive values, serializable values, defaults, and unsupported types.
- Project memory documentation covering the KV design and usage.
