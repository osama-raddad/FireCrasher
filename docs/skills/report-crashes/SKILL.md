---
name: report-crashes
description: Forward FireCrasher-caught exceptions to a crash reporter (Crashlytics, Sentry, etc.) while still recovering the app. Use when wiring crash reporting, logging non-fatals, or "make sure we still get crash reports after recovery".
---

# Report crashes while recovering

FireCrasher keeps the app alive, but you still want every crash recorded.
`CrashListener.onCrash` gives you the `Throwable` before recovery runs — log it
there, then call `recover()`. Because the app survives, these arrive at your
reporter as **non-fatal / handled** exceptions, not fatal crashes.

## Log, then recover

```kotlin
FireCrasher.install(this, object : CrashListener() {
    override fun onCrash(throwable: Throwable) {
        // 1. Report first, so nothing is lost if recovery itself struggles.
        FirebaseCrashlytics.getInstance().recordException(throwable)   // or Sentry.captureException(throwable)

        // 2. Then recover.
        recover()
    }
})
```

The order matters: report **before** `recover()` so the report is captured even
if a subsequent recovery attempt kills the process.

## Add context

`retryCount` tells you how many times FireCrasher has already tried to recover
this crash — useful signal on a report:

```kotlin
override fun onCrash(throwable: Throwable) {
    FirebaseCrashlytics.getInstance().apply {
        setCustomKey("firecrasher_retry", FireCrasher.retryCount)
        recordException(throwable)
    }
    recover()
}
```

## Don't forget out-of-process deaths

`onCrash` only sees JVM exceptions on threads FireCrasher controls. Native
crashes and ANRs never reach it — report those separately via the
`detect-native-crashes-and-anrs` skill (`onPreviousProcessExit`).

## Java

```java
FireCrasher.install(this, new CrashListener() {
    @Override
    public void onCrash(Throwable throwable) {
        FirebaseCrashlytics.getInstance().recordException(throwable);
        recover();
    }
});
```
