package com.osama.firecrasher;

import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by osama.
 */


public class SafeLooper  implements Runnable {
    private static final Object EXIT = new Object();
    private static final ThreadLocal<SafeLooper> RUNNINGS = new ThreadLocal<>();
    private static Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    private static Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Install SafeLooper in the main thread
     * <p>
     * Notice the action will take effect in the next event loop
     */
    public static void install() {
        handler.removeMessages(0, EXIT);
        handler.post(new SafeLooper());
    }

    /**
     * Exit SafeLooper after millis in the main thread
     * <p>
     * Notice the action will take effect in the next event loop
     */
    public static void uninstallDelay(long millis) {
        handler.removeMessages(0, EXIT);
        handler.sendMessageDelayed(handler.obtainMessage(0, EXIT), millis);
    }

    /**
     * Exit SafeLooper in the main thread
     * <p>
     * Notice the action will take effect in the next event loop
     */
    public static void uninstall() {
        uninstallDelay(0);
    }

    /**
     * Tell if the SafeLooper is running in the current thread
     */
    public static boolean isSafe() {
        return RUNNINGS.get() != null;
    }

    /**
     * The same as Thread.setDefaultUncaughtExceptionHandler
     */
    public static void setUncaughtExceptionHandler(
            Thread.UncaughtExceptionHandler h) {
        uncaughtExceptionHandler = h;
    }

    @Override
    public void run() {
        if (RUNNINGS.get() != null)
            return;

        Method next;
        Field target;
        try {
            Method m = MessageQueue.class.getDeclaredMethod("next");
            m.setAccessible(true);
            next = m;
            Field f = Message.class.getDeclaredField("target");
            f.setAccessible(true);
            target = f;
        } catch (Exception e) {
            return;
        }

        RUNNINGS.set(this);
        MessageQueue queue = Looper.myQueue();
        Binder.clearCallingIdentity();
        final long ident = Binder.clearCallingIdentity();

        while (true) {
            try {
                Message msg = (Message) next.invoke(queue);
                if (msg == null || msg.obj == EXIT)
                    break;

                Handler h = (Handler) target.get(msg);
                h.dispatchMessage(msg);
                final long newIdent = Binder.clearCallingIdentity();
                if (newIdent != ident) {
                }
                int currentapiVersion = android.os.Build.VERSION.SDK_INT;
                if (currentapiVersion < android.os.Build.VERSION_CODES.LOLLIPOP){
                    msg.recycle();
                }
            } catch (Exception e) {
                Thread.UncaughtExceptionHandler h = uncaughtExceptionHandler;
                Throwable ex = e;
                if (e instanceof InvocationTargetException) {
                    ex = e.getCause();
                    if (ex == null) {
                        ex = e;
                    }
                }
                // e.printStackTrace(System.err);
                if (h != null) {
                    h.uncaughtException(Thread.currentThread(), ex);
                }
                new Handler().post(this);
                break;
            }
        }

        RUNNINGS.set(null);
    }
}