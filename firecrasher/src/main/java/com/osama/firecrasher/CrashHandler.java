package com.osama.firecrasher;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.util.ArrayDeque;

public final class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "FireCrasher";

    /** Clock abstraction so crash-loop tests can control time (java.util.function needs API 24+). */
    interface Clock {
        long now();
    }

    /** How the process dies when recovery is declined and no previous handler exists. */
    interface ProcessTerminator {
        void terminate(Throwable throwable);
    }

    private Activity activity;
    private int activityCount;
    private Application.ActivityLifecycleCallbacks lifecycleCallbacks;
    private CrashListener crashListener;
    private Thread crashedThread;
    private FireCrasherConfig config = FireCrasherConfig.DEFAULT;
    private Thread.UncaughtExceptionHandler previousDefaultHandler;
    private final ArrayDeque<Long> crashTimestamps = new ArrayDeque<>();

    Clock clock = System::currentTimeMillis;

    // Mirrors the framework's KillApplicationHandler; injectable so Robolectric
    // tests exercising the die-path don't kill the test JVM.
    ProcessTerminator processTerminator = throwable -> {
        Log.e(TAG, "FATAL EXCEPTION (recovery declined)", throwable);
        Process.killProcess(Process.myPid());
        System.exit(10);
    };

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

    void setConfig(FireCrasherConfig config) {
        this.config = config;
    }

    void setPreviousDefaultHandler(Thread.UncaughtExceptionHandler handler) {
        this.previousDefaultHandler = handler;
    }

    Thread.UncaughtExceptionHandler getPreviousDefaultHandler() {
        return previousDefaultHandler;
    }

    Thread getCrashedThread() {
        return crashedThread;
    }

    void reset() {
        activity = null;
        activityCount = 0;
        crashListener = null;
        crashedThread = null;
        config = FireCrasherConfig.DEFAULT;
        previousDefaultHandler = null;
        crashTimestamps.clear();
        clock = System::currentTimeMillis;
    }

    @Override
    public void uncaughtException(Thread thread, final Throwable throwable) {
        if (crashListener == null) return;
        crashedThread = thread;
        if (!shouldRecover(throwable) || isCrashLooping()) {
            dieNormally(thread, throwable);
            return;
        }
        if (activity != null) {
            activity.runOnUiThread(() -> crashListener.onCrash(throwable));
        } else {
            // Crash before any activity exists (e.g. during Application.onCreate
            // or from a background thread at startup): still deliver the callback
            // on the main thread instead of throwing an NPE inside the handler.
            new Handler(Looper.getMainLooper()).post(() -> crashListener.onCrash(throwable));
        }
    }

    private boolean shouldRecover(Throwable throwable) {
        try {
            return config.getShouldRecover().shouldRecover(throwable);
        } catch (Throwable predicateFailure) {
            // A broken predicate must not turn every crash fatal; fall back to
            // the default of recovering.
            Log.w(TAG, "shouldRecover predicate threw; recovering anyway", predicateFailure);
            return true;
        }
    }

    // Sliding-window crash-loop breaker: once `crashLoopThreshold` crashes have
    // been caught within `crashLoopWindowMillis`, recovery has demonstrably
    // stopped working and the app should die as it would without FireCrasher.
    private boolean isCrashLooping() {
        int threshold = config.getCrashLoopThreshold();
        if (threshold <= 0) return false;
        long now = clock.now();
        crashTimestamps.addLast(now);
        long windowStart = now - config.getCrashLoopWindowMillis();
        while (!crashTimestamps.isEmpty() && crashTimestamps.peekFirst() < windowStart) {
            crashTimestamps.removeFirst();
        }
        return crashTimestamps.size() >= threshold;
    }

    // The crash proceeds as if FireCrasher were not installed: the handler that
    // was the process default before install() (typically a crash reporter's,
    // which records the fatal and then kills the process) gets the exception;
    // without one, kill the process the way the framework would.
    //
    // On the main thread this runs inside FireLooper's catch, which reposts the
    // loop afterwards — harmless while the process is dying.
    private void dieNormally(Thread thread, Throwable throwable) {
        if (previousDefaultHandler != null && previousDefaultHandler != this) {
            previousDefaultHandler.uncaughtException(thread, throwable);
        } else {
            processTerminator.terminate(throwable);
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
