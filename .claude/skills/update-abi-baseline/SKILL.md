---
name: update-abi-baseline
description: Regenerate and review FireCrasher's committed public ABI baseline (firecrasher/api/firecrasher.api) after an intentional public API change. Use when apiCheck fails, or after adding/removing/changing any public or protected library declaration.
---

# Update the ABI baseline

The library's public binary API is frozen in `firecrasher/api/firecrasher.api`
by the binary-compatibility-validator plugin. `:firecrasher:apiCheck` (part of
`check`, and therefore CI) fails whenever the compiled public surface no longer
matches that file. The baseline exists so public API changes are always
deliberate and reviewed — never accidental.

## When to use

- `apiCheck` failed and the API change was **intentional**.
- You added, removed, renamed, or changed the signature/visibility of any
  `public`/`protected` declaration in `firecrasher/src/main`.

If the API change was *not* intentional, do not run this — you have leaked an
implementation detail into the public surface. Make the declaration `internal`
or `private` instead (the library is compiled with `explicitApi()`).

## Steps

1. Regenerate the baseline from the current sources:
   ```bash
   ./gradlew :firecrasher:apiDump
   ```
2. Review the diff — this is the point of the skill. Every line must be a change
   you meant to make:
   ```bash
   git diff firecrasher/api/firecrasher.api
   ```
3. Confirm the check now passes:
   ```bash
   ./gradlew :firecrasher:apiCheck
   ```
4. Commit `firecrasher.api` **in the same commit** as the API change it
   describes, so the baseline and the code never drift apart.

## Notes

- Removing or changing an existing public signature is a **breaking change** for
  consumers — call it out in the PR description and the README "What's new", and
  it should drive a version bump (see the `release` skill).
- The validator tasks are wired by hand in `firecrasher/build.gradle` (AGP's
  built-in Kotlin doesn't auto-register them); `apiDump`/`apiCheck` are the only
  entry points you need.
