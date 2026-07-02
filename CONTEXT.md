# CONTEXT.md

Background and design rationale for **FireCrasher** — the *why* behind the
code. For how to build, test, and work in the repository, see
[`AGENTS.md`](AGENTS.md); for user-facing install/usage, see
[`README.md`](README.md).

## The problem

An uncaught exception on Android's main thread tears down the process: the app
vanishes and the user is dumped to the launcher. Surveys of app users put
crashing near the top of what makes people abandon or one-star an app, and a
large share will delete an app outright after a bad crash. A single stray
`RuntimeException` shipped to production can therefore cost real users.

FireCrasher's premise: most production crashes are *localized and transient* —
a bad state on one screen, a null from a flaky response, a race that fires
once. If the app can absorb that exception and get the user back to a working
state instead of dying, the crash becomes a hiccup rather than an exit. Crash
**reporting** still happens; the app just doesn't die while it reports.

## The core insight: own the message loop

Android's main thread is a `Looper` pulling messages off a queue and
dispatching them. If a dispatched message throws, the exception propagates out
of the loop and the process dies.

FireCrasher (`FireLooper`) replaces that loop with its own: it uses reflection
to pull messages off the same `MessageQueue` (`MessageQueue.next()`,
`Message.target`) and dispatches them **inside a try/catch**. When a dispatch
throws, the loop hands the exception to the registered handler and reposts
itself instead of unwinding. The main thread keeps running, so recovery UI can
be shown and the app stays alive.

This is deliberately a low-level, reflection-based trick. It is the single most
load-bearing — and most fragile — part of the library, which is why the loop
degrades safely: if the private `next`/`target` members can't be reached, it
simply returns and the app falls back to normal (crashing) behavior rather than
misbehaving.

## The recovery ladder

Not every recovery is equal, so FireCrasher escalates. `CrashLevel` encodes
three stages, chosen by `FireCrasher.evaluate(retryCount, backStackCount)`:

1. **LEVEL_ONE — restart the activity.** The first one or two crashes are
   treated as occasional/behavioral. `recreate()` (then a relaunch-and-finish)
   gives the current screen a clean slate. Most transient crashes stop here.
2. **LEVEL_TWO — go back.** If the same activity keeps crashing (`retryCount`
   climbs past the threshold) and there is something behind it on the back
   stack, the crashing screen is considered dead and FireCrasher pops it,
   returning the user to a screen that *was* working.
3. **LEVEL_THREE — restart the app.** If there is nothing to go back to, the
   only safe state left is a fresh launch from the default activity.

The ladder is the whole product thesis: cheap local recovery first, escalating
to more disruptive recovery only when the cheap options have demonstrably
failed. `retryCount` is what distinguishes "try again" from "give up on this
screen."

## Surviving process death during recovery

Recovery itself can crash the process — worse, it can crash *while restarting
the very activity that just failed*, which would loop forever. In-process
counters (`retryCount`) don't survive a process kill, so escalation state would
reset on every relaunch and the app could restart the same crashing screen
indefinitely.

To break that loop, on API 30+ FireCrasher persists its recovery progress
where the *next* process can read it: `ActivityManager.setProcessStateSummary`
stores a small blob (≤128 bytes) alongside the process's
`ApplicationExitInfo`. `RecoveryStateCodec` encodes the current `CrashLevel`
and retry count into that blob (magic bytes + version + two bytes). On the next
launch, `restorePreviousRecoveryState` reads it back — but only if the exit is
recent (within `RECOVERY_STATE_MAX_AGE_MS`, 30 s), because an older record just
means the user relaunched normally. If recovery was mid-flight, the new process
**resumes the escalation ladder** (never falling back below the level that just
failed) instead of restarting the activity that killed it.

## Reporting crashes the handler can't see

An in-process `UncaughtExceptionHandler` only ever sees JVM exceptions on
threads it controls. Native crashes, ANRs, and low-memory kills terminate the
process before any handler runs — they are invisible to FireCrasher's core
mechanism.

Rather than pretend to catch what it can't, FireCrasher surfaces the system's
own record of those deaths. On API 30+ it reads
`ActivityManager.getHistoricalProcessExitReasons` and exposes them through
`getHistoricalExitReasons`, `getLastAbnormalExit`, and the optional
`CrashListener.onPreviousProcessExit` callback fired at install time. This lets
a consuming app log native/ANR terminations to its crash reporter on the next
launch — completing the crash picture without claiming to have recovered from
what is genuinely unrecoverable in-process.

## Key constraints and trade-offs

- **minSdk 23.** Raised from 21 in 2.1.0 by the `androidx.activity` dependency,
  which brought `OnBackPressedDispatcher` so LEVEL_TWO works under Android's
  predictive-back model (API 36 no longer calls `Activity.onBackPressed()`).
  Plain framework activities still use the legacy call, so both paths are kept.
- **API-gated features degrade to no-ops.** Everything built on
  `ApplicationExitInfo` / `setProcessStateSummary` returns empty/null below
  API 30 rather than failing. The library must remain usable across the whole
  23–36 range.
- **Deliberate deprecations.** `overridePendingTransition` and framework
  `onBackPressed()` are deprecated on newer APIs but retained (suppressed) for
  minSdk 23 compatibility — the newer replacements don't exist that far back.
- **Locked API surface.** Kotlin explicit-API mode plus a committed ABI
  baseline (`firecrasher/api/firecrasher.api`, checked in CI) keep the public
  contract intentional — important for a library many apps depend on and that
  is distributed by checksum.
- **Reproducible artifacts.** Archive timestamps and file order are pinned
  because JitPack serves the library by checksum; identical sources must
  produce identical bytes.

## Distribution model

FireCrasher is consumed through **JitPack**: there is no separate publish
pipeline in this repo. JitPack builds from a git tag (`jitpack.yml` pins
JDK 21), and remaps the Maven coordinates configured in
`firecrasher/build.gradle` to `com.github.osama-raddad:FireCrasher:<tag>` at
build time. Cutting a release is therefore: land the change (with README
version, `VERSION_NAME`, and ABI baseline in sync), then tag.

## Where the pieces live

- `FireCrasher.kt` — the public façade and all policy (evaluate, recover,
  state persistence, exit-info queries).
- `FireLooper.kt` — the exception-surviving main loop.
- `CrashHandler.java` — bridges the JVM uncaught-exception hook and activity
  lifecycle into the current-activity / back-stack-depth signals `evaluate`
  needs.
- `CrashListener.kt` / `CrashLevel.kt` / `RecoveryState.kt` — the consumer
  callback, the escalation enum, and the cross-process state codec.

See [`firecrasher/AGENTS.md`](firecrasher/AGENTS.md) for the full source map
and the rules that govern changing each piece.
