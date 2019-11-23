package com.osama.firecrasher

import android.app.Activity
import android.app.Application
import android.content.Intent
import com.osama.firecrasher.CrashHandler.getBackStackCount


object FireCrasher {

    var retryCount: Int = 0
        private set

    private val crashHandler: CrashHandler by lazy { CrashHandler() }

    fun install(application: Application, crashListener: CrashListener) {
        if (FireLooper.isSafe) return
        crashHandler.setCrashListener(crashListener)
        application.registerActivityLifecycleCallbacks(crashHandler.lifecycleCallbacks)
        FireLooper.install()
        FireLooper.setUncaughtExceptionHandler(crashHandler)
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
    }

    fun evaluate(): CrashLevel {
        return when {
            retryCount <= 1 ->
                //try to restart the failing activity
                CrashLevel.LEVEL_ONE
            getBackStackCount(crashHandler.activity) >= 1 ->
                //failure in restarting the activity try to go back
                CrashLevel.LEVEL_TWO
            else ->
                //no activates to go back to so just restart the app
                CrashLevel.LEVEL_THREE
        }
    }

    fun evaluateAsync(onEvaluate: ((activity: Activity?, level: CrashLevel) -> Unit)?) {
        when {
            retryCount <= 1 ->
                //try to restart the failing activity
                onEvaluate?.invoke(crashHandler.activity, CrashLevel.LEVEL_ONE)
            getBackStackCount(crashHandler.activity) >= 1 ->
                //failure in restarting the activity try to go back
                onEvaluate?.invoke(crashHandler.activity, CrashLevel.LEVEL_TWO)
            else ->
                //no activates to go back to so just restart the app
                onEvaluate?.invoke(crashHandler.activity, CrashLevel.LEVEL_THREE)
        }
    }

    fun recover(level: CrashLevel = evaluate(), onRecover: ((activity: Activity?) -> Unit)?) {
        val activityPair = getActivityPair()
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
        onRecover?.invoke(crashHandler.activity)
    }


    private fun getActivityPair(): Pair<Activity?, Intent?> {
        val activity = crashHandler.activity
        val intent = if (activity?.intent?.action == "android.intent.action.MAIN")
            Intent(activity, activity.javaClass)
        else
            activity?.intent

        intent?.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        return Pair(activity, intent)
    }

    private fun restartActivity(activityPair: Pair<Activity?, Intent?>) {
        if (retryCount == 0) {
            activityPair.first?.recreate()
        } else {
            activityPair.first?.startActivity(activityPair.second)
            activityPair.first?.overridePendingTransition(0, 0)
            activityPair.first?.finish()
            activityPair.first?.overridePendingTransition(0, 0)
        }
        retryCount += 1
    }

    private fun goBack(activityPair: Pair<Activity?, Intent?>) {
        activityPair.first?.onBackPressed()
    }

    private fun restartApp(activityPair: Pair<Activity?, Intent?>) {
        val packageName = activityPair.first?.baseContext?.packageName
        if (packageName != null) {
            val intent = activityPair.first?.baseContext?.packageManager
                    ?.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                activityPair.first?.startActivity(intent)
            }
            activityPair.first?.overridePendingTransition(0, 0)
            activityPair.first?.finish()
            activityPair.first?.overridePendingTransition(0, 0)
        }
    }
}
