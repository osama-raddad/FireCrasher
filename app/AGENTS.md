# app/ — AGENTS.md

The **demo application** (`com.osama.firecrasherdemo`). It is **not published**
— it exists to exercise FireCrasher recovery on a device/emulator. Read the
root [`AGENTS.md`](../AGENTS.md) for the library architecture.

## What's here

- `App.kt` — installs FireCrasher in `Application.onCreate` via
  `installFireCrasher { onCrash { … } }`, showing a loading dialog on
  RESTART_ACTIVITY and a choice dialog otherwise. This is the reference
  example of consumer usage.
- `MainActivity.java` / `Main2Activity.kt` — screens that deliberately throw
  (e.g. `Main2Activity` throws from a delayed main-thread post) so you can watch
  the recovery ladder run.
- `res/` — standard Android resources for the demo UI.

## Rules for changing this module

- It depends on the library via `implementation(project(':firecrasher'))` — use
  it to smoke-test API changes end to end.
- Same toolchain as the library: minSdk 23, compileSdk/targetSdk 36, JDK 21.
  Dependency versions come from the root version catalog
  (`gradle/libs.versions.toml`) — don't hardcode them.
- Deliberate crashes are the point; don't "fix" them. If you change public
  library API, update the usage here to match so the demo keeps compiling.

## Run

```bash
./gradlew :app:assembleDebug       # build the demo APK
./gradlew :app:installDebug        # install on a connected device/emulator
```
