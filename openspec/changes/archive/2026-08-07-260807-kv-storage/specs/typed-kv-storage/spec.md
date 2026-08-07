## Purpose

Provides a typed, definition-based key-value API so application code can read and write supported values without depending on a concrete Android storage backend.

## ADDED Requirements

### Requirement: Definitions SHALL resolve valid storage identity and defaults

Each definition MUST provide a non-blank module name, exactly one key source, and exactly one default-value source. Static and dynamic sources MUST be mutually exclusive, and dynamic sources MUST be evaluated when the operation needs the value. A resolved key MUST be non-blank.

#### Scenario: Static definition is accepted

- **WHEN** a definition supplies a non-blank module, a static key, and a static default
- **THEN** the definition can be used for read and write operations

#### Scenario: Dynamic definition is evaluated per operation

- **WHEN** a definition supplies a key function or default-value function
- **THEN** the function is evaluated at operation time and its resolved result is used

#### Scenario: Invalid definition is rejected

- **WHEN** a definition omits a required source, supplies both static and dynamic sources, or resolves a blank key
- **THEN** the operation fails with an explicit argument or definition error

### Requirement: Supported values SHALL round-trip through the KV API

The system MUST persist and retrieve String, Boolean, Char, Byte, Short, Int, Long, Float, and Double values without changing their logical value. It MUST also persist and retrieve values implementing `KVSerializable`, using the value's serialized representation.

#### Scenario: Primitive value is absent

- **WHEN** a supported primitive definition is read before a value has been stored
- **THEN** the resolved default value is returned

#### Scenario: Primitive value is written and read

- **WHEN** a supported primitive value is written and then read with the same definition
- **THEN** the read operation returns the stored value with the original logical type

#### Scenario: Serializable value is written and read

- **WHEN** a `KVSerializable` value is written and then read with the same definition
- **THEN** the value is serialized for storage and reconstructed through its deserialization contract

### Requirement: Unsupported values SHALL fail explicitly

The system MUST reject values whose type is not one of the supported primitive types or `KVSerializable`. It MUST NOT silently stringify, discard, or substitute an unsupported value.

#### Scenario: Unsupported value is written

- **WHEN** a caller writes an unsupported object type
- **THEN** the operation throws an explicit unsupported-type exception

#### Scenario: Stored value cannot be converted

- **WHEN** a stored value is incompatible with the requested definition type
- **THEN** the read operation throws an explicit conversion or storage error rather than returning a fabricated default

### Requirement: Definition extension syntax SHALL delegate to the initialized KV access

Callers MUST be able to invoke `get()` on a definition and `set(value)` on a definition, and both operations MUST use the same initialized storage behavior as the underlying KV abstraction.

#### Scenario: Definition is read with extension syntax

- **WHEN** a caller invokes `definition.get()`
- **THEN** the operation returns the same result as the initialized KV operator

#### Scenario: Definition is written with extension syntax

- **WHEN** a caller invokes `definition.set(value)`
- **THEN** the value is persisted through the initialized KV operator

### Requirement: Multiple KV backends SHALL coexist without per-backend configuration

The system MUST register all built-in `KVType` backends from a single application initialization call. Each definition MUST select the backend it uses, and operations on one backend MUST NOT replace or redirect operations on another backend.

#### Scenario: Multiple backend types are initialized

- **WHEN** the application calls `KVStorageManager.initialize(context)`
- **THEN** definitions assigned to either backend can be read and written in the same process

#### Scenario: Definition selects its backend

- **WHEN** two definitions use the same key but select different backend types
- **THEN** each definition reads and writes only its selected backend's value
