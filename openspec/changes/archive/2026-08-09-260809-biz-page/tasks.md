## 1. Daymark network foundation

- [x] 1.1 Add immutable Daymark service configuration with the default host and test override support.
- [x] 1.2 Define anniversary request/response DTOs and map server field names and timestamps to typed app models.
- [x] 1.3 Implement authenticated Daymark API methods for add, edit, delete, get, list, sort, and refresh.
- [x] 1.4 Implement the Daymark repository with structured transport, authentication, parameter, decoding, and server-error mapping.
- [x] 1.5 Add focused network tests proving host replacement, query/body encoding, partial edits, response mapping, and failure propagation.

## 2. Shared business page infrastructure

- [x] 2.1 Add shared business Compose scaffold for warm backgrounds, safe areas, title bars, loading, empty, error, and unavailable states.
- [x] 2.2 Add business page state holders and lifecycle-aware repository collection for list, detail, create, and edit flows.
- [x] 2.3 Add stable list keys, lazy scrolling, accessibility labels, and explicit retry actions for long-running page content.
- [x] 2.4 Add or update Android manifest registrations and authenticated entry wiring for business destinations.

## 3. Anniversary pages

- [x] 3.1 Implement the home page with countdown hero, upcoming anniversary list, summary content, refresh, and floating navigation.
- [x] 3.2 Implement the new anniversary page with validated name, description, calendar, date, leap-month, and loop-type fields.
- [x] 3.3 Implement the edit anniversary page with typed ID loading, partial mutation support, and created/updated exit results.
- [x] 3.4 Implement the anniversary detail page with hero data, countdown, description, edit/share/delete actions, and timeline placeholder entry.
- [x] 3.5 Implement delete confirmation and one-shot mutation handling so repeated taps cannot duplicate a request.
- [x] 3.6 Preserve a compatibility route to the existing local ViewBinding editor until the new server-backed editor is manually verified.

## 4. Settings and management pages

- [x] 4.1 Implement the settings page sections and typed links to every requested management destination.
- [x] 4.2 Implement category management with reorderable local state and explicit unavailable synchronization status.
- [x] 4.3 Implement new/edit category pages with name, color, icon, and default reminder draft fields.
- [x] 4.4 Implement reminder management with designed controls and an explicit no-server-persistence state.
- [x] 4.5 Implement permission explanation with safe Android permission/settings intents and unsupported-action handling.
- [x] 4.6 Implement old/new version switching UI with reversible local selection and unavailable remote migration state.

## 5. Mark and timeline placeholder flows

- [x] 5.1 Implement Mark create/edit surfaces for text, image, video, voice, and mood drafts without invented network calls.
- [x] 5.2 Implement timeline page filtering, chronological layout, empty state, and explicit unavailable state.
- [x] 5.3 Implement timeline event create/edit page with typed anniversary/event parameters and saved/cancelled/unavailable outcomes.
- [x] 5.4 Ensure placeholder drafts are discarded or labeled local-only when leaving without confirmed persistence.

## 6. Typed navigation

- [x] 6.1 Add typed entry and exit models for all business destinations and centralize Bundle serialization keys.
- [x] 6.2 Extend `CSNavigator` with Observable helpers for home, anniversary, settings, category, reminder, Mark, timeline, permission, and version pages.
- [x] 6.3 Route business actions through the navigation proxy Activity and preserve one-shot completion, cancellation, disposal, and host-destruction behavior.
- [x] 6.4 Add navigator tests for parameter round-trips, result delivery, duplicate close, disposal, and host destruction.

## 7. Integration and verification

- [x] 7.1 Wire login success and authenticated app entry to the new home destination without changing account flows.
- [ ] 7.2 Verify all requested page-to-page actions and back navigation on an Android device.
- [x] 7.3 Run targeted unit/build checks for the network, navigation, and page modules and resolve integration failures.
- [x] 7.4 Document the confirmed Daymark API boundary and placeholder replacement points for future backend implementation.
