# CallShield

CallShield is an Android 10+ call-screening MVP. It uses Android's public `CallScreeningService` API to apply local rules to screenable incoming calls.  

I wanted an application that would simply block people not in my contacts list: that avoided effectively ALL spam and spoof calls for me.  
you will have to turn it off, if for example, you were in a job search and needed to allow recruiters to call you, or if you were expecting a repairman or something to return your call.  

The app is free, install it yourself, and only for android.  
i really wanted to do something like play a fax sound on pickup, but that is not allowed by androids built in rules(even though they can inject a voice bot that can screen calls) . so the only thing i could do is actively block.

no tracking, no phone home, no subscription.
just simple and free. it has been working very well for me for the last six months.

![callshield_screenshot.png](/media/lowell/storage/github/CallShield/callshield_screenshot.png)

## Version 0.1 features

- Requests and reports the Android call-screening role.
- Independently toggles failed carrier verification, foreign-number, and non-contact rules.
- Rejects or silences matching calls.
- Shows received and rejected-call counts, including rejected calls in the last 1, 7, and 30 days.
- Uses a deterministic, local rule engine with allow-on-error behavior.
- Clearly discloses Android limitations for private/unknown presentations.

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

Caller verification is a carrier-provided risk signal and cannot prove spoofing. Android does not deliver restricted, unknown, unavailable, or payphone presentations to third-party screening services.
