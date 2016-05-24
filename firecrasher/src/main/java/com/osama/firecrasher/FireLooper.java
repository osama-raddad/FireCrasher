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


public class FireLooper implements Runnable {
    private static final Object EXIT = new Object();
    private static final ThreadLocal<FireLooper> RUNNINGS = new ThreadLocal<>();
    private static Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    private static Handler handler = new Handler(Looper.getMainLooper());

    public static void install() {
        handler.removeMessages(0, EXIT);
        handler.post(new FireLooper());
    }

    public static void uninstallDelay(long millis) {
        handler.removeMessages(0, EXIT);
        handler.sendMessageDelayed(handler.obtainMessage(0, EXIT), millis);
    }

    public static void uninstall() {
        uninstallDelay(0);
    }

    public static boolean isSafe() {
        return RUNNINGS.get() != null;
    }

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
