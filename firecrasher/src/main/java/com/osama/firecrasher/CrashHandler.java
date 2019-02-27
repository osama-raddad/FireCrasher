package com.osama.firecrasher;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.List;

public final class CrashHandler implements Thread.UncaughtExceptionHandler {
    private Activity activity;
    private Application.ActivityLifecycleCallbacks lifecycleCallbacks;
    private CrashListener crashListener;
    private CrashInterface crashInterface;

    CrashHandler() {
        lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                CrashHandler.this.activity = activity;
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {

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

    void setCrashListener(CrashListener crashListener) {
        this.crashListener = crashListener;
    }

    void setCrashInterface(CrashInterface crashListener) {
        this.crashInterface = crashListener;
    }

    @Override
    public void uncaughtException(Thread thread, final Throwable throwable) {
        activity.runOnUiThread(() -> {
            if (crashListener != null) {
                crashListener.onCrash(throwable, activity);
            } else if (crashInterface != null) {
                crashInterface.onCrash(throwable, activity);
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
                alertDialog.setTitle("Crash");
                alertDialog.setMessage(throwable.getMessage());
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Recover",
                        (dialog, which) -> {
                            dialog.dismiss();
                            FireCrasher.INSTANCE.recover(activity);
                        });
                alertDialog.show();
            }
        });
        Log.e("FireCrasher.err", thread.getName(), throwable);
    }

    Application.ActivityLifecycleCallbacks getLifecycleCallbacks() {
        return lifecycleCallbacks;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static Activity getRunningActivity() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread")
                    .invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            ArrayMap activities = (ArrayMap) activitiesField.get(activityThread);
            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    return (Activity) activityField.get(activityRecord);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        throw new RuntimeException("Didn't find the running activity");
    }

    public static int getBackStackCount(Activity activity) {
        ActivityManager m = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfoList = m.getRunningTasks(10);
        int numOfActivities = 0;
        for (ActivityManager.RunningTaskInfo runningTaskInfo : runningTaskInfoList) {
            int id = runningTaskInfo.id;
            CharSequence desc = runningTaskInfo.description;
            numOfActivities = runningTaskInfo.numActivities;
            String topActivity = runningTaskInfo.topActivity.getShortClassName();
        }
        return numOfActivities <= 0 ? 0 : numOfActivities - 1;
    }
}
