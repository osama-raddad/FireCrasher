---
name: detect-native-crashes-and-anrs
description: Report native crashes, ANRs, and low-memory kills that FireCrasher's in-process handler cannot catch, using ApplicationExitInfo on API 30+. Use when you need a complete crash picture, or "why don't I get reports for native crashes / ANRs".
---

# Detect crashes the handler can't catch

FireCrasher's `onCrash` only sees JVM exceptions on threads it controls. Native
crashes, ANRs, and low-memory kills terminate the process before any in-process
handler runs — they are invisible to it.

On **API 30+**, Android records these in `ApplicationExitInfo`. FireCrasher
surfaces that record on the *next* launch so you can report it. All of these
return empty/null below API 30, so no version guard is needed in your code.

## Option A: the callback (recommended)

Configure `onPreviousProcessExit`. FireCrasher calls it from install when the
system has a record of the app dying abnormally last time:

```kotlin
installFireCrasher {
    onCrash { recover() }

    onPreviousProcessExit { exitInfo ->
        // Only invoked on API 30+; the guard is for lint, which can't see
        // that guarantee through the lambda.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // The record may predate the last launch — check freshness if it matters.
            val recent = System.currentTimeMillis() - exitInfo.timestamp < 60_000
            FirebaseCrashlytics.getInstance().log(
                "previous exit: reason=${exitInfo.reason} desc=${exitInfo.description} recent=$recent"
            )
        }
    }
}
```

`exitInfo.reason` values worth branching on include `REASON_CRASH`,
`REASON_CRASH_NATIVE`, `REASON_ANR`, and `REASON_LOW_MEMORY`.

## Option B: query directly

Ask for the records from any `Context` whenever you need them:

```kotlin
// The most recent crash / native crash / ANR, or null.
val lastCrash: ApplicationExitInfo? = lastAbnormalExit()

// Full history, newest first (default up to 16).
val history: List<ApplicationExitInfo> = historicalExitReasons(maxCount = 16)
```

(From Java: `ExitInfoKt.lastAbnormalExit(context)` and
`ExitInfoKt.historicalExitReasons(context, 16)`.)

## Notes

- **De-duplicate.** The same `ApplicationExitInfo` record persists across
  launches. Track the last-seen `exitInfo.timestamp` (e.g. in SharedPreferences)
  so you don't report the same death on every launch.
- An ANR record can carry a trace via `exitInfo.traceInputStream` — attach it to
  your report when present.
- Below API 30 these APIs no-op; combine this with `report-crashes` for the JVM
  exceptions FireCrasher *can* catch to get full coverage.
