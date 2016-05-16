package com.osama.firecrasher;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

/**
 * Created by Osama Raddad.
 */
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

    protected static void recover(Activity activity) {
        if (activity != null)
            try {
                ActivityInfo[] list = activity
                        .getPackageManager()
                        .getPackageInfo(activity.getPackageName(), PackageManager.GET_ACTIVITIES)
                        .activities;
                if (list.length == 1 &&
                        list[0].name.equals(activity.getClass().getName())) {
                    activity.finish();
                    activity.startActivity(new Intent(activity, activity.getClass()));
                } else {
                    activity.onBackPressed();
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
    }


}