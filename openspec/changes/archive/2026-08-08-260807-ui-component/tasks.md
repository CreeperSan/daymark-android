## 1. Foundation and token system

- [x] 1.1 Create the `widget/composable/cs` package structure and shared `CS` variant, semantic color, category color, and state types.
- [x] 1.2 Implement `CSColors`, `CSTypography`, `CSSpacing`, `CSShapes`, and `CSMotion` from the design HTML tokens, including light/dark and reduced-motion defaults.
- [x] 1.3 Implement `CSTheme` and integrate it with `MemorialDayTheme` without changing existing non-showcase screen behavior.
- [x] 1.4 Add shared accessibility, minimum touch-target, clamping, and slot/content helper utilities with unit-testable boundaries.

## 2. Text and input components

- [x] 2.1 Replace the unfinished text placeholders with `CSTextDisplay`, `CSTextTitle`, `CSTextSectionTitle`, `CSTextCardTitle`, `CSTextBody`, `CSTextBodySmall`, `CSTextHint`, and `CSTextNumber`.
- [x] 2.2 Implement `CSButton` variants for primary, secondary, ghost, text, danger, small, icon, loading, disabled, full-width, and custom slot content.
- [x] 2.3 Implement `CSInput` with label, placeholder, helper/error, search/password affordances, keyboard options, read-only, disabled, and leading/trailing slots.
- [x] 2.4 Implement `CSOtpInput` with configurable length, focus movement, paste, delete, masking, error, disabled, keyboard semantics, and state-hoisted value handling.

## 3. Selection and content components

- [x] 3.1 Implement `CSSwitch` with animated and reduced-motion states, 44dp semantics, custom colors, and thumb content.
- [x] 3.2 Implement `CSRadioButton` and `CSCheckbox` with selected/unselected/disabled semantics, labels, custom content, and callback contracts.
- [x] 3.3 Implement `CSCard` featured and category variants with icon/content slots, safe long text, click handling, and category token mapping.
- [x] 3.4 Implement `CSList` with stable keys, item renderer, header/footer, empty content, optional click handling, and large/empty collection safety.
- [x] 3.5 Implement `CSProgressBar` linear/circular determinate and indeterminate variants with clamped progress and accessible descriptions.
- [x] 3.6 Implement `CSDivider` solid, dashed, and labeled variants using soft-border tokens.
- [x] 3.7 Implement `CSNotification` with semantic types, icon/title/body slots, close behavior, action content, and accessible dismissal.

## 4. Navigation and feedback components

- [x] 4.1 Implement `CSNavigationBar` standard, immersive, and scrolled modes with navigation/action slots and scroll transition handling.
- [x] 4.2 Implement `CSPillNavigation` floating translucent navigation with selected state, center action, stable item keys, and minimum touch targets.
- [x] 4.3 Implement `CSToast` state/host APIs for success, warning, error, and info messages with duration, action, dismissal, max width, and lifecycle-safe timers.
- [x] 4.4 Implement `CSModal` center dialog and bottom-sheet variants with scrim, dismissal policy, focus behavior, and confirm/dismiss/content slots.
- [x] 4.5 Implement `CSLoading` spinner, dots, and text variants with static reduced-motion fallback and no unnecessary background work.
- [x] 4.6 Implement `CSSkeleton` warm static/shimmer variants and card helper slots with lifecycle-safe animation.

## 5. Empty, error, and carousel components

- [x] 5.1 Implement `CSEmptyState` with illustration/title/description/action/content slots and a usable no-data layout.
- [x] 5.2 Implement `CSErrorState` with error illustration/title/description/retry action/content slots and accessible error semantics.
- [x] 5.3 Implement `CSCarousel` with swipe gestures, indicators, clamped state, stable keys, optional auto-play, reduced-motion behavior, and lifecycle cancellation.

## 6. Standalone showcase

- [x] 6.1 Create `CSComponentShowcaseActivity` based on `BaseComposeActivity`, register it in the manifest, and keep it independent of business services.
- [x] 6.2 Build grouped showcase sections for all 20 categories using local sample data and state-hoisted interactive examples.
- [x] 6.3 Demonstrate default, disabled, selected, error, loading, empty, custom-slot, modal/toast, and carousel states where applicable.
- [x] 6.4 Add previews or screenshot-testable composables for representative token and component states without changing the application launch route.

## 7. Documentation and verification

- [x] 7.1 Create `docs/components/compose-component-library.md` with the complete component inventory, parameter matrix, design tokens, scenarios, accessibility rules, and usage snippets.
- [x] 7.2 Cross-check every documented public name against the implementation and showcase, ensuring every public component entry starts with `CS`.
- [x] 7.3 Add focused Compose tests for progress/index clamping, state callbacks, disabled behavior, minimum semantics, empty content, and reduced-motion behavior.
- [x] 7.4 Run formatting/build/unit tests and manually verify the standalone showcase on the target Android configuration.

## 8. Atomic component refactor

- [x] 8.1 Flatten the `cs` package into `widget/composable`, split aggregate files into one file per component family, and add shared no-ripple interaction support.
- [x] 8.2 Correct typography and rebuild the dedicated button, input, OTP, switch, selection, card, list, navigation, progress, and divider APIs and visuals.
- [x] 8.3 Rebuild feedback components: root toast policies and animations, custom modal surfaces, correct spinner/dots loading, shared skeleton shimmer, and empty/error spacing.
- [x] 8.4 Replace carousel scrolling with snapping pager behavior and update the standalone showcase to use all flattened APIs and corrected states.
- [x] 8.5 Update tests and the canonical component documentation, run unit/instrumentation builds, and validate the revised showcase on a physical device.
