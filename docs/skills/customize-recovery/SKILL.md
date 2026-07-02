---
name: customize-recovery
description: Inspect FireCrasher's crash level and show custom recovery UX (loading dialog, toast, snackbar) or force a specific recovery level. Use when tailoring what the user sees during recovery, or reacting differently to occasional vs. repeated crashes.
---

# Customize the recovery experience

FireCrasher escalates recovery through three `CrashLevel`s:

- **LEVEL_ONE** — restart the crashing activity (occasional crash).
- **LEVEL_TWO** — the activity keeps crashing, so go back to the previous screen.
- **LEVEL_THREE** — nothing to go back to, so restart the whole app.

You can read the level before recovering and show appropriate UX, or force a
level.

## React to the crash level

Inside `onCrash`, `evaluate { activity, level -> … }` hands you the current
activity and the level FireCrasher would use. Call `recover { … }` from there;
its lambda runs after recovery is kicked off.

```kotlin
FireCrasher.install(this, object : CrashListener() {
    override fun onCrash(throwable: Throwable) {
        report(throwable)   // see report-crashes skill

        evaluate { activity, level ->
            val context = activity ?: return@evaluate
            when (level) {
                CrashLevel.LEVEL_ONE ->
                    // Quiet restart of the current screen — show a brief spinner.
                    recover {
                        Toast.makeText(context, "Recovering…", Toast.LENGTH_SHORT).show()
                    }
                CrashLevel.LEVEL_TWO ->
                    recover {
                        Toast.makeText(context, "Returning to the previous screen", Toast.LENGTH_SHORT).show()
                    }
                CrashLevel.LEVEL_THREE ->
                    recover {
                        Toast.makeText(context, "Restarting the app", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }
})
```

`FireCrasher.retryCount` is also readable if you want to distinguish the first
attempt from later ones (e.g. only show a dialog once):

```kotlin
if (FireCrasher.retryCount <= 1 && level == CrashLevel.LEVEL_ONE) {
    // show a loading dialog
}
```

## Force a specific level

If you know how you want to recover, pass the level explicitly instead of
letting `evaluate()` decide:

```kotlin
override fun onCrash(throwable: Throwable) {
    recover(CrashLevel.LEVEL_THREE)   // always restart the app on any crash
}
```

## Guidance

- Keep recovery UX **fast and non-blocking** — the user just hit a crash; a long
  modal makes it worse. A spinner or short toast is usually enough.
- Do the crash **reporting** before showing UX (see `report-crashes`).
- The recovery lambda runs on the main thread; don't do heavy work in it.
