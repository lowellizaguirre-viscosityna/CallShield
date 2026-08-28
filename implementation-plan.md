# Android Call Screening Initiative — Implementation Plan

Status: Proposed  
Created: 2026-08-27T20:03:13-04:00  
Target: Android only

## 1. Outcome

Build a privacy-first Android application that lets a user independently enable rules for unwanted calls and choose a supported handling action. The first release will prioritize fast, on-device screening and honest explanations of what Android and the carrier can verify.

Success means:

- the user can grant the app the Android call-screening role;
- each supported rule can be enabled or disabled independently;
- every incoming, screenable call receives a decision within Android's five-second deadline;
- calls can be allowed, rejected/blocked, or silenced according to the matched rule;
- the app records a local, privacy-preserving explanation for each decision;
- behavior is verified on real devices and across multiple carriers/OEMs.

## 2. Feasibility boundaries

These boundaries must be treated as product requirements, not implementation defects:

- Caller-ID spoofing cannot be detected perfectly. Use the carrier-provided STIR/SHAKEN verdict (`callerNumberVerificationStatus`) as a signal, never as proof. A failed verdict may be blocked by policy; passed and unavailable verdicts require separate handling.
- A third-party `CallScreeningService` receives ordinary `tel:` calls but Android does not deliver restricted, unknown, unavailable, or payphone presentations to it. Consequently, the requested private/hidden/unknown filters cannot be guaranteed through the standard screening role. Show these rules as unsupported when the device/API cannot enforce them; do not imply protection that is not active.
- On Android 10+, calls not in contacts are naturally sent to the screening service. `READ_CONTACTS` is required only if the product must inspect calls involving saved contacts. The MVP should avoid that permission unless device testing proves it is necessary for the requested behavior.
- A screening service must respond within five seconds. All decision data must therefore be local and indexed; network reputation checks cannot sit on the critical path.
- “Answer and play fax audio for 30 seconds” is not supported by `CallScreeningService`. Becoming the default dialer exposes answer/disconnect and audio-route controls, but the public Android APIs do not document injection of an audio file into a carrier call's uplink. Treat this as a gated research spike. Do not ship hacks based on speakerphone/microphone feedback, accessibility automation, hidden APIs, or call recording.

## 3. Proposed technical approach

- Kotlin Android app using Jetpack Compose and Material 3.
- Minimum supported version: Android 10 / API 29, because `ROLE_CALL_SCREENING` and silence-call handling are available there. Compile/target SDK should be set to the current stable SDK at implementation time.
- `CallScreeningService` as the call decision entry point.
- A pure Kotlin rule engine, isolated from Android services and covered by table-driven unit tests.
- Room for local rules, explicit block/allow entries, settings, and decision history.
- DataStore for simple preferences and onboarding state.
- Hilt or lightweight constructor injection so the screening path can be tested without a device.
- No analytics or remote call-data upload in the MVP. Any later reputation service requires explicit consent, retention rules, encryption, abuse controls, and a separate privacy review.

Decision priority:

1. explicit allowlist and emergency/system safeguards;
2. explicit user blocklist;
3. failed carrier verification (spoof-risk rule);
4. foreign-number rule;
5. non-contact rule;
6. default allow.

If several rules match, use the highest-priority match and log all matches. Each rule maps to one supported response: reject/block or silence. “Pick up and hang up” and fax playback are not screening-service responses and remain outside the MVP.

## 4. Delivery phases

### Phase 0 — Product and device validation

- Define “foreign” using the user's chosen home country calling code, not device locale alone.
- Define behavior for malformed numbers, withheld presentations, roaming, dual-SIM, Wi-Fi calling, voicemail, emergency callbacks, and verification status `NOT_VERIFIED`.
- Build a minimal proof of concept that requests `ROLE_CALL_SCREENING`, logs sanitized call metadata, and returns allow/reject/silence within five seconds.
- Test on at least one Pixel and two non-Pixel devices, with two carriers if possible.
- Produce a capability matrix for private/unknown calls and OEM/carrier differences.

Exit criterion: a documented, repeatable screening callback and response on supported test devices.

### Phase 1 — Project foundation

- Create the Gradle Android project, package structure, CI, static analysis, unit-test setup, and debug/release build types.
- Register the screening service with `BIND_SCREENING_SERVICE` and implement role onboarding, revocation handling, and a clear inactive-state screen.
- Add the rule domain model, response model, normalized-number abstraction, and decision audit model.
- Add Room/DataStore storage with migrations and repository interfaces.

Exit criterion: CI builds, tests pass, and an installed debug build accurately reports whether it owns the screening role.

### Phase 2 — Rules and supported actions

- Implement toggles for failed-verification/spoof risk, foreign numbers, and non-contact numbers.
- Represent private/hidden/unknown rules in the capability model; expose them only where enforceable and explain platform limitations otherwise.
- Add explicit allowlist/blocklist management and conflict resolution.
- Implement allow, reject/block, and silence response builders.
- Keep all screening decisions local and bounded; add timeout-safe fallback to allow when the engine fails.

Exit criterion: a deterministic rule test matrix covers every rule, combination, priority conflict, and response.

### Phase 3 — User experience and accountability

- Build onboarding for role assignment and narrowly scoped permissions.
- Build settings with independent toggles, response selection, home-country selection, and plain-language limitations.
- Build a local decision history showing timestamp, redacted number, matched rule, verification verdict, response, and reason.
- Add delete-history and data-retention controls.
- Add accessibility, localization-ready strings, dark theme, and safe recovery when the role or permission is revoked.

Exit criterion: a user can understand whether protection is active, configure it, and explain why a test call was handled.

### Phase 4 — Verification and release readiness

- Unit-test number normalization, country classification, priority, and every `CallResponse` mapping.
- Instrument role onboarding, persistence, permission denial, process recreation, and service response timing.
- Execute a real-device matrix for contacts/non-contacts, international formats, dual-SIM, Wi-Fi calling, voicemail, and all carrier-verification verdicts available to testers.
- Run privacy/security review, dependency scanning, accessibility checks, and battery/performance profiling.
- Prepare Play listing, privacy policy, data-safety disclosure, permission declarations, support documentation, and a staged rollout with crash/ANR monitoring.

Exit criterion: no decision exceeds the five-second platform deadline, no unsupported promise appears in UI/store copy, and the staged build meets agreed reliability thresholds.

### Phase 5 — Fax-response research spike (separate go/no-go)

- Prototype only with documented public APIs and only after assessing the cost of becoming a full default dialer (`ROLE_DIALER` plus `InCallService`, dial UI, in-call UI, emergency-call behavior, and call history).
- Verify on physical devices whether any public, policy-compliant API can transmit generated audio to the remote party. Merely playing audio locally is not success.
- Review consent, anti-harassment, telecom law, accessibility, emergency behavior, and Google Play policy before productization.
- If uplink injection is unsupported or inconsistent, close the feature as infeasible and offer only supported reject/silence actions.

Exit criterion: a written go/no-go decision backed by public API documentation and reproducible device tests. This phase must not block the MVP.

## 5. Work breakdown

Suggested modules/components:

- `app`: Compose UI, navigation, onboarding, dependency wiring.
- `screening`: `CallScreeningService`, Android-to-domain mapping, response deadline protection.
- `domain`: rule engine, decisions, priorities, capability model.
- `data`: Room entities/DAOs, DataStore preferences, repositories, retention.
- `telephony`: number normalization, country classification, carrier-verification mapping.
- `testing`: fixtures and fake call details/repositories.

Initial epics:

1. Repository and CI foundation.
2. Screening-role onboarding and service proof of concept.
3. Deterministic local rule engine.
4. Spoof-risk and unwanted-caller rules.
5. Supported call actions.
6. Configuration and decision-history UI.
7. Privacy, accessibility, policy, and release readiness.
8. Default-dialer/fax feasibility spike.

## 6. Acceptance criteria

- With no rule matched, the call is allowed.
- Each toggle changes only its own rule and survives process/device restart.
- A failed STIR/SHAKEN verdict matches the spoof-risk rule; unavailable verification is not mislabeled as spoofed.
- Foreign classification uses parsed E.164 country information plus the user-selected home country; parsing failure follows a documented safe fallback.
- Explicit allow always wins over automatic blocking; explicit block wins over general automatic rules.
- Reject and silence generate the correct Android response flags.
- Every callback responds within five seconds, including storage failure; the safe fallback is allow and locally record an error when possible.
- The UI clearly distinguishes active, inactive, unsupported, and permission-limited rules.
- No raw phone number leaves the device in the MVP.
- Private/hidden/unknown and fax-response limitations are disclosed in product and release documentation.

## 7. Risks and mitigations

- False positives: default to allow, provide allowlists, show reasons, and make automation opt-in per rule.
- Carrier/OEM variance: maintain a physical-device capability matrix and staged rollout.
- Spoof-verdict gaps: model `PASSED`, `FAILED`, and `NOT_VERIFIED` separately; never infer certainty from absence.
- Permission/policy rejection: request the screening role first, minimize permissions, and avoid Call Log permissions unless an approved core use requires them.
- Five-second deadline: local-only decision path, indexed storage, strict time budget, and allow-on-error fallback.
- Fax feature infeasibility: isolate it behind a research gate and keep it out of MVP estimates.
- Sensitive data exposure: local storage, number redaction, configurable retention, no ad SDKs, and explicit privacy review before telemetry.

## 8. Open product decisions

- Should a matched automatic rule reject the call or merely silence it by default?
- Should failed carrier verification be enabled by default, given false-positive risk?
- What home country should be preselected, and may the user configure multiple domestic country codes?
- Is a user-maintained allowlist/blocklist required for MVP?
- How long should local decision history be retained?
- Is Google Play distribution required, or is private/sideloaded distribution acceptable for early testing?

## 9. Source references

- Android `CallScreeningService`: https://developer.android.com/reference/android/telecom/CallScreeningService
- Android call-screening guide: https://developer.android.com/develop/connectivity/telecom/dialer-app/screen-calls
- Android spoofing/STIR-SHAKEN guide: https://developer.android.com/develop/connectivity/telecom/dialer-app/prevent-spoofing
- Android `InCallService`: https://developer.android.com/reference/android/telecom/InCallService
- Google Play Call Log/SMS permission policy: https://support.google.com/googleplay/android-developer/answer/10208820

