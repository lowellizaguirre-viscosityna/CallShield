# CallShield

CallShield is an Android 10+ call-screening MVP. It uses Android's public `CallScreeningService` API to apply local rules to screenable incoming calls.

## Version 0.1 features

- Requests and reports the Android call-screening role.
- Independently toggles failed carrier verification, foreign-number, and non-contact rules.
- Rejects or silences matching calls.
- Provides a stoppable 30-second fax CNG preview and an optional persistent notification control for use after manually answering a call.
- Uses a deterministic, local rule engine with allow-on-error behavior.
- Clearly discloses Android limitations for private/unknown presentations and fax-audio playback.

## Build

Requirements:

- JDK 17 or newer
- Android SDK Platform 37 and Build Tools 36.0.0

Run:

```shell
./gradlew test assembleDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk` on a physical Android phone, open CallShield, and grant the call-screening role when prompted.

## Important limitations

Caller verification is a carrier-provided risk signal and cannot prove spoofing. Android does not deliver restricted, unknown, unavailable, or payphone presentations to third-party screening services. The public screening APIs do not support answering a carrier call and transmitting a prerecorded fax sound.

The call-time notification plays the tone through the phone speaker. Turn on speakerphone before tapping it. Android does not inject the tone into the call, so whether the caller hears it depends on the device's microphone and echo-cancellation behavior.
