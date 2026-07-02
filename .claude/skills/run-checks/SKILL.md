---
name: run-checks
description: Run the full local build that mirrors CI for FireCrasher — unit tests, Android lint, and the ABI baseline comparison. Use before pushing or opening a PR, or whenever asked to "run the tests", "check the build", or "verify it compiles".
---

# Run the CI-equivalent checks

CI (`.github/workflows/build.yml`) runs `./gradlew build --stacktrace` on
JDK 21. `build` depends on `check`, which runs the unit tests, Android lint
(`abortOnError = true`), and `:firecrasher:apiCheck` (the ABI baseline
comparison). Reproduce it locally before pushing.

## Steps

1. Confirm the toolchain is JDK 21 (`java -version`). AGP 9.x + the Java 21
   bytecode target require it.
2. Run the full build:
   ```bash
   ./gradlew build --stacktrace
   ```
3. For a faster inner loop while iterating, run the pieces directly:
   ```bash
   ./gradlew :firecrasher:test        # Robolectric unit tests
   ./gradlew :firecrasher:lint        # Android lint
   ./gradlew :firecrasher:apiCheck    # ABI baseline comparison
   ```

## If it fails

- **Test failures** — reports are under `firecrasher/build/reports/tests/`.
- **Lint failures** — reports under `firecrasher/build/reports/lint-results-*`.
  Lint is set to fail the build; fix the finding rather than lowering severity.
- **`apiCheck` failure** — the public API changed. If the change was
  intentional, use the `update-abi-baseline` skill; if not, it means you leaked
  something into the public surface — revisit the change.

Do not weaken `abortOnError`, delete tests, or edit the ABI baseline just to get
green. Fix the underlying cause.
