# firecrasher/ — AGENTS.md

The **published library** module (`com.osama.firecrasher`, JitPack artifact
`com.github.osama-raddad:FireCrasher`). Read the root [`AGENTS.md`](../AGENTS.md)
first for the overall architecture; this file covers what is specific to
editing the library.

## Source map (`src/main/java/com/osama/firecrasher/`)

| File | Role |
|------|------|
| `FireCrasher.kt` | Public `FireCrasher.install` + `Application.installFireCrasher` extension; internal policy: `evaluate`, `recover`, `dispatchCrash`, `retryCount`, recovery-state persistence. |
| `FireCrasherConfig.kt` | The install DSL builder (`onCrash { }`, `onPreviousProcessExit { }`) and the `@FireCrasherDsl` marker. |
| `CrashScope.kt` | `onCrash` receiver: `throwable`, `activity`, `level`, `retryCount`, `recover(...)`. |
| `RecoveryLevel.kt` | The three recovery levels. Ordinals are persisted by the codec — append only, never reorder. |
| `ExitInfo.kt` | Public `Context.historicalExitReasons` / `Context.lastAbnormalExit` extensions. |
| `CrashHandler.kt` | Internal `UncaughtExceptionHandler` + `ActivityLifecycleCallbacks`. Tracks the current activity and live activity count (`backStackCount`). |
| `FireLooper.kt` | Reflection-based main loop that survives main-thread exceptions; `isSafe` guards double-install. |
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
  empty/null below that — see `Context.historicalExitReasons`/`lastAbnormalExit`.
- **Deliberate deprecations.** `overridePendingTransition` and framework
  `onBackPressed()` are kept with `@Suppress("DEPRECATION")` for minSdk 23. Do
  not remove them without preserving the legacy path.
- **Reproducible archives** (`preserveFileTimestamps = false`,
  `reproducibleFileOrder = true`) — JitPack consumes AARs by checksum. Keep it.
- **Version bumps** must keep `VERSION_NAME` (in `build.gradle`, currently
  `3.0.0`), the ABI baseline, and the README version in sync.

## Tests (`src/test/java/...`)

JVM unit tests run under **Robolectric** — no device needed.

```bash
./gradlew :firecrasher:test        # unit tests
./gradlew :firecrasher:apiCheck    # ABI baseline comparison
./gradlew :firecrasher:apiDump     # regenerate the ABI baseline
```

Suites: `RecoveryLevelEvaluationTest` (level selection), `BackStackCountTest`,
`CrashHandlerTest` (crash delivery), `FireCrasherDslTest` (scope population,
auto-recover default, forced-level recover), `GoBackDispatchTest` (dispatcher
vs. legacy back), `ExitInfoTest` (`@Config(sdk = [30, 36])`),
`RecoveryStateCodecTest`. Add a test alongside any behavior change.
