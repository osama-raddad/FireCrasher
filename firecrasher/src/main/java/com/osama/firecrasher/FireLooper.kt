package com.osama.firecrasher

import android.annotation.SuppressLint
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.MessageQueue

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class FireLooper : Runnable {

    override fun run() {
        if (FIRE_LOOPER_THREAD_LOCAL!!.get() != null)
            return

        val next: Method
        val target: Field
        try {
            @SuppressLint("PrivateApi") val method = MessageQueue::class.java.getDeclaredMethod("next")
            method.isAccessible = true
            next = method
            val field = Message::class.java.getDeclaredField("target")
            field.isAccessible = true
            target = field
        } catch (e: Exception) {
            return
        }

        FIRE_LOOPER_THREAD_LOCAL!!.set(this)
        val queue = Looper.myQueue()
        Binder.clearCallingIdentity()

        while (true) {
            try {
                val message = next.invoke(queue) as Message
                if (message == null || message.obj === EXIT)
                    break

                val handler = target.get(message) as Handler
                handler.dispatchMessage(message)

                Binder.clearCallingIdentity()

                val currentVersion = android.os.Build.VERSION.SDK_INT
                if (currentVersion < android.os.Build.VERSION_CODES.LOLLIPOP)
                    message.recycle()
            } catch (exception: InvocationTargetException) {
                val exceptionHandler = uncaughtExceptionHandler
                var throwable: Throwable?
                throwable = exception.cause
                if (throwable == null) throwable = exception

                exceptionHandler?.uncaughtException(Thread.currentThread(), throwable)

                Handler().post(this)
                break
            } catch (e: Exception) {
                val exceptionHandler = uncaughtExceptionHandler
                exceptionHandler?.uncaughtException(Thread.currentThread(), e)
                Handler().post(this)
                break
            }

        }

        FIRE_LOOPER_THREAD_LOCAL!!.set(null)
    }

    companion object {
        private var EXIT: Any? = null
        private var FIRE_LOOPER_THREAD_LOCAL: ThreadLocal<FireLooper>? = null
        private var uncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null
        private var handler: Handler? = null

        init {
            EXIT = Any()
            FIRE_LOOPER_THREAD_LOCAL = ThreadLocal()
            handler = Handler(Looper.getMainLooper())
        }

        internal fun install() {
            handler!!.removeMessages(0, EXIT)
            handler!!.post(FireLooper())
        }

        internal val isSafe: Boolean
            get() = FIRE_LOOPER_THREAD_LOCAL!!.get() != null

        internal fun setUncaughtExceptionHandler(
                h: Thread.UncaughtExceptionHandler) {
            uncaughtExceptionHandler = h
        }
    }
}
