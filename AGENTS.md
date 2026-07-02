# AGENTS.md

Guidance for AI coding agents working in this repository. This is the
canonical instructions file; `CLAUDE.md` points here. Module-specific notes
live in `firecrasher/AGENTS.md` and `app/AGENTS.md`.


## What this is

**FireCrasher** is an Android library that catches uncaught exceptions and
runs a staged **recovery** sequence to keep the app alive instead of letting
it die. It is published to JitPack as `com.github.osama-raddad:FireCrasher`.
Current version is **2.2.0**.

The recovery escalation ladder has three levels (`CrashLevel`):

- **LEVEL_ONE** — restart the crashed activity (`recreate()` on the first
  attempt, then relaunch-and-finish). Used while `retryCount <= 1`.
- **LEVEL_TWO** — the activity keeps crashing, so pop the back stack (go back)
  if there is another activity behind it.
- **LEVEL_THREE** — nothing to go back to, so relaunch the whole app from its
  launcher activity.

The core trick (`FireLooper`) is to replace the main-thread message loop with a
reflection-driven loop wrapped in a try/catch, so an uncaught exception on the
main thread does not tear down the process — the loop reposts itself and
recovery runs.

## Module layout

This is a two-module Gradle project (`settings.gradle`):

- **`firecrasher/`** — the published library. This is where real work happens.
- **`app/`** — a demo application (`com.osama.firecrasherdemo`) that installs
  FireCrasher and deliberately crashes to exercise recovery. Not published.

### Library source (`firecrasher/src/main/java/com/osama/firecrasher/`)

| File | Role |
|------|------|
| `FireCrasher.kt` | Public API entry point (Kotlin `object`). `install` (listener and `CrashEvent`-lambda overloads), `uninstall`, `evaluate`, `recover`, and the `ApplicationExitInfo` query helpers. Holds `retryCount` and the recovery-state persistence logic. |
| `FireCrasherConfig.kt` | Recovery-policy configuration: `Builder`, `RecoveryPredicate` (shouldRecover filter), `BackStackDepthProvider` (single-activity/Compose depth), the LEVEL_ONE retry threshold, and the crash-loop breaker settings (on by default: 10 crashes / 60 s). |
| `CrashEvent.kt` | Crash context delivered to the lambda install form: throwable, thread, activity, retryCount, suggestedLevel, plus `recover()` shortcuts. Internal constructor so fields can be added compatibly. |
| `CrashHandler.java` | `Thread.UncaughtExceptionHandler` + `ActivityLifecycleCallbacks`. Tracks the current activity and the live activity count (`getBackStackCount`). Applies `shouldRecover` and the crash-loop breaker; declined crashes chain to the pre-install default handler. Delivers accepted crashes to the listener on the main thread. |
| `FireLooper.kt` | Reflection-based replacement main loop that survives exceptions. `isSafe` guards against double-install; `uninstall()` posts the EXIT sentinel to hand control back to the framework loop. |
| `CrashListener.kt` | Abstract callback the consumer implements: `onCrash` (required), `onPreviousProcessExit` (optional), plus protected `recover`/`evaluate` helpers. |
| `CrashLevel.kt` | The three-level recovery enum. |
| `RecoveryState.kt` | `RecoveryState` data class + `RecoveryStateCodec` — encodes recovery progress into a ≤128-byte blob stored via `ActivityManager.setProcessStateSummary` so recovery escalation survives process death (API 30+). |

### Tests (`firecrasher/src/test/java/...`)

JVM unit tests run under **Robolectric** (no device/emulator needed):

- `CrashLevelEvaluationTest` — the level-selection logic (pure `evaluate`).
- `ConfigEvaluationTest` — the configurable retry threshold and `Builder` validation.
- `BackStackCountTest` — activity counting via lifecycle callbacks.
- `CrashHandlerTest` — crash delivery to the listener.
- `ShouldRecoverTest` — the `shouldRecover` filter and the die-normally path
  (previous-handler chaining, injectable process terminator).
- `CrashLoopBreakerTest` — the sliding-window breaker with an injected clock.
- `LambdaInstallTest` — `CrashEvent` delivery and `backStackDepthProvider`.
- `UninstallTest` — handler restoration and reinstall.
- `GoBackDispatchTest` — LEVEL_TWO uses `OnBackPressedDispatcher` for
  `ComponentActivity`, legacy `onBackPressed()` for plain activities.
- `ExitInfoTest` — `ApplicationExitInfo` querying (`@Config(sdk = [30, 36])`).
- `RecoveryStateCodecTest` — encode/decode round-trips and the 128-byte limit.

Tests that install FireCrasher must use the internal
`install(..., hookMainLooper = false)` seam: the real replacement loop blocks
in `MessageQueue.next()` by design, which would park Robolectric's main
thread forever.

## Build & test

The build targets **JDK 21** and uses **AGP 9.2.1** (with its built-in Kotlin —
there is no standalone Kotlin plugin). Versions are centralized in the version
catalog `gradle/libs.versions.toml`; do not hardcode versions in `build.gradle`
files.

```bash
./gradlew build          # compiles, runs check (tests + lint + apiCheck)
./gradlew :firecrasher:test    # library unit tests only
./gradlew :firecrasher:apiCheck    # verify public ABI against the baseline
./gradlew :firecrasher:apiDump     # regenerate the ABI baseline after API changes
```

CI (`.github/workflows/build.yml`) runs `./gradlew build --stacktrace` on
push to `master` and on every PR, using JDK 21.

## Conventions that matter

**Explicit API mode.** The library is compiled with Kotlin `explicitApi()`.
Every public/internal declaration must carry explicit visibility and an
explicit return type. Keep implementation details `internal` or `private` so
they stay out of the published surface.

**ABI baseline is enforced.** The public binary API is frozen in
`firecrasher/api/firecrasher.api` (binary-compatibility-validator). Any change
to the public API surface will fail `apiCheck` in CI. When you intentionally
change the public API, run `./gradlew :firecrasher:apiDump` and commit the
updated `firecrasher.api` in the same change.

**minSdk 23, targetSdk/compileSdk 36.** minSdk was raised from 21 to 23 in
2.1.0 by the `androidx.activity` dependency. Guard newer-API calls with
`Build.VERSION.SDK_INT` checks — the `ApplicationExitInfo` and
`setProcessStateSummary` features are API 30+ and degrade gracefully (return
empty/null) below that.

**Deprecations are deliberate.** `overridePendingTransition` and framework
`onBackPressed()` are kept (with `@Suppress("DEPRECATION")`) for minSdk 23
compatibility; do not "fix" them without preserving the old path.

**Reproducible artifacts.** The library build pins archive timestamps and file
order (`preserveFileTimestamps = false`, `reproducibleFileOrder = true`)
because JitPack consumes AARs by checksum. Don't remove this.

**Language mix.** `CrashHandler` is Java; the rest of the library is Kotlin.
Match the language of the file you're editing.

## Documentation

`README.md` is the user-facing documentation (install, usage, API 30+ exit
reporting, the "What's new" changelog). When you change public behavior, the
supported SDK range, or the version, update `README.md` — especially the
version in the install snippet and the `VERSION_NAME` default in
`firecrasher/build.gradle` (currently `2.2.0`).

## Publishing

JitPack builds from git tags (`jitpack.yml` pins openjdk21). The Maven
publication is configured in `firecrasher/build.gradle`; JitPack remaps the
coordinates to `com.github.osama-raddad` at build time. No manual publish step
is run from this repo.

## Git workflow

Default branch is `master`. Do not push directly to it; open a PR. Only create
a PR when explicitly asked. Keep the ABI baseline, README version, and
`VERSION_NAME` in sync within a single change when bumping the version.
