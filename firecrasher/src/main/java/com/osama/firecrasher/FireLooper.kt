package com.osama.firecrasher

import android.annotation.SuppressLint
import android.os.*
import java.lang.reflect.Field
import java.lang.reflect.Method

class FireLooper : Runnable {

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
            val message =
                    next.invoke(queue)
                            .takeIf { it is Message && it.obj != EXIT } as Message? ?: break

            val handler = target.get(message) as? Handler ?: break
            handler.dispatchMessage(message)

            Binder.clearCallingIdentity()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) message.recycle()
        } catch (exception: Throwable) {
            uncaughtExceptionHandler?.uncaughtException(Thread.currentThread(), exception.cause
                    ?: exception)
            Handler().post(this)
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
            handler.removeMessages(0, EXIT)
            handler.post(FireLooper())
        }

        internal val isSafe: Boolean
            get() = FIRE_LOOPER_THREAD_LOCAL.get() != null

        internal fun setUncaughtExceptionHandler(h: Thread.UncaughtExceptionHandler) {
            uncaughtExceptionHandler = h
        }
    }
}
