---
name: release
description: Cut a new FireCrasher library release — bump the version, sync the README and ABI baseline, verify, and tag for JitPack. Use when asked to "release", "publish", "cut a version", or "bump to x.y.z".
---

# Release FireCrasher

FireCrasher is distributed through **JitPack**, which builds from a git tag —
there is no separate publish pipeline in this repo. A release is: get the
version and metadata in sync, verify, commit, and tag. JitPack does the rest
(`jitpack.yml` pins JDK 21; coordinates are remapped to
`com.github.osama-raddad:FireCrasher:<tag>`).

The current version is **3.0.0**. Pick the new version with semantic versioning:
a breaking public API change (anything that alters `firecrasher/api/firecrasher.api`
incompatibly) is a **major** bump.

## Steps

1. **Land the code first.** All feature/fix work for the release should already
   be merged or staged. Run the `run-checks` skill and confirm green.

2. **Bump `VERSION_NAME`** in `firecrasher/build.gradle` (the
   `version = project.findProperty('VERSION_NAME') ?: '3.0.0'` fallback in the
   `publishing` block). Set it to the new version.

3. **Update `README.md`:**
   - The install snippet: `implementation 'com.github.osama-raddad:FireCrasher:<new>'`.
   - Add a **"What's new in <new>"** section describing the changes (breaking
     changes first, and call out any minSdk/toolchain change).

4. **Sync the ABI baseline.** If the public API changed, run the
   `update-abi-baseline` skill so `firecrasher/api/firecrasher.api` matches.
   `apiCheck` must pass.

5. **Verify** the whole thing once more:
   ```bash
   ./gradlew build --stacktrace
   ```

6. **Commit** the version bump, README, and any baseline change together, e.g.
   `Release 2.2.0`.

7. **Tag and push** (the tag is what JitPack builds):
   ```bash
   git tag <new>          # e.g. git tag 2.2.0 — match the README's coordinate
   git push origin <new>
   ```
   Tag names become the JitPack version verbatim, so the tag must equal the
   version string in the README install snippet.

8. **Confirm the JitPack build** at `https://jitpack.io/#osama-raddad/FireCrasher`
   resolves the new version (first fetch triggers the build).

## Notes

- The demo `app/` module has its own `versionCode`/`versionName`; it is **not**
  published and does not need to track the library version.
- Never edit `firecrasher.api` by hand to force a release — regenerate it with
  `apiDump`.
- Don't push tags to a feature branch's flow; releases are cut from the
  release-ready commit on the mainline per the repo's branching rules.
