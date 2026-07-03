---
name: customize-recovery
description: Inspect FireCrasher's recovery level and show custom recovery UX (loading dialog, toast, snackbar) or force a specific recovery level. Use when tailoring what the user sees during recovery, or reacting differently to occasional vs. repeated crashes.
---

# Customize the recovery experience

FireCrasher escalates recovery through three `RecoveryLevel`s:

- **RESTART_ACTIVITY** — restart the crashing activity (occasional crash).
- **GO_BACK** — the activity keeps crashing, so go back to the previous screen.
- **RELAUNCH_APP** — nothing to go back to, so restart the whole app.

You can read the level before recovering and show appropriate UX, or force a
level.

## React to the recovery level

Inside `onCrash`, the evaluated `level` and the current `activity` are already
in scope. Call `recover { … }`; its lambda runs after recovery is kicked off.

```kotlin
installFireCrasher {
    onCrash {
        report(throwable)   // see report-crashes skill

        val context = activity ?: return@onCrash
        when (level) {
            RecoveryLevel.RESTART_ACTIVITY ->
                // Quiet restart of the current screen — show a brief spinner.
                recover {
                    Toast.makeText(context, "Recovering…", Toast.LENGTH_SHORT).show()
                }
            RecoveryLevel.GO_BACK ->
                recover {
                    Toast.makeText(context, "Returning to the previous screen", Toast.LENGTH_SHORT).show()
                }
            RecoveryLevel.RELAUNCH_APP ->
                recover {
                    Toast.makeText(context, "Restarting the app", Toast.LENGTH_LONG).show()
                }
        }
    }
}
```

`retryCount` is also in scope if you want to distinguish the first attempt
from later ones (e.g. only show a dialog once):

```kotlin
if (retryCount <= 1 && level == RecoveryLevel.RESTART_ACTIVITY) {
    // show a loading dialog
}
```

## Force a specific level

If you know how you want to recover, pass the level explicitly instead of
using the evaluated one:

```kotlin
onCrash {
    recover(RecoveryLevel.RELAUNCH_APP)   // always restart the app on any crash
}
```

## Guidance

- Keep recovery UX **fast and non-blocking** — the user just hit a crash; a long
  modal makes it worse. A spinner or short toast is usually enough.
- Do the crash **reporting** before showing UX (see `report-crashes`).
- The recovery lambda runs on the main thread; don't do heavy work in it.
