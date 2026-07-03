package com.osama.firecrasher

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper

internal class CrashHandler : Thread.UncaughtExceptionHandler {

    internal var onCrash: ((Throwable) -> Unit)? = null

    internal var activity: Activity? = null
        private set

    private var activityCount = 0

    internal val lifecycleCallbacks: Application.ActivityLifecycleCallbacks =
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                this@CrashHandler.activity = activity
                activityCount++
            }

            override fun onActivityStarted(activity: Activity) {
                this@CrashHandler.activity = activity
            }

            override fun onActivityResumed(activity: Activity) {
                this@CrashHandler.activity = activity
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (activityCount > 0) activityCount--
                if (this@CrashHandler.activity === activity) {
                    this@CrashHandler.activity = null
                }
            }
        }

    // Counts activities this process has created and not yet destroyed. Restart
    // cycles (recreate(), start-new-then-finish) are net zero, so the count
    // tracks the depth the user can navigate back through. Requires install()
    // to run before the first activity is created, i.e. in Application.onCreate.
    internal val backStackCount: Int
        get() = maxOf(0, activityCount - 1)

    internal fun resetForTest() {
        onCrash = null
        activity = null
        activityCount = 0
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val onCrash = onCrash ?: return
        val activity = activity
        if (activity != null) {
            activity.runOnUiThread { onCrash(throwable) }
        } else {
            // Crash before any activity exists (e.g. during Application.onCreate
            // or from a background thread at startup): still deliver the callback
            // on the main thread instead of throwing an NPE inside the handler.
            Handler(Looper.getMainLooper()).post { onCrash(throwable) }
        }
    }
}
