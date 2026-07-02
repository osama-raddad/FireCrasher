package com.osama.firecrasher

import android.annotation.SuppressLint
import android.os.*
import java.lang.reflect.Field
import java.lang.reflect.Method

internal class FireLooper : Runnable {

    @SuppressLint("DiscouragedPrivateApi")
    override fun run() {
        if (FIRE_LOOPER_THREAD_LOCAL.get() != null) return

        val next: Method
        val target: Field
        try {
            val method = MessageQueue::class.java.getDeclaredMethod("next")
            method.isAccessible = true
            next = method
            val field = Message::class.java.getDeclaredField("target")
            field.isAccessible = true
            target = field
        } catch (exception: Throwable) {
            return
        }

        FIRE_LOOPER_THREAD_LOCAL.set(this)

        val queue = Looper.myQueue()
        Binder.clearCallingIdentity()

        while (true) try {
            val message = next.invoke(queue) as Message? ?: break

            // The EXIT sentinel is posted by uninstall(): leave the loop and
            // fall back to the framework's Looper.loop(). The message is
            // dropped, not recycled — Message.recycle() throws on in-use
            // messages and recycleUnchecked() is hidden; losing one message
            // object on uninstall is harmless.
            if (message.obj === EXIT) break

            val handler = target.get(message) as? Handler ?: break
            handler.dispatchMessage(message)

            Binder.clearCallingIdentity()
        } catch (exception: Throwable) {
            uncaughtExceptionHandler?.uncaughtException(Thread.currentThread(), exception.cause
                    ?: exception)
            Handler(Looper.getMainLooper()).post(this)
            break
        }

        FIRE_LOOPER_THREAD_LOCAL.set(null)
    }

    companion object {
        private var EXIT = Any()
        private var FIRE_LOOPER_THREAD_LOCAL: ThreadLocal<FireLooper> = ThreadLocal()
        private var uncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null
        private var handler: Handler = Handler(Looper.getMainLooper())

        internal fun install() {
            // Drop any EXIT still queued from an uninstall() so it cannot
            // immediately shut the fresh loop down.
            handler.removeMessages(0, EXIT)
            handler.post(FireLooper())
        }

        // Ask the running loop to exit at its next queue poll; the framework's
        // own Looper.loop() takes over from there. No-op if the loop is not
        // running: the sentinel is consumed by the plain Handler and ignored.
        internal fun uninstall() {
            uncaughtExceptionHandler = null
            handler.sendMessage(handler.obtainMessage(0, EXIT))
        }

        internal val isSafe: Boolean
            get() = FIRE_LOOPER_THREAD_LOCAL.get() != null

        internal fun setUncaughtExceptionHandler(h: Thread.UncaughtExceptionHandler) {
            uncaughtExceptionHandler = h
        }
    }
}
