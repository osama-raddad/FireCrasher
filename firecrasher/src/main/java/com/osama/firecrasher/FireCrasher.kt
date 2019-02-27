package com.osama.firecrasher

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.Log
import com.osama.firecrasher.CrashHandler.getBackStackCount
import com.osama.firecrasher.CrashHandler.getRunningActivity
import java.lang.Exception


object FireCrasher {
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

    fun recover(activity: Activity?) {
        if (activity != null) {
            if (getBackStackCount(activity) >= 1) {
                //try to restart the failing activity
                try {
                    activity.startActivity(Intent(activity, activity.javaClass))
                    activity.finish()
                } catch (e: Exception) {
                    //failure in restarting the activity try to go back
                    activity.onBackPressed()
                }
            } else {
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
