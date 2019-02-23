package com.osama.firecrasher

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.Log


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
            try {
                val list = activity.packageManager
                        .getPackageInfo(activity.packageName, PackageManager.GET_ACTIVITIES).activities
                if (list.size == 1 && list[0].name == activity.javaClass.name) {
                    try {
                        activity.startActivity(Intent(activity, activity.javaClass))
                    } catch (e: Exception) {
                        // e.printStackTrace();
                    }

                    activity.finish()
                } else {
                    activity.onBackPressed()
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("FireCrasher", e.message)
            }

        }
    }
}
