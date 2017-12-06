package com.osama.firecrasher;

import android.annotation.SuppressLint;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FireLooper implements Runnable {
    private static Object EXIT;
    private static ThreadLocal<FireLooper> FIRE_LOOPER_THREAD_LOCAL;
    private static Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    private static Handler handler;

    static {
        EXIT = new Object();
        FIRE_LOOPER_THREAD_LOCAL = new ThreadLocal<>();
        handler = new Handler(Looper.getMainLooper());
    }

    static void install() {
        handler.removeMessages(0, EXIT);
        handler.post(new FireLooper());
    }

    static boolean isSafe() {
        return FIRE_LOOPER_THREAD_LOCAL.get() != null;
    }

    static void setUncaughtExceptionHandler(
            Thread.UncaughtExceptionHandler h) {
        uncaughtExceptionHandler = h;
    }

    @Override
    public void run() {
        if (FIRE_LOOPER_THREAD_LOCAL.get() != null)
            return;

        Method next;
        Field target;
        try {
            @SuppressLint("PrivateApi") Method method = MessageQueue.class.getDeclaredMethod("next");
            method.setAccessible(true);
            next = method;
            Field field = Message.class.getDeclaredField("target");
            field.setAccessible(true);
            target = field;
        } catch (Exception e) {
            return;
        }

        FIRE_LOOPER_THREAD_LOCAL.set(this);
        MessageQueue queue = Looper.myQueue();
        Binder.clearCallingIdentity();

        while (true) {
            try {
                Message message = (Message) next.invoke(queue);
                if (message == null || message.obj == EXIT)
                    break;

                Handler handler = (Handler) target.get(message);
                handler.dispatchMessage(message);

                Binder.clearCallingIdentity();

                int currentVersion = android.os.Build.VERSION.SDK_INT;
                if (currentVersion < android.os.Build.VERSION_CODES.LOLLIPOP)
                    message.recycle();
            } catch (InvocationTargetException exception) {
                Thread.UncaughtExceptionHandler exceptionHandler = uncaughtExceptionHandler;
                Throwable throwable;
                throwable = exception.getCause();
                if (throwable == null) throwable = exception;

                if (exceptionHandler != null) exceptionHandler
                        .uncaughtException(Thread.currentThread(), throwable);

                new Handler().post(this);
                break;
            } catch (Exception e) {
                Thread.UncaughtExceptionHandler exceptionHandler = uncaughtExceptionHandler;
                if (exceptionHandler != null)
                    exceptionHandler.uncaughtException(Thread.currentThread(), e);
                new Handler().post(this);
                break;
            }
        }

        FIRE_LOOPER_THREAD_LOCAL.set(null);
    }
}
