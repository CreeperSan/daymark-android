# daymark-business-pages Specification

## Purpose

为 Daymark 提供可从认证流程进入的核心业务页面，覆盖纪念日管理、首页展示、详情查看、设置与时间线入口，并保持设计稿中的页面结构可由组件库复用。

## Requirements

### Requirement: Home page presents the user's anniversary workspace

The application SHALL provide a home page with the warm gradient background, greeting or date context, primary countdown hero, upcoming anniversary list, summary information, and floating navigation actions represented by the design source.

#### Scenario: Open the authenticated home page

- **WHEN** an authenticated user completes login or navigates home
- **THEN** the home page loads anniversary data, displays loading and error states explicitly, and shows available navigation actions

#### Scenario: Refresh home data

- **WHEN** the user requests a refresh
- **THEN** the page invokes anniversary refresh and list loading, preserves the previous content while loading when possible, and exposes an error instead of silently clearing data

### Requirement: Anniversary creation and editing are reachable and stateful

The application SHALL provide new and edit pages that accept typed entry parameters, support name, description, calendar type, date, leap-month flag, loop type, category and reminder fields at the UI boundary, and return a typed completion result after a successful mutation.

#### Scenario: Create an anniversary from home

- **WHEN** the user opens the create page, submits valid required fields, and the server accepts the request
- **THEN** the page closes with a created result and the caller can refresh the home list

#### Scenario: Edit an existing anniversary

- **WHEN** the user opens the edit page with an anniversary ID
- **THEN** the page loads that item, preserves unchanged fields, and returns an updated result only after a successful edit

#### Scenario: Validation failure

- **WHEN** required name or date fields are invalid
- **THEN** the page displays a field-level validation state and does not send a mutation request

### Requirement: Anniversary detail supports common actions

The application SHALL provide a detail page with hero information, countdown/date presentation, description, timeline summary, edit action, share action, and delete confirmation. Missing server-backed timeline content SHALL be represented as an explicit placeholder.

#### Scenario: Open anniversary detail

- **WHEN** the detail page receives an anniversary ID
- **THEN** it loads the corresponding item and presents a not-found or error state when loading fails

#### Scenario: Delete an anniversary

- **WHEN** the user confirms deletion
- **THEN** the delete request is sent once, the page closes with a deleted result after success, and repeated taps do not duplicate the request

### Requirement: Settings and management pages are navigable

The application SHALL provide settings, category management, reminder management, permission explanation, version switching, and category create/edit pages with the sections and actions represented in the design sources. Each page SHALL expose an explicit empty, unavailable, or placeholder state when its server capability is not implemented.

#### Scenario: Navigate through settings

- **WHEN** the user taps a settings row
- **THEN** the corresponding typed destination opens and back navigation returns to settings without losing the parent page state

#### Scenario: Manage categories

- **WHEN** the user opens category management
- **THEN** categories are displayed in a reorderable list, supported local edits are reflected immediately, and unavailable synchronization is labeled as a placeholder

#### Scenario: Open permissions

- **WHEN** the user opens permission explanation and selects a permission action
- **THEN** the page presents the explanation and attempts the appropriate Android settings or permission entry point, reporting unsupported actions rather than crashing

### Requirement: Timeline pages provide a stable placeholder-ready flow

The application SHALL provide timeline and timeline-event pages that accept an anniversary ID and optional event ID, display event type filtering and chronological content, and allow the UI flow to be exercised even when the backend has no timeline endpoints.

#### Scenario: Open an anniversary timeline

- **WHEN** the user opens timeline from detail or edit
- **THEN** the timeline page receives the anniversary ID and displays available events or an explicit unavailable placeholder

#### Scenario: Open a timeline event editor

- **WHEN** the user chooses to add or edit an event
- **THEN** the event editor receives typed parameters and returns a saved, cancelled, or unavailable result without fabricating server data
