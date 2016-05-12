package com.osama.firecrasher;

import android.app.Application;

/**
 * Created by Osama Rsaddad.
 */
public class FireCrasher {
    public static void install(Application application){
        if (!SafeLooper.isSafe()) {
            CrashHandler crashHandler = new CrashHandler();
            application.registerActivityLifecycleCallbacks(crashHandler.getLifecycleCallbacks());
            SafeLooper.install();
            SafeLooper.setUncaughtExceptionHandler(crashHandler);
            Thread.setDefaultUncaughtExceptionHandler(crashHandler);
        }
    }

    public static void install(Application application,CrashListener crashListener){
        if (!SafeLooper.isSafe()) {
            CrashHandler crashHandler = new CrashHandler();
            crashHandler.setCrashListener(crashListener);
            application.registerActivityLifecycleCallbacks(crashHandler.getLifecycleCallbacks());
            SafeLooper.install();
            SafeLooper.setUncaughtExceptionHandler(crashHandler);
            Thread.setDefaultUncaughtExceptionHandler(crashHandler);
        }
    }


}
