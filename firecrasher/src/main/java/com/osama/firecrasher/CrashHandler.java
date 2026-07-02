package com.osama.firecrasher;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public final class CrashHandler implements Thread.UncaughtExceptionHandler {
    private Activity activity;
    private int activityCount;
    private Application.ActivityLifecycleCallbacks lifecycleCallbacks;
    private CrashListener crashListener;

    CrashHandler() {
        lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                CrashHandler.this.activity = activity;
                activityCount++;
            }

            @Override
            public void onActivityStarted(Activity activity) {
                CrashHandler.this.activity = activity;
            }

            @Override
            public void onActivityResumed(Activity activity) {
                CrashHandler.this.activity = activity;
            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (activityCount > 0) activityCount--;
                if (CrashHandler.this.activity == activity) {
                    CrashHandler.this.activity = null;
                }
            }
        };
    }

    public Activity getActivity() {
        return activity;
    }


    void setCrashListener(CrashListener crashListener) {
        this.crashListener = crashListener;
    }

    @Override
    public void uncaughtException(Thread thread, final Throwable throwable) {
        if (crashListener == null) return;
        if (activity != null) {
            activity.runOnUiThread(() -> crashListener.onCrash(throwable));
        } else {
            // Crash before any activity exists (e.g. during Application.onCreate
            // or from a background thread at startup): still deliver the callback
            // on the main thread instead of throwing an NPE inside the handler.
            new Handler(Looper.getMainLooper()).post(() -> crashListener.onCrash(throwable));
        }
    }

    Application.ActivityLifecycleCallbacks getLifecycleCallbacks() {
        return lifecycleCallbacks;
    }

    // Counts activities this process has created and not yet destroyed. Restart
    // cycles (recreate(), start-new-then-finish) are net zero, so the count
    // tracks the depth the user can navigate back through. Requires install()
    // to run before the first activity is created, i.e. in Application.onCreate.
    public int getBackStackCount() {
        return Math.max(0, activityCount - 1);
    }
}
