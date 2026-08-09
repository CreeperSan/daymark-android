# daymark-anniversary-network Specification

## Purpose

为纪念日业务提供可替换服务地址、稳定的数据契约和统一错误处理，使页面能够安全使用现有 Daymark 后端并在测试环境中切换服务。

## Requirements

### Requirement: Daymark service host is independently configurable

The client SHALL use `https://daymark.creepersan.com/` as the default Daymark service base address and SHALL allow an application or test environment to replace only that service address without changing the account service or other clients.

#### Scenario: Use the production Daymark host by default

- **WHEN** the Daymark client is created without an override
- **THEN** requests use `https://daymark.creepersan.com/`

#### Scenario: Use a test Daymark host

- **WHEN** a test injects a valid replacement base address
- **THEN** all Daymark requests use the replacement and no request is sent to the production host

### Requirement: Anniversary API contract is complete

The client SHALL expose authenticated operations for anniversary add, edit, delete, get, list, sort, and refresh, preserving the server's JSON field names and endpoint semantics. List requests SHALL support name, calendar type, page, and page size; edit requests SHALL preserve omitted nullable fields as unchanged.

#### Scenario: Create an anniversary

- **WHEN** a valid create request contains name, date, calendar type, and loop type
- **THEN** the client sends `POST /api/anniversary/add` and returns the created anniversary data

#### Scenario: List anniversaries

- **WHEN** a caller requests a page with optional name or calendar type filters
- **THEN** the client sends the corresponding query parameters to `POST /api/anniversary/list` and returns the server list

#### Scenario: Partially edit an anniversary

- **WHEN** a caller changes only selected editable fields
- **THEN** the client sends `POST /api/anniversary/edit` with only those fields and returns the updated anniversary

#### Scenario: Sort anniversaries

- **WHEN** a caller submits an ordered list of anniversary IDs
- **THEN** the client sends `POST /api/anniversary/sort` and reports success only after the server acknowledges it

### Requirement: Daymark requests use account authentication and structured failures

Every anniversary request SHALL use the existing Daymark app token authentication path. HTTP, decoding, parameter, authentication, and server failures SHALL be surfaced as structured repository errors; the client MUST NOT convert a failed request into an empty list or successful mutation.

#### Scenario: Expired authentication

- **WHEN** the server rejects a Daymark request because the app token is invalid or expired
- **THEN** the repository returns an authentication failure that callers can handle without treating it as valid data

#### Scenario: Malformed response

- **WHEN** a successful HTTP response cannot be decoded into the expected response envelope
- **THEN** the repository returns a decoding failure containing the operation context
