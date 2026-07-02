# FireCrasher user skills

A collection of [Claude Code / agent skills](https://docs.claude.com) for
developers **using** FireCrasher in their own Android app. They are *not* used
to develop this library (those live in the repo's `.claude/skills/`) — they are
meant to be copied into a consuming project.

## Install into your app

Copy the skill folders into your project's skills directory:

```bash
cp -r docs/skills/{install-firecrasher,report-crashes,customize-recovery,detect-native-crashes-and-anrs} \
      /path/to/your-app/.claude/skills/
```

(Each skill is a folder containing a `SKILL.md`. Drop the ones you want into
`.claude/skills/` in your app; skip this README.)

## The skills

| Skill | Use it when you want to… |
|-------|--------------------------|
| `install-firecrasher` | Add the dependency and install the crash handler in your `Application`. |
| `report-crashes` | Forward caught crashes to Crashlytics/Sentry while recovering. |
| `customize-recovery` | Inspect the crash level and show your own recovery UX. |
| `detect-native-crashes-and-anrs` | Report native crashes / ANRs the in-process handler can't catch (API 30+). |

All snippets target the public API of FireCrasher **2.1.0** (minSdk 23). See the
project [`README.md`](../../README.md) for the full reference.
