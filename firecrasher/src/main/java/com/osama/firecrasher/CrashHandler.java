package com.osama.firecrasher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * Created by Osama Rsaddad.
 */
public final class CrashHandler implements Thread.UncaughtExceptionHandler {
    private Activity activity;
    private Application.ActivityLifecycleCallbacks lifecycleCallbacks;
    CrashListener crashListener;

    public CrashHandler() {
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

    public void setCrashListener(CrashListener crashListener) {
        this.crashListener = crashListener;
    }

    @Override
    public void uncaughtException(Thread thread, final Throwable throwable) {
        StringWriter exception = new StringWriter();
        throwable.printStackTrace(new PrintWriter(exception));
        activity.runOnUiThread(new Runnable() {
            public void run() {
                if (crashListener != null) {
                    crashListener.onCrash(throwable, activity);
                    crashListener.recover(new CrashListener.RecoverListener() {
                        @Override
                        public void OnRecover() {
                            recover();
                        }
                    });
                } else {
                    AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
                    alertDialog.setTitle("Crash");
                    alertDialog.setMessage(throwable.getMessage());
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Recover",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    recover();
                                }
                            });
                    alertDialog.show();
                }
            }
        });
        Log.e("error", thread.getName(), throwable);
    }

    public void recover() {
        if (activity != null)
            try {
                ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> taskList = manager.getRunningTasks(10);
                if (taskList.get(0).numActivities == 1 &&
                        taskList.get(0).topActivity.getClassName().equals(activity.getClass().getName())) {
                    activity.finish();
                    activity.startActivity(new Intent(activity, activity.getClass()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public Application.ActivityLifecycleCallbacks getLifecycleCallbacks() {
        return lifecycleCallbacks;
    }
}
