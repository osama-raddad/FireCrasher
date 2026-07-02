# firecrasher/ — AGENTS.md

The **published library** module (`com.osama.firecrasher`, JitPack artifact
`com.github.osama-raddad:FireCrasher`). Read the root [`AGENTS.md`](../AGENTS.md)
first for the overall architecture; this file covers what is specific to
editing the library.

## Source map (`src/main/java/com/osama/firecrasher/`)

| File | Role |
|------|------|
| `FireCrasher.kt` | Public API `object`: `install`, `evaluate`, `recover`, and the `ApplicationExitInfo` helpers. Owns `retryCount` and recovery-state persistence. |
| `CrashHandler.java` | `UncaughtExceptionHandler` + `ActivityLifecycleCallbacks`. Tracks the current activity and live activity count (`getBackStackCount`). Java, not Kotlin. |
| `FireLooper.kt` | Reflection-based main loop that survives main-thread exceptions; `isSafe` guards double-install. |
| `CrashListener.kt` | Abstract consumer callback (`onCrash`, optional `onPreviousProcessExit`). |
| `CrashLevel.kt` | The three recovery levels. |
| `RecoveryState.kt` | `RecoveryStateCodec` — encodes recovery progress into a ≤128-byte blob for `setProcessStateSummary` (API 30+). |

## Rules for changing this module

- **Explicit API mode** (`explicitApi()`): every public/internal declaration
  needs explicit visibility and return type. Keep internals `internal`/`private`.
- **ABI is frozen** in `api/firecrasher.api`. Any public API change fails
  `apiCheck` in CI. After an intentional change, run
  `./gradlew :firecrasher:apiDump` and commit the updated `firecrasher.api` in
  the same change.
- **minSdk 23.** Guard API 30+ features (`ApplicationExitInfo`,
  `setProcessStateSummary`) behind `Build.VERSION.SDK_INT` checks and degrade to
  empty/null below that — see `getHistoricalExitReasons`/`getLastAbnormalExit`.
- **Deliberate deprecations.** `overridePendingTransition` and framework
  `onBackPressed()` are kept with `@Suppress("DEPRECATION")` for minSdk 23. Do
  not remove them without preserving the legacy path.
- **Reproducible archives** (`preserveFileTimestamps = false`,
  `reproducibleFileOrder = true`) — JitPack consumes AARs by checksum. Keep it.
- **Version bumps** must keep `VERSION_NAME` (in `build.gradle`, currently
  `2.2.0`), the ABI baseline, and the README version in sync.

## Tests (`src/test/java/...`)

JVM unit tests run under **Robolectric** — no device needed.

```bash
./gradlew :firecrasher:test        # unit tests
./gradlew :firecrasher:apiCheck    # ABI baseline comparison
./gradlew :firecrasher:apiDump     # regenerate the ABI baseline
```

Suites: `CrashLevelEvaluationTest` (level selection), `BackStackCountTest`,
`CrashHandlerTest` (crash delivery), `GoBackDispatchTest` (dispatcher vs.
legacy back), `ExitInfoTest` (`@Config(sdk = [30, 36])`),
`RecoveryStateCodecTest`. Add a test alongside any behavior change.
