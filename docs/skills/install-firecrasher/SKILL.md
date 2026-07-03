---
name: install-firecrasher
description: Add the FireCrasher crash-recovery library to an Android app and install it in the Application class. Use when integrating FireCrasher, setting up crash recovery, or when "app crashes should recover instead of closing".
---

# Install FireCrasher

FireCrasher catches uncaught main-thread exceptions and recovers the app
(restart the activity → go back → restart the app) instead of letting it die.
It is distributed via JitPack.

## Requirements

- **minSdk 23** or higher.
- A toolchain that accepts Java 21 bytecode / Kotlin 2.1+ metadata (AGP 8.2+).

## 1. Add the JitPack repository

In `settings.gradle` (or the root `build.gradle` for older setups):

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

## 2. Add the dependency

In your app module's `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.osama-raddad:FireCrasher:3.0.0'
}
```

## 3. Install in your Application class

Installation **must** happen in `Application.onCreate`, before any activity is
created — FireCrasher hooks the main looper and activity lifecycle there. A
bare install already recovers automatically at the evaluated level:

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        installFireCrasher()
    }
}
```

To take over crash handling, configure `onCrash` (see the report-crashes and
customize-recovery skills for reporting and custom UX):

```kotlin
installFireCrasher {
    onCrash {
        // `throwable`, `activity`, `level`, `retryCount` are in scope.
        recover()
    }
}
```

Register the Application in your manifest if it isn't already:

```xml
<application android:name=".App" ... />
```

## Verify

Build and run, then throw from a screen (e.g. a button handler) to confirm the
app recovers instead of closing:

```kotlin
button.setOnClickListener { throw RuntimeException("boom") }
```

## Next

- `report-crashes` — forward the crash to your crash reporter.
- `customize-recovery` — show a loading/recovery UI based on the recovery level.
- `detect-native-crashes-and-anrs` — capture crashes the handler can't see.
