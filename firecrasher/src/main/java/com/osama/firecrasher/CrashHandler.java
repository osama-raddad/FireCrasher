package com.osama.firecrasher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

public final class CrashHandler implements Thread.UncaughtExceptionHandler {
    private Activity activity;
    private Application.ActivityLifecycleCallbacks lifecycleCallbacks;
    private CrashListener crashListener;

    CrashHandler() {
        lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                CrashHandler.this.activity = activity;
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
        activity.runOnUiThread(() -> {
            if (crashListener != null) {
                crashListener.onCrash(throwable);
            }
        });
        Log.e("FireCrasher.err", thread.getName(), throwable);
    }

    Application.ActivityLifecycleCallbacks getLifecycleCallbacks() {
        return lifecycleCallbacks;
    }

    public int getBackStackCount() {
        if (activity == null) return 0;
        ActivityManager m = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfoList = m.getRunningTasks(10);
        int numOfActivities = 0;
        if (runningTaskInfoList.size() >= 1)
            numOfActivities = runningTaskInfoList.get(0).numActivities;
        return numOfActivities <= 0 ? 0 : numOfActivities - 1;
    }
}
