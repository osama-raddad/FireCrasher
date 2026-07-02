package com.osama.firecrasher

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity


public object FireCrasher {

    /**
     * How long a recovery state stored with the previous process's exit record
     * stays authoritative. Older records mean the user relaunched the app
     * normally, not that a recovery restart is still in flight.
     */
    private const val RECOVERY_STATE_MAX_AGE_MS = 30_000L

    public var retryCount: Int = 0
        private set

    private var application: Application? = null

    private var config: FireCrasherConfig = FireCrasherConfig.DEFAULT

    private val crashHandler: CrashHandler by lazy { CrashHandler() }

    public fun install(application: Application, crashListener: CrashListener) {
        install(application, FireCrasherConfig.DEFAULT, crashListener)
    }

    /**
     * Install with a lambda instead of a [CrashListener] subclass. The lambda
     * receives a [CrashEvent] carrying the throwable plus the crash context
     * (thread, activity, retry count, suggested level) and starts recovery via
     * [CrashEvent.recover]. Use the [CrashListener] overloads when you also
     * need [CrashListener.onPreviousProcessExit].
     */
    public fun install(
        application: Application,
        config: FireCrasherConfig = FireCrasherConfig.DEFAULT,
        onCrash: (CrashEvent) -> Unit,
    ) {
        install(application, config, asListener(onCrash))
    }

    public fun install(
        application: Application,
        config: FireCrasherConfig,
        crashListener: CrashListener,
    ) {
        install(application, config, crashListener, hookMainLooper = true)
    }

    // hookMainLooper=false is a test seam: FireLooper's replacement loop blocks
    // in MessageQueue.next() by design (it replaces Looper.loop()), which under
    // Robolectric would park the sandbox main thread forever.
    internal fun install(
        application: Application,
        config: FireCrasherConfig,
        crashListener: CrashListener,
        hookMainLooper: Boolean,
    ) {
        if (FireLooper.isSafe) return
        this.application = application
        this.config = config
        crashHandler.setCrashListener(crashListener)
        crashHandler.setConfig(config)
        // Capture whatever handler was installed before us (a crash reporter's,
        // or the framework's) so non-recovered crashes still reach it instead
        // of being silently clobbered. Guard against a repeated install
        // capturing FireCrasher itself.
        val currentDefault = Thread.getDefaultUncaughtExceptionHandler()
        if (currentDefault !== crashHandler) {
            crashHandler.setPreviousDefaultHandler(currentDefault)
        }
        application.registerActivityLifecycleCallbacks(crashHandler.lifecycleCallbacks)
        restorePreviousRecoveryState(application, crashListener)
        if (hookMainLooper) FireLooper.install()
        FireLooper.setUncaughtExceptionHandler(crashHandler)
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
    }

    internal fun asListener(onCrash: (CrashEvent) -> Unit): CrashListener =
        object : CrashListener() {
            override fun onCrash(throwable: Throwable) {
                onCrash(buildCrashEvent(throwable))
            }
        }

    /**
     * Undo [install]: restore the uncaught-exception handler that was the
     * process default before FireCrasher, stop the exception-surviving main
     * loop at its next message, and stop tracking activities. Subsequent
     * crashes behave as if FireCrasher was never installed. Safe to call when
     * not installed; [install] may be called again afterwards.
     *
     * Do not call from inside [CrashListener.onCrash] — the crash being
     * handled still needs the recovery machinery.
     */
    public fun uninstall() {
        application?.unregisterActivityLifecycleCallbacks(crashHandler.lifecycleCallbacks)
        if (Thread.getDefaultUncaughtExceptionHandler() === crashHandler) {
            Thread.setDefaultUncaughtExceptionHandler(crashHandler.previousDefaultHandler)
        }
        FireLooper.uninstall()
        retryCount = 0
        config = FireCrasherConfig.DEFAULT
        application = null
        crashHandler.reset()
    }

    public fun evaluate(): CrashLevel =
        evaluate(retryCount, currentBackStackDepth(), config.levelOneRetries)

    internal fun evaluate(
        retryCount: Int,
        backStackCount: Int,
        levelOneRetries: Int = FireCrasherConfig.DEFAULT.levelOneRetries,
    ): CrashLevel {
        return when {
            retryCount < levelOneRetries ->
                //try to restart the failing activity
                CrashLevel.LEVEL_ONE
            backStackCount >= 1 ->
                //failure in restarting the activity try to go back
                CrashLevel.LEVEL_TWO
            else ->
                //no activates to go back to so just restart the app
                CrashLevel.LEVEL_THREE
        }
    }

    private fun currentBackStackDepth(): Int =
        config.backStackDepthProvider?.getDepth() ?: crashHandler.backStackCount

    internal fun buildCrashEvent(throwable: Throwable): CrashEvent = CrashEvent(
        throwable = throwable,
        thread = crashHandler.crashedThread ?: Thread.currentThread(),
        activity = crashHandler.activity,
        retryCount = retryCount,
        suggestedLevel = evaluate(),
    )

    public fun evaluateAsync(onEvaluate: ((activity: Activity?, level: CrashLevel) -> Unit)?) {
        onEvaluate?.invoke(crashHandler.activity, evaluate())
    }

    public fun recover(level: CrashLevel = evaluate(), onRecover: ((activity: Activity?) -> Unit)? = null) {
        val activityPair = getActivityPair()
        val retryCountAtCrash = retryCount
        when (level) {
            //try to restart the failing activity
            CrashLevel.LEVEL_ONE -> {
                restartActivity(activityPair)
            }
            //failure in restarting the activity try to go back
            CrashLevel.LEVEL_TWO -> {
                retryCount = 0
                goBack(activityPair)
            }
            //no activates to go back to so just restart the app
            CrashLevel.LEVEL_THREE -> {
                retryCount = 0
                restartApp(activityPair)
            }
        }
        // Stash the escalated state (not the zeroed counter) with the process
        // exit record: if this recovery attempt kills the process, the next
        // launch must know how far recovery had already escalated.
        publishRecoveryState(
            level,
            if (level == CrashLevel.LEVEL_ONE) retryCount else retryCountAtCrash,
        )
        onRecover?.invoke(crashHandler.activity)
    }

    /**
     * Past terminations of this app recorded by the system, newest first.
     * Includes causes the in-process handler can never see, such as native
     * crashes, ANRs, and low-memory kills. Empty below API 30.
     */
    @JvmStatic
    @JvmOverloads
    public fun getHistoricalExitReasons(context: Context, maxCount: Int = 16): List<ApplicationExitInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.getHistoricalProcessExitReasons(context.packageName, 0, maxCount)
    }

    /**
     * The most recent recorded exit caused by a crash, native crash, or ANR,
     * or null if none is recorded. The record may predate the previous launch;
     * use [ApplicationExitInfo.getTimestamp] to judge freshness. Null below API 30.
     */
    @JvmStatic
    public fun getLastAbnormalExit(context: Context): ApplicationExitInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return getHistoricalExitReasons(context).firstOrNull {
            it.reason == ApplicationExitInfo.REASON_CRASH ||
                    it.reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
                    it.reason == ApplicationExitInfo.REASON_ANR
        }
    }

    internal fun restorePreviousRecoveryState(context: Context, crashListener: CrashListener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val lastExit = getLastAbnormalExit(context) ?: return
        val state = RecoveryStateCodec.decode(lastExit.processStateSummary)
        if (state != null && System.currentTimeMillis() - lastExit.timestamp < RECOVERY_STATE_MAX_AGE_MS) {
            // The previous process died while a recovery was in flight. Resume
            // the escalation ladder instead of retrying the level that just
            // failed: anything past LEVEL_ONE must not fall back to restarting
            // the activity that killed the process.
            retryCount = if (state.level == CrashLevel.LEVEL_ONE) state.retryCount
            else maxOf(state.retryCount, 2)
        }
        crashListener.onPreviousProcessExit(lastExit)
    }

    private fun publishRecoveryState(level: CrashLevel, retryCount: Int) {
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
        application?.unregisterActivityLifecycleCallbacks(crashHandler.lifecycleCallbacks)
        retryCount = 0
        application = null
        config = FireCrasherConfig.DEFAULT
        crashHandler.reset()
    }
}
