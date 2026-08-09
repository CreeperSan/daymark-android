## 1. Network and service configuration

- [x] 1.1 Add an independently replaceable account service base URL with the production default `https://account.creepersan.com/` and test/staging override support.
- [x] 1.2 Define account API DTOs and Retrofit service methods for login, account/email existence checks, registration auth-code requests, registration, token refresh, and token renewal using the documented relative paths.
- [x] 1.3 Implement account response decoding and error mapping for success data, parameter errors, authentication failures, server failures, malformed responses, and cancellation.
- [x] 1.4 Add account repository/service orchestration that validates inputs, prevents duplicate submissions, persists successful user/token data, and exposes refresh/renew operations.
- [x] 1.5 Add or extend token storage using the existing project storage layer, including clear/update behavior and safe handling of expired or missing tokens.
- [x] 1.6 Add account API tests with a replaceable MockWebServer base URL covering request payloads, response mapping, error messages, and cancellation.

## 2. Typed reactive navigation

- [ ] 2.1 Define reusable typed page-enter/page-exit models and explicit completed, cancelled, and failed navigation outcomes.
- [ ] 2.2 Implement `CSNavigator` Activity Result registration, launch, result decoding, and one-shot close handling without retaining destroyed Activities.
- [ ] 2.3 Add RxJava 3 `Observable` adapters that launch on subscription, emit at most one result, complete after delivery, and unregister on disposal or host destruction.
- [ ] 2.4 Add navigation helpers for login, registration, email verification, password recovery, and existing add/edit page flows without changing unrelated routes.
- [ ] 2.5 Add unit tests for typed parameter transfer, one-shot result delivery, disposal, repeated close calls, cancellation, and destroyed hosts.

## 3. Authentication page implementation

- [ ] 3.1 Implement shared authentication page scaffolding for the design gradient background, logo, headings, footer links, scrolling, safe-area insets, and accessibility semantics.
- [ ] 3.2 Implement `LoginActivity` with controlled account/email and password fields, password visibility, validation, loading/error states, login submission, forgot-password navigation, registration navigation, and disabled social placeholders.
- [ ] 3.3 Implement `RegisterActivity` with nickname-to-account mapping, email/password fields, password visibility, terms checkbox, availability checks, registration-code request, countdown, and registration result handling.
- [ ] 3.4 Add the email verification page/activity using `CSOtpInput` with six-character validation, resend countdown, confirm/cancel results, and replacement-email navigation.
- [ ] 3.5 Implement `ForgetPasswordActivity` according to the design稿 with email validation, return-to-login navigation, and an explicit unavailable state until a password-reset API is supplied.
- [ ] 3.6 Wire authentication Activity results and success navigation into the manifest and existing launch flow, preserving current non-auth behavior.
- [ ] 3.7 Add UI tests for visual states and interactions: empty/invalid fields, password visibility, terms gating, loading, server errors, OTP entry/resend, navigation links, and repeated taps.

## 4. Design fidelity and verification

- [ ] 4.1 Cross-check authentication dimensions and colors against `docs/design/auth-pages.html`, using existing `CS` components wherever their contracts match.
- [ ] 4.2 Verify accessibility roles, labels, error descriptions, minimum touch targets, keyboard behavior, and focus traversal on all authentication controls.
- [ ] 4.3 Run existing unit, Compose, and instrumentation checks for the changed module and fix regressions without altering unrelated screens.
- [ ] 4.4 Install the Debug build, launch the authentication entry on a physical device, and verify login/register/OTP/navigation behavior with the replaceable account Host configuration.
