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
  - [React to the recovery level](#react-to-the-recovery-level)
  - [Detect native crashes and ANRs (API 30+)](#detect-native-crashes-and-anrs-api-30)
- [API reference](#api-reference)
- [What's new in 3.0.0](#whats-new-in-300)
- [Migrating from 2.x](#migrating-from-2x)
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
exception to your `onCrash` handler and runs a staged recovery — so the crash
becomes a hiccup, not an exit. **Your crash reporting still fires**; the app
just stays alive while it reports.

## How recovery works

When a crash is caught, FireCrasher picks a recovery level based on how many
times it has already tried and whether there's a screen to fall back to:

| Level | When | What happens |
|-------|------|--------------|
| **RESTART_ACTIVITY** | First one or two crashes (`retryCount ≤ 1`) | Restart the crashing activity (`recreate()`, then relaunch-and-finish). Treats it as an occasional glitch. |
| **GO_BACK** | The activity keeps crashing **and** there's a back stack | Give up on the dead screen and go back to the previous one (predictive-back aware). |
| **RELAUNCH_APP** | Keeps crashing with nothing to go back to | Restart the whole app from its launcher activity. |

Recovery escalates cheaply first and only gets more disruptive once the cheaper
options have demonstrably failed. On API 30+ the current level even survives
process death, so a crash *during* recovery escalates instead of looping the
same broken screen. See [CONTEXT.md](CONTEXT.md) for the full design rationale.

## Requirements

- **minSdk 23+**.
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
    implementation 'com.github.osama-raddad:FireCrasher:3.0.0'
}
```

## Quick start

Install FireCrasher in your `Application.onCreate` — before any activity is
created. A bare install already recovers automatically at the evaluated level:

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        installFireCrasher()
    }
}
```

Register the Application in your manifest if it isn't already:

```xml
<application android:name=".App" ... />
```

That's it — uncaught main-thread exceptions now trigger recovery instead of
killing the app.

## Usage

### Report crashes while recovering

Configure `onCrash` to take over crash handling. The handler runs on the main
thread with everything about the crash in scope: `throwable`, `activity`,
`level`, and `retryCount`. Report the throwable **first** (so nothing is lost
if a later recovery attempt struggles), then call `recover()`. Because the app
survives, these reach your reporter as *non-fatal / handled* exceptions.

```kotlin
installFireCrasher {
    onCrash {
        FirebaseCrashlytics.getInstance().recordException(throwable)   // or Sentry, Bugsnag, …
        recover()
    }
}
```

### React to the recovery level

The evaluated `level` is already in scope, so you can show your own recovery
UX and then `recover { … }` — no separate evaluate step:

```kotlin
installFireCrasher {
    onCrash {
        report(throwable)

        val context = activity ?: return@onCrash
        recover {
            val message = when (level) {
                RecoveryLevel.RESTART_ACTIVITY -> "Recovering…"
                RecoveryLevel.GO_BACK          -> "Returning to the previous screen"
                RecoveryLevel.RELAUNCH_APP     -> "Restarting the app"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
```

You can also force a specific level: `recover(RecoveryLevel.RELAUNCH_APP)`.

> **Tip:** keep recovery UX fast and non-blocking — the user just hit a crash, so
> a brief spinner or toast beats a modal dialog. Do the reporting before showing UX.

### Detect native crashes and ANRs (API 30+)

Native crashes, ANRs, and low-memory kills terminate the process before any
in-process handler can run, so `onCrash` never sees them. On **API 30+**,
FireCrasher surfaces the system's own `ApplicationExitInfo` record of such
deaths on the next launch.

Configure `onPreviousProcessExit`, called from install when the last process
died abnormally:

```kotlin
installFireCrasher {
    onCrash { recover() }

    onPreviousProcessExit { exitInfo ->
        // Only invoked on API 30+; the guard is for lint, which can't see
        // that guarantee through the lambda.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // The record may predate the last launch — check exitInfo.timestamp.
            FirebaseCrashlytics.getInstance()
                .log("previous exit: ${exitInfo.reason} ${exitInfo.description}")
        }
    }
}
```

Or query the records directly from any `Context`:

```kotlin
val lastCrash = context.lastAbnormalExit()        // most recent crash/native/ANR, or null
val history   = context.historicalExitReasons(16) // newest first
```

Both return empty/null below API 30, so no version guard is needed in your code.
The same record persists across launches — de-duplicate on `exitInfo.timestamp`
so you don't report the same death twice.
(From Java, the extensions are reachable as `ExitInfoKt.lastAbnormalExit(context)`
and `ExitInfoKt.historicalExitReasons(context, 16)`.)

## API reference

Everything lives in the `com.osama.firecrasher` package. The whole surface:

### Installing

| Member | Description |
|--------|-------------|
| `Application.installFireCrasher { … }` | Hook the main loop and activity lifecycle. Call once, in `Application.onCreate`. The lambda configures handlers; a bare call auto-recovers. |
| `FireCrasher.install(application) { … }` | The same entry point in object form. |

### `FireCrasherConfig` (the install lambda's receiver)

| Member | Description |
|--------|-------------|
| `onCrash { … }` | Called on the main thread when an exception is caught, with a `CrashScope` receiver. Last registration wins; the default is `{ recover() }`. |
| `onPreviousProcessExit { exitInfo -> … }` | Optional. Called from install on API 30+ when the previous process died abnormally. |

### `CrashScope` (the `onCrash` receiver)

| Member | Description |
|--------|-------------|
| `throwable: Throwable` | The uncaught exception. Report it before recovering. |
| `activity: Activity?` | The foreground activity when the crash was delivered, if any. |
| `level: RecoveryLevel` | The recovery level FireCrasher evaluated for this crash. |
| `retryCount: Int` | How many times recovery has retried the current crash. |
| `recover(level = this.level) { activity -> … }` | Run recovery, optionally at a forced level, with a callback after it starts. |

### `RecoveryLevel` (enum)

`RESTART_ACTIVITY` · `GO_BACK` · `RELAUNCH_APP` — the recovery escalation
ladder described [above](#how-recovery-works).

### `Context` extensions

| Member | Description |
|--------|-------------|
| `Context.lastAbnormalExit()` | Most recent crash / native crash / ANR exit record, or `null`. API 30+. |
| `Context.historicalExitReasons(maxCount = 16)` | Past process exits, newest first. API 30+. |

## What's new in 3.0.0

- **A fluent Kotlin-first API.** Installation is a DSL —
  `installFireCrasher { onCrash { … } }` — and the crash handler receives a
  `CrashScope` carrying `throwable`, `activity`, `level`, and `retryCount`,
  with `recover()` right there in scope. The `CrashListener` subclass, the
  separate `evaluate` step, and the static exit-info helpers are gone.
- **Recovery levels with meaningful names.** `CrashLevel.LEVEL_ONE/TWO/THREE`
  is now `RecoveryLevel.RESTART_ACTIVITY / GO_BACK / RELAUNCH_APP`.
- **Zero-config install.** `installFireCrasher()` alone recovers automatically
  at the evaluated level.
- **A smaller, deliberate surface.** `CrashHandler` (accidentally public in
  2.x) is internal now; the library is 100% Kotlin, still with explicit-API
  mode and a CI-enforced ABI baseline. No new dependencies.

## Migrating from 2.x

3.0.0 is a breaking release. Everything has a direct replacement:

| 2.x | 3.0.0 |
|-----|-------|
| `FireCrasher.install(app, object : CrashListener() { override fun onCrash(t) { … } })` | `app.installFireCrasher { onCrash { … } }` (`throwable` in scope) |
| `CrashListener.onPreviousProcessExit(exitInfo)` override | `onPreviousProcessExit { exitInfo -> … }` in the install lambda |
| `evaluate { activity, level -> … }` | Just use `activity` / `level` — already in `onCrash` scope |
| `recover()` / `recover(level) { … }` | `recover()` / `recover(level) { … }` on the `onCrash` scope (unchanged shape) |
| `FireCrasher.retryCount` | `retryCount` in `onCrash` scope |
| `CrashLevel.LEVEL_ONE / LEVEL_TWO / LEVEL_THREE` | `RecoveryLevel.RESTART_ACTIVITY / GO_BACK / RELAUNCH_APP` |
| `FireCrasher.getLastAbnormalExit(context)` | `context.lastAbnormalExit()` |
| `FireCrasher.getHistoricalExitReasons(context, n)` | `context.historicalExitReasons(n)` |
| `FireCrasher.evaluate()` / `evaluateAsync { … }` | Removed — the evaluated `level` is in `onCrash` scope |

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
