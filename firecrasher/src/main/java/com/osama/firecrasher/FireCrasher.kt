package com.osama.firecrasher

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.util.Log
import com.osama.firecrasher.CrashHandler.getBackStackCount


object FireCrasher {
    var retryCount: Int = 0
    fun install(application: Application) {
        if (!FireLooper.isSafe) {
            val crashHandler = CrashHandler()
            application.registerActivityLifecycleCallbacks(crashHandler.lifecycleCallbacks)
            FireLooper.install()
            FireLooper.setUncaughtExceptionHandler(crashHandler)
            Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        }
    }

    fun install(application: Application, crashListener: CrashListener) {
        if (!FireLooper.isSafe) {
            val crashHandler = CrashHandler()
            crashHandler.setCrashListener(crashListener)
            application.registerActivityLifecycleCallbacks(crashHandler.lifecycleCallbacks)
            FireLooper.install()
            FireLooper.setUncaughtExceptionHandler(crashHandler)
            Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        }
    }

    fun install(application: Application, crashListener: CrashInterface) {
        if (!FireLooper.isSafe) {
            val crashHandler = CrashHandler()
            crashHandler.setCrashInterface(crashListener)
            application.registerActivityLifecycleCallbacks(crashHandler.lifecycleCallbacks)
            FireLooper.install()
            FireLooper.setUncaughtExceptionHandler(crashHandler)
            Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        }
    }

    fun recover(activity: Activity) {
        val intent = Intent(activity, activity.javaClass)
        activity.overridePendingTransition(0, 0)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        if (getBackStackCount(activity) >= 1) {
            //try to restart the failing activity
            if (retryCount <= 3) {
                retryCount += 1
                activity.startActivity(intent)
                activity.finish()
            } else {
                retryCount = 0
                //failure in restarting the activity try to go back
                activity.onBackPressed()
            }
        } else {
            if (retryCount <= 3) {
                retryCount += 1
                //try to restart the failing activity
                activity.startActivity(intent)
                activity.finish()
            } else {
                retryCount = 0
                //no activates to go back to so just restart the app
                restartApp(activity)
                activity.finish()
            }
        }
    }


    private fun restartApp(activity: Activity) {
        val i = activity.baseContext
                .packageManager
                .getLaunchIntentForPackage(activity.baseContext.packageName)
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            activity.startActivity(i)
        }
    }
}
