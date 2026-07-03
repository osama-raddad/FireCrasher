package com.osama.firecrasher

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity

/**
 * Installs FireCrasher on this application. Fluent alias for
 * [FireCrasher.install]; call once, in [Application.onCreate]:
 *
 * ```
 * installFireCrasher {
 *     onCrash {
 *         report(throwable)
 *         recover()
 *     }
 * }
 * ```
 *
 * A bare `installFireCrasher()` recovers automatically at the evaluated level.
 */
public fun Application.installFireCrasher(configure: FireCrasherConfig.() -> Unit = {}) {
    FireCrasher.install(this, configure)
}

public object FireCrasher {

    /**
     * How long a recovery state stored with the previous process's exit record
     * stays authoritative. Older records mean the user relaunched the app
     * normally, not that a recovery restart is still in flight.
     */
    private const val RECOVERY_STATE_MAX_AGE_MS = 30_000L

    internal var retryCount: Int = 0
        private set

    private var application: Application? = null

    internal var onCrashHandler: CrashScope.() -> Unit = { recover() }

    internal val crashHandler: CrashHandler by lazy { CrashHandler() }

    /**
     * Hooks the main loop and activity lifecycle. Call once, in
     * [Application.onCreate] — or use the [installFireCrasher] extension.
     */
    public fun install(application: Application, configure: FireCrasherConfig.() -> Unit = {}) {
        if (FireLooper.isSafe) return
        val config = FireCrasherConfig().apply(configure)
        this.application = application
        this.onCrashHandler = config.onCrash
        crashHandler.onCrash = ::dispatchCrash
        application.registerActivityLifecycleCallbacks(crashHandler.lifecycleCallbacks)
        restorePreviousRecoveryState(application, config.onPreviousProcessExit)
        FireLooper.install()
        FireLooper.setUncaughtExceptionHandler(crashHandler)
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
    }

    internal fun dispatchCrash(throwable: Throwable) {
        val scope = CrashScope(
            throwable = throwable,
            activity = crashHandler.activity,
            level = evaluate(retryCount, crashHandler.backStackCount),
            retryCount = retryCount,
        )
        scope.onCrashHandler()
    }

    internal fun evaluate(retryCount: Int, backStackCount: Int): RecoveryLevel {
        return when {
            retryCount <= 1 ->
                //try to restart the failing activity
                RecoveryLevel.RESTART_ACTIVITY
            backStackCount >= 1 ->
                //failure in restarting the activity try to go back
                RecoveryLevel.GO_BACK
            else ->
                //no activates to go back to so just restart the app
                RecoveryLevel.RELAUNCH_APP
        }
    }

    internal fun recover(level: RecoveryLevel, onRecovered: (activity: Activity?) -> Unit) {
        val activityPair = getActivityPair()
        val retryCountAtCrash = retryCount
        when (level) {
            //try to restart the failing activity
            RecoveryLevel.RESTART_ACTIVITY -> {
                restartActivity(activityPair)
            }
            //failure in restarting the activity try to go back
            RecoveryLevel.GO_BACK -> {
                retryCount = 0
                goBack(activityPair)
            }
            //no activates to go back to so just restart the app
            RecoveryLevel.RELAUNCH_APP -> {
                retryCount = 0
                restartApp(activityPair)
            }
        }
        // Stash the escalated state (not the zeroed counter) with the process
        // exit record: if this recovery attempt kills the process, the next
        // launch must know how far recovery had already escalated.
        publishRecoveryState(
            level,
            if (level == RecoveryLevel.RESTART_ACTIVITY) retryCount else retryCountAtCrash,
        )
        onRecovered(crashHandler.activity)
    }

    internal fun restorePreviousRecoveryState(
        context: Context,
        onPreviousProcessExit: (ApplicationExitInfo) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val lastExit = context.lastAbnormalExit() ?: return
        val state = RecoveryStateCodec.decode(lastExit.processStateSummary)
        if (state != null && System.currentTimeMillis() - lastExit.timestamp < RECOVERY_STATE_MAX_AGE_MS) {
            // The previous process died while a recovery was in flight. Resume
            // the escalation ladder instead of retrying the level that just
            // failed: anything past RESTART_ACTIVITY must not fall back to
            // restarting the activity that killed the process.
            retryCount = if (state.level == RecoveryLevel.RESTART_ACTIVITY) state.retryCount
            else maxOf(state.retryCount, 2)
        }
        onPreviousProcessExit(lastExit)
    }

    private fun publishRecoveryState(level: RecoveryLevel, retryCount: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val context = application ?: crashHandler.activity ?: return
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.setProcessStateSummary(RecoveryStateCodec.encode(level, retryCount))
    }

    private fun getActivityPair(): Pair<Activity?, Intent?> {
        val activity: Activity? = crashHandler.activity
        val intent: Intent? = if (activity?.intent?.action == "android.intent.action.MAIN")
            Intent(activity, activity.javaClass)
        else
            activity?.intent

        intent?.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        return Pair(activity, intent)
    }

    // overridePendingTransition is deprecated on API 34+ (replaced by
    // overrideActivityTransition), but is kept here for minSdk 23 compatibility.
    @Suppress("DEPRECATION")
    private fun restartActivity(activityPair: Pair<Activity?, Intent?>) {
        val activity = activityPair.first ?: run {
            retryCount += 1
            return
        }

        when (retryCount) {
            0 -> activity.recreate()
            else -> {
                activity.startActivity(activityPair.second)
                activity.overridePendingTransition(0, 0)
                activity.finish()
                activity.overridePendingTransition(0, 0)
            }
        }

        retryCount += 1
    }

    internal fun goBack(activityPair: Pair<Activity?, Intent?>) {
        val activity = activityPair.first ?: return
        // The dispatcher is @MainThread; runOnUiThread executes inline when
        // already on the main thread (the normal crash path), so recovery
        // stays synchronous there.
        activity.runOnUiThread {
            if (activity is ComponentActivity) {
                activity.onBackPressedDispatcher.onBackPressed()
            } else {
                // Plain framework Activity: no dispatcher exists, so the
                // deprecated call remains the only way to pop the back stack.
                @Suppress("DEPRECATION")
                activity.onBackPressed()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun restartApp(activityPair: Pair<Activity?, Intent?>) {
        val activity = activityPair.first ?: return
        val packageName = activity.baseContext.packageName

        activity.baseContext.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            activity.startActivity(intent)
        }

        with(activity) {
            overridePendingTransition(0, 0)
            finish()
            overridePendingTransition(0, 0)
        }
    }

    internal fun resetForTest() {
        retryCount = 0
        application = null
        onCrashHandler = { recover() }
        crashHandler.resetForTest()
    }
}
