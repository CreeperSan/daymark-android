## Context

The app already has a Retrofit-based account module, a replaceable account host, a centralized `CSNavigator`, and a legacy ViewBinding anniversary editor. The requested business surfaces are broader than the confirmed server API: the Daymark backend currently registers only anniversary add, edit, delete, list, get, sort, and refresh endpoints. The design sources are static HTML references in `docs/design/app-pages.html` and `docs/design/new-pages.html`; the existing Compose component library is the preferred UI foundation.

## Goals / Non-Goals

**Goals:**

- Add a coherent business-page flow that can be entered after authentication and navigated entirely through typed `CSNavigator` APIs.
- Keep Daymark networking independently configurable and aligned with the confirmed endpoint contracts.
- Make pages usable with real anniversary data while keeping unsupported features explicit and replaceable.
- Reuse the existing `CS` components and preserve Android lifecycle-safe RxJava 3 navigation.

**Non-Goals:**

- Invent or call backend endpoints that are not present in the Daymark server.
- Complete server persistence for Mark media, timeline events, categories, reminders, or version switching in this change.
- Replace the account authentication protocol or redesign the existing component library.

## Decisions

### Separate Daymark service configuration from account configuration

Add an immutable Daymark service configuration with the default `https://daymark.creepersan.com/`, injected into a Daymark service/repository factory. Tests can provide a local or mock base URL. The account configuration remains independent so changing one host cannot redirect the other service.

The client will use the existing authentication/token header path and map the server response envelope into typed DTOs. The repository boundary will own transport, decoding, and server-error mapping; Compose screens will consume state rather than Retrofit responses.

### Use a layered Daymark repository for confirmed and placeholder capabilities

The confirmed anniversary repository exposes add, edit, delete, get, list, sort, and refresh. Its DTOs preserve server names such as `calendar_type`, `loop_type`, `next_timestamp`, and `is_leap_month`, with explicit conversion to UI models.

Unsupported capabilities receive interfaces or local placeholder implementations that return an explicit unavailable outcome. This keeps page contracts stable while preventing accidental fake persistence. When a real API is added, a repository implementation can replace the placeholder without changing navigator entry or exit models.

### Add business destinations to the existing navigation bridge

Extend the current proxy-activity and `Observable` bridge rather than introducing a second navigation system. Each destination receives a typed entry model through the existing Bundle transport and returns a typed exit model with completed, cancelled, failed, or unavailable outcomes. The navigator remains the only caller-facing route construction API.

Use one destination Activity per page family where it improves lifecycle isolation, but share Compose screen content and state holders. The legacy anniversary editor remains callable during migration; the new route contract can point to the new page while preserving the existing database-backed flow until the server-backed implementation is wired.

### Build pages around a shared Compose scaffold

Business pages use a shared scaffold for warm gradient backgrounds, safe-area handling, title bars, loading/error/empty states, and floating navigation. Page-specific content is composed from the existing `CS` components. Long content uses lazy containers with stable keys; destructive actions use custom modal content instead of platform-default styling.

The page groups are:

- Home and detail: server-backed anniversary list/get/refresh, countdown presentation, actions, and explicit timeline placeholder.
- Create/edit: server-backed anniversary fields with local-only category/reminder controls until their APIs exist.
- Settings and management: settings sections, category/reorder UI, reminders, permissions, version switch, and category editor.
- Mark and timeline: design-complete entry surfaces with local draft state and unavailable persistence outcome.

### Keep missing API behavior visible and reversible

A placeholder page may collect a local draft, but it must not emit a successful remote-save result. Leaving without confirmation discards or clearly labels the draft. Unsupported actions show an unavailable state and do not silently fall back to empty successful data.

### Preserve responsive and testable page boundaries

Page state holders expose loading, content, empty, error, and unavailable states. Repository calls are cancellable with the page lifecycle. Navigation tests verify parameter round-trips and one-shot completion; network tests use injected base URLs and mock responses rather than production hosts.

## Risks / Trade-offs

- [Risk] The existing editor is ViewBinding/local-database based while the new flow is Compose/server-backed → Mitigation: retain the old implementation as a compatibility path and isolate the new repository/page contract behind `CSNavigator`.
- [Risk] Backend fields or response envelopes may evolve → Mitigation: centralize DTO mapping and return structured decoding errors instead of silently defaulting values.
- [Risk] Placeholder pages may be mistaken for persisted features → Mitigation: use explicit unavailable labels/outcomes and never report remote success without a repository acknowledgement.
- [Risk] Many page routes increase manifest and navigation surface area → Mitigation: centralize route constants, typed models, and registration checks in navigator tests.

## Migration Plan

1. Add Daymark host configuration, API contracts, repository, and focused network tests.
2. Add shared business page state/scaffold and the confirmed anniversary pages.
3. Add placeholder-ready settings, category, reminder, Mark, timeline, permission, version, and category editor pages.
4. Extend `CSNavigator`, register destinations, and wire the authenticated entry to the home page.
5. Keep the legacy editor route available until manual device verification confirms the new route.

Rollback consists of removing the authenticated home entry and selecting the legacy editor route; the independent Daymark module can remain unused without changing account behavior.
