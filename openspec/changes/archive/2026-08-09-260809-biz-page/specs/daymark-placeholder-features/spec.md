## Purpose

为尚未提供服务端接口的 Mark、媒体、分类、提醒和版本能力建立透明的占位边界，使页面和导航可以先交付而不会产生虚假的成功状态或不可逆数据丢失。

## ADDED Requirements

### Requirement: Unsupported server features remain explicit placeholders

When a requested feature has no confirmed Daymark API, the application SHALL render an unavailable or local-only state, SHALL NOT send an invented endpoint request, and SHALL identify the boundary so a later repository adapter can replace it.

#### Scenario: Open Mark editor without a Mark API

- **WHEN** the user opens the Mark editor
- **THEN** the page exposes the designed text, image, video, voice, and mood entry surfaces while save reports unavailable or remains explicitly local-only

#### Scenario: Open reminder management without a reminder API

- **WHEN** the user opens reminder management
- **THEN** the page displays the reminder configuration surface and an explicit unavailable synchronization state without claiming server persistence

### Requirement: Local placeholder actions are reversible

Placeholder edits SHALL either remain in page-local state until the user discards them or use an existing local storage boundary; leaving the page without a confirmed save SHALL not report a remote success result.

#### Scenario: Cancel an unavailable edit

- **WHEN** the user changes placeholder fields and chooses cancel
- **THEN** the changes are discarded and the navigation result is cancellation or unavailable, not success

#### Scenario: Retry after backend support is added

- **WHEN** a repository adapter is later supplied for a placeholder capability
- **THEN** the page contract can delegate persistence to that adapter without changing its typed entry and exit parameters
