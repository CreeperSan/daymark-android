## Context

See `proposal.md` for the motivation and capability boundaries. The repository is a single Android application using Kotlin 2.2, Jetpack Compose Material 3, Compose BOM, Java 17, and a partially scaffolded `widget/composable` package. The design source is `docs/design/design-spec.html` plus the 20-category inventory in `docs/design/component-library.html`; implementation must fit the existing `BaseComposeActivity` and must not introduce a second UI framework.

## Goals / Non-Goals

**Goals:**

- Expose a coherent, `CS`-prefixed Compose API for all 20 design categories.
- Centralize Daymark tokens and semantic states so components can be themed without editing each component.
- Prefer state-hoisted APIs, slot-based content, stable keys, defensive bounds handling, and accessible semantics.
- Make the showcase independently runnable and useful as a visual contract.
- Keep documentation close to the source tree and aligned with public APIs.

**Non-Goals:**

- Rewriting existing XML business screens or migrating the whole application to Compose.
- Adding network, database, image-loading, navigation, or dependency-injection infrastructure.
- Creating a new icon dependency; icons may use Material/Lucide-compatible vector assets already available in the project.
- Implementing a production notification service or a cross-process Toast manager.

## Decisions

### 1. Package and public API shape

Place the library under `com.creepersan.memorialday.widget.composable.cs`, grouped by concern:

- `token/`: `CSColors`, `CSTypography`, `CSSpacing`, `CSShapes`, `CSMotion`, `CSTheme`.
- `text/`: `CSTextDisplay`, `CSTextTitle`, `CSTextSectionTitle`, `CSTextCardTitle`, `CSTextBody`, `CSTextBodySmall`, `CSTextHint`, `CSTextNumber`.
- `input/`: `CSInput`, `CSOtpInput`, `CSSwitch`, `CSRadioButton`, `CSCheckbox`.
- `content/`: `CSButton`, `CSCard`, `CSList`, `CSProgressBar`, `CSDivider`, `CSNotification`, `CSCarousel`.
- `feedback/`: `CSToast`, `CSModal`, `CSLoading`, `CSSkeleton`, `CSEmptyState`, `CSErrorState`.
- `navigation/`: `CSNavigationBar`, `CSPillNavigation`.

Every public composable and public style/value type introduced for this library uses the `CS` prefix. Standard Compose types such as `Modifier`, `Color`, `TextStyle`, `Dp`, `State`, and slot lambdas remain unchanged. This keeps imports discoverable and prevents the unfinished `TextTitle`/`TextHint` names from becoming competing public APIs.

### 2. Token mapping

Map the design HTML tokens to Kotlin constants in one theme surface:

| Area | Defaults |
| --- | --- |
| Primary | Terracotta `#D9622E`, with 50..900 supporting shades |
| Secondary | Sage `#4A7A51`, with 50..900 supporting shades |
| Neutral | Warm gray `#FAF6F1` page background and `#2A241D` primary text |
| Semantic | Success `#4A7A51`, warning `#8B5E14`, danger `#B5432A`, info `#3B6B85` |
| Categories | Rose `#E8675A`, amber `#E8A838`, teal `#3B8B8E`, lavender `#8B76B5`, each with soft background |
| Type | 48/36/30/24/20/18/16/15/14/13/12sp hierarchy with documented weights/line heights |
| Spacing | 4, 8, 12, 16, 20, 24, 32, then larger section values as needed |
| Shape | 0, 4, 6, 10, 16, 20, 28, and full |
| Motion | Standard 250ms, emphasized 400ms, exit 150ms; all motion can be disabled |

`CSTheme` provides light/dark color schemes, typography and token locals. Dynamic colors remain opt-in rather than replacing the design defaults, so the showcase and production screens have deterministic visuals.

### 3. Parameter and extension matrix

The following parameters are the minimum public contract; each component also accepts `modifier` and content descriptions where relevant.

| Component | Required behavior | Extension parameters |
| --- | --- | --- |
| Text variants | Render the matching type scale and wrap safely | `text`, `modifier`, `color`, `style`, `maxLines`, `overflow`, `softWrap` |
| `CSButton` | Primary/secondary/ghost/text/danger, icon, loading, disabled | `onClick`, `style`, `enabled`, `loading`, `leadingIcon`, `trailingIcon`, `content`, `modifier`, `minHeight` |
| `CSInput` | Filled input, search/password affordances, helper/error | `value`, `onValueChange`, `label`, `placeholder`, `supportingText`, `isError`, `readOnly`, `enabled`, `leadingContent`, `trailingContent`, `keyboardOptions`, `visualTransformation` |
| `CSOtpInput` | Six-slot default with paste/delete/focus behavior | `value`, `onValueChange`, `length`, `enabled`, `isError`, `mask`, `keyboardOptions`, `slotContent`, `modifier` |
| `CSSwitch` | Animated on/off with 44dp semantics | `checked`, `onCheckedChange`, `enabled`, `colors`, `thumbContent`, `modifier` |
| `CSRadioButton`/`CSCheckbox` | Selected/unselected/disabled semantics | `selected`/`checked`, callback, `enabled`, `label`, `content`, `colors`, `modifier` |
| `CSCard` | Featured and category card variants | `variant`, `category`, `onClick`, `title`, `subtitle`, `value`, `icon`, `content`, `modifier`, `elevation` |
| `CSList` | Stable keyed list with empty/custom item support | `items`, `key`, `itemContent`, `header`, `footer`, `emptyContent`, `onItemClick`, `modifier` |
| `CSNavigationBar` | Standard, immersive and scrolled modes | `title`, `mode`, `navigationIcon`, `actions`, `scrollFraction`, `contentColor`, `modifier`, `onNavigationClick` |
| `CSProgressBar` | Linear/circular determinate and indeterminate | `progress`, `variant`, `size`, `showLabel`, `colors`, `contentDescription`, `modifier` |
| `CSDivider` | Solid, dashed, or labeled divider | `variant`, `label`, `thickness`, `color`, `modifier` |
| `CSToast` | Success/warning/error/info, max width and timed dismissal | `message`, `type`, `icon`, `duration`, `onDismiss`, `action`, `modifier`; host state is hoisted |
| `CSModal` | Center dialog and bottom sheet with scrim | `visible`, `onDismissRequest`, `variant`, `title`, `description`, `icon`, `confirmButton`, `dismissButton`, `content`, `dismissOnBackPress`, `dismissOnClickOutside` |
| `CSLoading` | Spinner, dots, or text-loading variants | `variant`, `size`, `label`, `color`, `reducedMotion`, `modifier` |
| `CSNotification` | Icon, title/body, optional close/action | `title`, `message`, `type`, `icon`, `onClick`, `onDismiss`, `action`, `modifier` |
| `CSPillNavigation` | Floating translucent navigation with center action | `items`, `selectedIndex`, `onItemSelected`, `centerAction`, `contentDescription`, `modifier` |
| `CSSkeleton` | Warm shimmer or static placeholder | `shape`, `color`, `highlightColor`, `animated`, `modifier`; card helper accepts slots |
| `CSEmptyState` | Illustration/title/body/action | `title`, `description`, `illustration`, `action`, `content`, `modifier` |
| `CSErrorState` | Error illustration/title/body/retry action | `title`, `description`, `illustration`, `retryAction`, `content`, `modifier` |
| `CSCarousel` | Swipe, indicators, optional auto-play | `items`, `key`, `itemContent`, `currentIndex`, `onIndexChange`, `autoPlay`, `autoPlayInterval`, `showIndicators`, `reducedMotion`, `modifier` |

Use sealed or enum-like `CS` value types for variants and semantic colors where a finite set is useful, while allowing custom colors and slots for extension. Keep mutable state outside components unless a convenience overload is explicitly documented.

### 4. State, robustness, and accessibility

- Clamp progress to `0f..1f`; normalize empty item collections and clamp carousel indices to valid bounds.
- Preserve caller state as the source of truth for text, OTP, switch, selection, modal visibility and carousel index.
- Use `rememberSaveable` only for showcase-local state or convenience state holders, never to hide production state from the caller.
- Use stable keys for list and carousel items, avoid `notify`-style mutable list ownership, and cancel auto-play/animated effects when the composable leaves composition.
- Add `semantics` for labels, roles, selected/checked/disabled states, progress descriptions, error messages and dismiss actions. Enforce 44dp minimum hit targets even when visuals are compact.
- Use `LocalReducedMotion`/a `CSMotion` setting to disable non-essential transitions and auto-play. Shimmer and spinners remain static but informative in reduced-motion mode.
- Avoid broad exception handling and avoid starting work for invisible or empty components.

### 5. Theme integration and showcase

Add `CSTheme` as a wrapper around Material 3 `MaterialTheme` and keep `MemorialDayTheme` as the application-facing entry point, delegating token values to the CS theme. Add a standalone `CSComponentShowcaseActivity` based on `BaseComposeActivity`, register it in the manifest, and render a vertically scrollable screen with grouped sections matching the HTML inventory. Use local sample state for every interactive example and include a small API label beside each group.

This is preferred over embedding the showcase in `IntroActivity`: it keeps the acceptance surface isolated, does not alter the app launch flow, and can later be wired to a developer-only route without changing the component contracts.

### 6. Documentation location

Create `docs/components/compose-component-library.md` as the canonical human/AI guide. It will contain the component inventory, parameter matrix, token table, state/accessibility rules, usage snippets, scenario guidance, and a checklist that maps each component to the showcase. Keep the source design HTML references linked rather than copying large HTML sections.

## Risks / Trade-offs

- [Risk] The existing project uses Material 3 defaults and the HTML uses custom warm tokens, so partial theme migration could cause visual drift. → Keep `CSTheme` token ownership explicit and make the showcase use deterministic non-dynamic colors.
- [Risk] Twenty components in one change can produce inconsistent APIs. → Apply the parameter matrix and shared `CS` style/value types before implementing component bodies; review all public signatures together.
- [Risk] Auto-play, shimmer, and modal effects can leak or continue after navigation. → Scope effects with composition lifecycle, cancel jobs on exit, and add reduced-motion handling.
- [Risk] Local design values do not define every Android accessibility detail. → Preserve platform semantics, minimum touch targets, and explicit content descriptions even where the visual spec is silent.
- [Risk] A single application module is not a distributable library module. → Keep package boundaries and documentation library-like now; defer Gradle module extraction until there is a second consumer.

## Migration Plan

1. Flatten `widget/composable/cs/**` into `widget/composable/**`, retaining only package-level `token` support files. Every public component will have one file named after its exported primary component, such as `CSButton.kt`, `CSInput.kt`, and `CSToast.kt.
2. Replace variant-driven UI APIs with dedicated component entry points where the visual contract differs. In particular, button variants become `CSButton`, `CSButtonSecondary`, `CSButtonText`, and `CSButtonLoading`; cards become a single styled container with caller-owned content.
3. Replace Material default visual components where they conflict with the supplied specification: inputs, OTP, switches, navigation bars, progress, modals, loading indicators, and carousel paging use custom Compose layouts and drawing primitives.
4. Centralize no-ripple interaction in a shared modifier and use it for every library click target. Visual state feedback remains through color, scale, and semantics rather than platform ripple.
5. Rebuild the showcase against the flattened API and validate it on a physical Android device. Add/update focused tests for input, OTP, toast queue policy, paging, and reduced motion.

Rollback is file-level: restore the previous component package and showcase entry. Existing XML screens and unrelated Compose screens remain unaffected.

### 7. Atomic component layout and behavior corrections

The library uses `widget/composable/` directly, rather than a `cs` directory or concern-grouped aggregate files. Shared token-only files remain under `widget/composable/token/`; all component implementation files map one-to-one with a component family.

`CSTextTitle` uses a compact navigation/title scale and `CSTextNumber` is renamed to `CSTextEmphasis`. `CSNavigationBar` owns explicit left, center, and right slots; a background model supports solid, transparent, gradient, and blur styles with configurable height.

`CSInput` is a custom outlined field. `CSOtpInput` owns one hidden editable field and renders read-only cells, so exactly one cursor/focus target exists while paste and deletion work consistently. `CSSwitch`, radio/checkbox controls, progress indicators, modal surfaces, and loaders use custom drawing/layout rather than Material default visuals.

`CSToastHost` is placed at a root overlay. Its state supports queue and replace policies, enter/exit animation, loading toasts, optional outside cancellation, and fully custom toast content. Skeleton cards receive one shared shimmer brush across the whole content area. `CSCarousel` uses a snapping pager so partial resting positions are impossible.
