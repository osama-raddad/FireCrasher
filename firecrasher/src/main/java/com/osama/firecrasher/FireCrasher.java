package com.osama.firecrasher;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;


public class FireCrasher {
    public static void install(Application application) {
        if (!FireLooper.isSafe()) {
            CrashHandler crashHandler = new CrashHandler();
            application.registerActivityLifecycleCallbacks(crashHandler.getLifecycleCallbacks());
            FireLooper.install();
            FireLooper.setUncaughtExceptionHandler(crashHandler);
            Thread.setDefaultUncaughtExceptionHandler(crashHandler);
        }
    }

    public static void install(Application application, CrashListener crashListener) {
        if (!FireLooper.isSafe()) {
            CrashHandler crashHandler = new CrashHandler();
            crashHandler.setCrashListener(crashListener);
            application.registerActivityLifecycleCallbacks(crashHandler.getLifecycleCallbacks());
            FireLooper.install();
            FireLooper.setUncaughtExceptionHandler(crashHandler);
            Thread.setDefaultUncaughtExceptionHandler(crashHandler);
        }
    }

    public static void install(Application application, CrashInterface crashListener) {
        if (!FireLooper.isSafe()) {
            CrashHandler crashHandler = new CrashHandler();
            crashHandler.setCrashInterface(crashListener);
            application.registerActivityLifecycleCallbacks(crashHandler.getLifecycleCallbacks());
            FireLooper.install();
            FireLooper.setUncaughtExceptionHandler(crashHandler);
            Thread.setDefaultUncaughtExceptionHandler(crashHandler);
        }
    }

    static void recover(Activity activity) {
        if (activity != null) {
            try {
                ActivityInfo[] list = activity.getPackageManager()
                        .getPackageInfo(activity.getPackageName(), PackageManager.GET_ACTIVITIES).activities;
                if (list.length == 1 && list[0].name.equals(activity.getClass().getName())) {
                    try {
                        activity.startActivity(new Intent(activity, activity.getClass()));
                    } catch (Exception e) {
                        // e.printStackTrace();
                    }
                    activity.finish();
                } else {
                    activity.onBackPressed();
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("FireCrasher", e.getMessage());
            }
        }
    }
}
