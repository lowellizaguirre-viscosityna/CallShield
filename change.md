# Change Log

## 2026-08-28T06:11:53-04:00

- Added an optional persistent call-time notification with `Play fax (30 sec)`, `Stop`, and `Hide` actions.
- Added Android 13+ notification permission handling and a foreground media-playback service so the control remains available while the phone app is showing an answered call.
- Added an in-app `Enable call-time fax button` control and clear speakerphone/acoustic-playback instructions.
- Registered the media-playback service and its required foreground-service and notification permissions.
- Verified `./gradlew test assembleDebug` completes successfully.

## 2026-08-28T01:10:54-04:00

- Accepted all seven Google Android SDK package licenses in a user-writable temporary SDK.
- Installed Android Platform 37.0, Build Tools 36.0.0, and platform-tools for build verification.
- Supplied a portable Java 21 compiler because the machine's system Java installation contained only a runtime.
- Verified `./gradlew test assembleDebug` completes successfully.
- Rebuilt `app/build/outputs/apk/debug/app-debug.apk` (SHA-256 `3581650d6559f5b24eaf6b98e6c140f012829f7468b6c9a74ee6558a58246f8a`).

## 2026-08-28T00:48:08-04:00

- Added the requested fourth handling choice to the UI so its status is explicit: answering a cellular call and transmitting generated fax audio is unavailable to third-party apps through Android's public telephony APIs.
- Added a functional 30-second fax CNG preview using the standard 1100 Hz calling-tone cadence, with start and stop controls and lifecycle cleanup.
- Kept the preview separate from call screening so the app does not falsely claim that locally played audio is transmitted to a caller.

## 2026-08-27T20:36:24-04:00

- Created the first functional Android application, CallShield `0.1.0` (`com.lowell.callshield`).
- Added a self-contained Gradle 9.5 / Android Gradle Plugin 9.3.2 build targeting Android API 37 with Android 10 (API 29) as the minimum version.
- Implemented Android call-screening role onboarding and active/inactive status reporting.
- Implemented `CallScreeningService` handling with a deterministic local rule engine and safe allow-on-error behavior.
- Added independent settings for failed carrier verification, foreign numbers, and non-contact numbers.
- Added reject and silence actions, home calling-code configuration, persistent local preferences, and platform-limitation disclosures.
- Added eight unit tests covering rule priority, disabled rules, action safety, and number classification.
- Verified `./gradlew test` and `./gradlew assembleDebug` complete successfully using Android Platform 37.
- Produced `app/build/outputs/apk/debug/app-debug.apk` (SHA-256 `ef7485431b083539a9da5cd2307f14ca8a3d5b12e933eecd9416dc51b183e7ee`).
- Added build, installation, feature, and limitation documentation to `README.md`.

## 2026-08-27T20:03:13-04:00

- Read `plan.md` and `requirements.md`.
- Added `implementation-plan.md`, converting the initiative into phased architecture, delivery, testing, acceptance, risk, and release work.
- Documented current Android feasibility constraints for private/unknown call screening, carrier spoof-verification signals, the five-second screening deadline, and fax-audio playback.
- Created the timestamped request log required by `requirements.md`.
