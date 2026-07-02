<p align="center">
  <img src="https://cdn-images-1.medium.com/max/2600/1*7CVLni2XSYNFzy7dRHLtsQ.png" alt="FireCrasher"/>
</p>

<h1 align="center">FireCrasher</h1>

<p align="center">
  <b>Catch uncaught Android exceptions and recover — instead of crashing to the launcher.</b>
</p>

<p align="center">
  <a href="https://github.com/osama-raddad/FireCrasher/actions/workflows/build.yml"><img src="https://github.com/osama-raddad/FireCrasher/actions/workflows/build.yml/badge.svg" alt="Build Status"/></a>
  <a href="https://jitpack.io/#osama-raddad/FireCrasher"><img src="https://jitpack.io/v/osama-raddad/FireCrasher.svg" alt="JitPack"/></a>
  <a href="https://android-arsenal.com/api?level=23"><img src="https://img.shields.io/badge/API-23%2B-blue.svg?style=flat" alt="API 23+"/></a>
  <a href="https://opensource.org/licenses/Apache-2.0"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License Apache 2.0"/></a>
</p>

---

## Contents

- [What it does](#what-it-does)
- [How recovery works](#how-recovery-works)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Usage](#usage)
  - [Report crashes while recovering](#report-crashes-while-recovering)
  - [Configure the recovery policy](#configure-the-recovery-policy)
  - [React to the crash level](#react-to-the-crash-level)
  - [Single-activity and Jetpack Compose apps](#single-activity-and-jetpack-compose-apps)
  - [Detect native crashes and ANRs (API 30+)](#detect-native-crashes-and-anrs-api-30)
  - [Turning FireCrasher off](#turning-firecrasher-off)
- [API reference](#api-reference)
- [What's new in 2.2.0](#whats-new-in-220)
- [What's new in 2.1.0](#whats-new-in-210)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

## What it does

An uncaught exception on Android's main thread tears down the whole process:
the app disappears and the user is dropped back at the launcher. Crashing is one
of the fastest ways to lose a user — a large share will one-star or delete an
app after a single bad crash.

Most production crashes, though, are **localized and transient**: a bad state on
one screen, a null from a flaky response, a race that fires once. FireCrasher's
premise is that the app should *absorb* that exception and get the user back to a
working state rather than die.

It does this by replacing the main-thread message loop with one that dispatches
messages inside a `try/catch`. When something throws, FireCrasher hands the
exception to your `CrashListener` and runs a staged recovery — so the crash
becomes a hiccup, not an exit. **Your crash reporting still fires**; the app
just stays alive while it reports.

## How recovery works

When a crash is caught, FireCrasher picks a recovery level based on how many
times it has already tried and whether there's a screen to fall back to:

| Level | When | What happens |
|-------|------|--------------|
| **LEVEL&nbsp;ONE** | First one or two crashes (`retryCount ≤ 1`) | Restart the crashing activity (`recreate()`, then relaunch-and-finish). Treats it as an occasional glitch. |
| **LEVEL&nbsp;TWO** | The activity keeps crashing **and** there's a back stack | Give up on the dead screen and go back to the previous one (predictive-back aware). |
| **LEVEL&nbsp;THREE** | Keeps crashing with nothing to go back to | Restart the whole app from its launcher activity. |

Recovery escalates cheaply first and only gets more disruptive once the cheaper
options have demonstrably failed. On API 30+ the current level even survives
process death, so a crash *during* recovery escalates instead of looping the
same broken screen. See [CONTEXT.md](CONTEXT.md) for the full design rationale.

## Requirements

- **minSdk 23+** (raised from 21 in 2.1.0 by the `androidx.activity` dependency).
- A toolchain that accepts **Java 21 bytecode and Kotlin 2.1+ metadata** —
  AGP 8.2+ / Kotlin 2.1+.
- No native code, so **no 16 KB page-size alignment concerns**.

## Installation

**1. Add the JitPack repository** in `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**2. Add the dependency** in your app module's `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.osama-raddad:FireCrasher:2.2.0'
}
```

## Quick start

Install FireCrasher in your `Application.onCreate` — before any activity is
created — and start recovery from the crash callback:

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FireCrasher.install(this) { crash ->
            crash.recover()
        }
    }
}
```

The lambda receives a [`CrashEvent`](#crashevent) with the throwable and its
context (thread, activity, retry count, suggested level). The subclass form
still works — use it when you also need `onPreviousProcessExit`:

```kotlin
FireCrasher.install(this, object : CrashListener() {
    override fun onCrash(throwable: Throwable) {
        recover()
    }
})
```

Register the Application in your manifest if it isn't already:

```xml
<application android:name=".App" ... />
```

That's it — uncaught main-thread exceptions now trigger recovery instead of
killing the app.

## Usage

### Report crashes while recovering

`onCrash` hands you the `Throwable` before recovery runs. Report it **first**
(so nothing is lost if a later recovery attempt struggles), then recover.
Because the app survives, these reach your reporter as *non-fatal / handled*
exceptions.

```kotlin
FireCrasher.install(this) { crash ->
    FirebaseCrashlytics.getInstance().recordException(crash.throwable)   // or Sentry, Bugsnag, …
    crash.recover()
}
```

### Configure the recovery policy

Pass a `FireCrasherConfig` to `install` to control how much FireCrasher
retries, which throwables it recovers, and when it gives up:

```kotlin
val config = FireCrasherConfig.Builder()
    // Restart the crashing activity this many times before escalating (default 2).
    .setLevelOneRetries(2)
    // Let JVM errors (OutOfMemoryError, StackOverflowError, …) kill the app
    // normally instead of recovering. Default: recover everything.
    .setShouldRecover { it !is Error }
    // Stop recovering after 10 crashes within 60s — the app then dies exactly
    // as it would without FireCrasher, and your crash reporter still gets the
    // fatal. This is the default; disableCrashLoopBreaker() turns it off.
    .setCrashLoopBreaker(10, 60_000L)
    .build()

FireCrasher.install(this, config) { crash ->
    report(crash.throwable)
    crash.recover()
}
```

The same builder works from Java — `RecoveryPredicate` is a SAM interface:

```java
FireCrasherConfig config = new FireCrasherConfig.Builder()
        .setShouldRecover(t -> !(t instanceof Error))
        .build();
FireCrasher.install(this, config, new CrashListener() { ... });
```

When recovery is declined — by `shouldRecover` or the crash-loop breaker — the
exception goes to whatever `Thread.getDefaultUncaughtExceptionHandler()` was
installed **before** FireCrasher (typically your crash reporter's), so fatals
are reported exactly as they would be without FireCrasher.

### React to the crash level

Use `evaluate { activity, level -> … }` to inspect the level FireCrasher would
use and show your own recovery UX, then call `recover { … }`:

```kotlin
FireCrasher.install(this, object : CrashListener() {
    override fun onCrash(throwable: Throwable) {
        report(throwable)

        evaluate { activity, level ->
            val context = activity ?: return@evaluate
            recover {
                val message = when (level) {
                    CrashLevel.LEVEL_ONE   -> "Recovering…"
                    CrashLevel.LEVEL_TWO   -> "Returning to the previous screen"
                    CrashLevel.LEVEL_THREE -> "Restarting the app"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
})
```

You can also force a specific level: `recover(CrashLevel.LEVEL_THREE)`.

> **Tip:** keep recovery UX fast and non-blocking — the user just hit a crash, so
> a brief spinner or toast beats a modal dialog. Do the reporting before showing UX.

### Single-activity and Jetpack Compose apps

FireCrasher counts *activities* to decide whether there is somewhere to go
back to (LEVEL_TWO). A single-activity app — the usual shape of a Jetpack
Compose app — always reports zero depth, so the ladder would jump from
restarting the activity straight to relaunching the whole app.

Tell FireCrasher about your navigation stack instead with
`setBackStackDepthProvider`. Going back is dispatched through
`OnBackPressedDispatcher`, which Navigation Compose participates in — so
LEVEL_TWO pops the crashed *destination* and keeps the rest of your app alive:

```kotlin
// Hold a reference your Application can reach, e.g. set from your activity:
var navDepth: () -> Int = { 0 }

val config = FireCrasherConfig.Builder()
    .setBackStackDepthProvider { navDepth() }
    .build()

// In your composable host, keep it current:
navDepth = { navController.previousBackStackEntry?.let { 1 } ?: 0 }
```

`install()` still belongs in `Application.onCreate` — nothing else is
Compose-specific.

### Detect native crashes and ANRs (API 30+)

Native crashes, ANRs, and low-memory kills terminate the process before any
in-process handler can run, so `onCrash` never sees them. On **API 30+**,
FireCrasher surfaces the system's own `ApplicationExitInfo` record of such
deaths on the next launch.

Override `onPreviousProcessExit`, called from `install()` when the last process
died abnormally:

```kotlin
FireCrasher.install(this, object : CrashListener() {
    override fun onCrash(throwable: Throwable) {
        recover()
    }

    override fun onPreviousProcessExit(exitInfo: ApplicationExitInfo) {
        // The record may predate the last launch — check exitInfo.timestamp.
        FirebaseCrashlytics.getInstance()
            .log("previous exit: ${exitInfo.reason} ${exitInfo.description}")
    }
})
```

Or query the records directly, from Kotlin or Java:

```kotlin
val lastCrash = FireCrasher.getLastAbnormalExit(context)          // most recent crash/native/ANR, or null
val history   = FireCrasher.getHistoricalExitReasons(context, 16) // newest first
```

```java
ApplicationExitInfo lastCrash = FireCrasher.getLastAbnormalExit(context);
List<ApplicationExitInfo> history = FireCrasher.getHistoricalExitReasons(context);
```

Both return empty/null below API 30, so no version guard is needed in your code.
The same record persists across launches — de-duplicate on `exitInfo.timestamp`
so you don't report the same death twice.

### Turning FireCrasher off

`uninstall()` undoes `install()`: the pre-install default exception handler is
restored, the replacement main loop exits at its next message, and subsequent
crashes behave as if FireCrasher was never there. Useful as a remote-config
kill switch or for A/B-testing recovery:

```kotlin
if (!remoteConfig.getBoolean("firecrasher_enabled")) {
    FireCrasher.uninstall()
}
```

`install()` may be called again later. Don't call `uninstall()` from inside
the crash callback itself.

## API reference

Everything lives in the `com.osama.firecrasher` package.

### `FireCrasher` (object)

| Member | Description |
|--------|-------------|
| `install(application) { crash -> … }` | Hook the main loop and activity lifecycle, with a [`CrashEvent`](#crashevent) lambda. Call once, in `Application.onCreate`. |
| `install(application, config) { crash -> … }` | Same, with a [`FireCrasherConfig`](#firecrasherconfig). |
| `install(application, [config,] listener)` | Subclass form; needed for `onPreviousProcessExit`. |
| `uninstall()` | Undo `install()` — restore the previous handler and stop the replacement loop. |
| `retryCount: Int` | How many times recovery has retried the current crash (read-only). |
| `evaluate(): CrashLevel` | The level recovery would use right now. |
| `recover(level = evaluate(), onRecover = null)` | Run recovery, optionally at a forced level, with an optional callback after it starts. |
| `getLastAbnormalExit(context)` | Most recent crash / native crash / ANR exit record, or `null`. API 30+. |
| `getHistoricalExitReasons(context, maxCount = 16)` | Past process exits, newest first. API 30+. |

### `FireCrasherConfig`

Built with `FireCrasherConfig.Builder`; pass to `install`. `DEFAULT` preserves
historical behavior except the crash-loop breaker, which defaults to on.

| Builder method | Description |
|--------|-------------|
| `setLevelOneRetries(count)` | Crashes absorbed by restarting the same activity before escalating (default 2). |
| `setShouldRecover(predicate)` | `RecoveryPredicate` consulted per crash; `false` hands the crash to the previous default handler (default: recover everything). |
| `setCrashLoopBreaker(maxCrashes, windowMillis)` | Stop recovering after this many crashes inside the window (default 10 / 60 000 ms). |
| `disableCrashLoopBreaker()` | Restore the pre-2.2.0 always-recover behavior. |
| `setBackStackDepthProvider(provider)` | Report your own navigation depth (single-activity / Compose apps). |

### `CrashEvent`

Delivered to the `install` lambda. Properties: `throwable`, `thread`,
`activity` (nullable), `retryCount`, `suggestedLevel`. Methods: `recover()`
and `recover(level)`.

### `CrashListener` (abstract — you implement it)

| Member | Description |
|--------|-------------|
| `onCrash(throwable)` | **Required.** Called on the main thread when an exception is caught. Report and call `recover()` here. |
| `onPreviousProcessExit(exitInfo)` | Optional. Called from `install()` on API 30+ when the previous process died abnormally. |
| `recover(…)` / `evaluate(…)` | Protected helpers to start recovery / inspect the crash level from inside the listener. |

### `CrashLevel` (enum)

`LEVEL_ONE` · `LEVEL_TWO` · `LEVEL_THREE` — the recovery escalation ladder
described [above](#how-recovery-works).

## What's new in 2.2.0

- **Crash-loop breaker, on by default.** After 10 caught crashes within 60
  seconds, FireCrasher stops recovering and lets the app die exactly as it
  would without the library — your crash reporter still receives the fatal.
  This is a deliberate behavioral change; restore the old behavior with
  `FireCrasherConfig.Builder().disableCrashLoopBreaker()`.
- **Configurable recovery policy.** `FireCrasherConfig` controls the
  LEVEL_ONE retry threshold (`setLevelOneRetries`), which throwables are
  recovered (`setShouldRecover`), and the loop breaker.
- **Lambda install with rich crash context.**
  `FireCrasher.install(app) { crash -> … }` delivers a `CrashEvent` carrying
  the throwable, crashed thread, foreground activity, retry count, and
  suggested level — no subclass needed. `FireCrasher.recover()` is now
  callable without arguments.
- **Single-activity / Compose support.** `setBackStackDepthProvider` lets a
  single-activity app report its navigation depth so LEVEL_TWO (go back) can
  pop a Compose destination instead of restarting the whole app.
- **`uninstall()`.** Fully reversible installation — restore the previous
  handler and main loop for kill switches, A/B tests, and test isolation.
- **Previous handler chaining (fix).** `install()` now captures the
  previously installed default exception handler instead of silently
  replacing it; any crash FireCrasher declines to recover reaches it.

## What's new in 2.1.0

- **Predictive-back compatible recovery.** LEVEL_TWO recovery goes through
  `OnBackPressedDispatcher` when the crashed activity is a `ComponentActivity`,
  so the recovery chain keeps working on apps targeting Android 16 (API 36),
  where the system no longer calls `Activity.onBackPressed()`. Plain framework
  activities still get the legacy call.
- **Crash reports beyond the JVM.** On API 30+ FireCrasher reads the system's
  [`ApplicationExitInfo`](https://developer.android.com/reference/android/app/ApplicationExitInfo)
  records, surfacing native crashes and ANRs an in-process handler can never
  intercept — via `onPreviousProcessExit` and `getLastAbnormalExit`.
- **Restart-loop protection.** On API 30+ the current recovery level survives
  process death (`ActivityManager.setProcessStateSummary`), so a crash during
  recovery escalates instead of looping the same crashing activity.
- **Locked-down API surface.** The library is compiled in Kotlin explicit-API
  mode and every change is checked against a committed ABI baseline
  (`firecrasher/api/firecrasher.api`) in CI.
- **minSdk raised to 23** (from 21) by the `androidx.activity` dependency.

## Documentation

- **[CONTEXT.md](CONTEXT.md)** — design rationale: why the message-loop
  replacement, the recovery ladder, and cross-process recovery state.
- **[AGENTS.md](AGENTS.md)** — repository guide for contributors and AI agents.
- **[docs/skills/](docs/skills/)** — drop-in agent skills for integrating
  FireCrasher into your own app.

## Contributing

Pull requests and stars are always welcome. When changing the library's public
API, regenerate the ABI baseline (`./gradlew :firecrasher:apiDump`) and commit
it with your change — see [AGENTS.md](AGENTS.md) for the full workflow.

I'd also love to hear where you're using FireCrasher — email
osama.s.raddad@gmail.com with questions or suggestions.

## License

```
Copyright 2019, Osama Raddad

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
