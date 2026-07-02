---
name: add-library-test
description: Add or update a Robolectric JVM unit test for the FireCrasher library. Use when changing recovery logic, crash handling, back-stack counting, exit-info reporting, or the recovery-state codec, and a test should cover it.
---

# Add a library unit test

The library's tests run on the JVM under **Robolectric** — no device or
emulator needed. They live in
`firecrasher/src/test/java/com/osama/firecrasher/` and use JUnit 4. Every
behavior change to the library should come with a test.

## Where behavior is tested today

| Concern | Test |
|---------|------|
| Recovery level selection (`evaluate`) | `CrashLevelEvaluationTest` (pure, no Robolectric) |
| Activity counting / back-stack depth | `BackStackCountTest` |
| Crash delivered to the listener on the main thread | `CrashHandlerTest` |
| LEVEL_TWO back dispatch (dispatcher vs. legacy `onBackPressed`) | `GoBackDispatchTest` |
| `ApplicationExitInfo` querying (API 30+) | `ExitInfoTest` (`@Config(sdk = [30, 36])`) |
| Recovery-state encode/decode + 128-byte limit | `RecoveryStateCodecTest` |

Extend the matching suite when your change fits one; add a new
`*Test.kt` in the same package otherwise.

## Conventions to follow

- **Runner:** annotate device-dependent tests with
  `@RunWith(RobolectricTestRunner::class)`. Pure-logic tests (like
  `CrashLevelEvaluationTest`) need no runner — prefer that when possible.
- **Build activities** with `Robolectric.buildActivity(...)`; drive lifecycle
  through `CrashHandler`'s `lifecycleCallbacks` rather than reaching into
  internals.
- **SDK-gated behavior:** pin the API levels under test with
  `@Config(sdk = [30, 36])` (see `ExitInfoTest`) so both the guarded and
  unguarded paths are exercised.
- **Reset global state:** `FireCrasher` is an `object` with process-wide state
  (`retryCount`). Tests that call into it must clean up in `@After` via
  `FireCrasher.resetForTest()` (see `GoBackDispatchTest`) so tests don't leak
  into each other.
- Name tests with backtick sentences describing the behavior, matching the
  existing style (e.g. `` `first crash restarts the activity` ``).

## Run

```bash
./gradlew :firecrasher:test
```

Test reports land in `firecrasher/build/reports/tests/`. Before finishing, run
the `run-checks` skill so lint and the ABI baseline are covered too.
