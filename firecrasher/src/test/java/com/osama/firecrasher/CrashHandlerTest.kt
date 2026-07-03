package com.osama.firecrasher

import android.app.Activity
import android.os.Looper
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class CrashHandlerTest {

    @Test
    fun `delivers the crash to the handler`() {
        val handler = CrashHandler()
        var received: Throwable? = null
        handler.onCrash = { received = it }
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        handler.lifecycleCallbacks.onActivityResumed(activity)
        val boom = RuntimeException("boom")

        handler.uncaughtException(Thread.currentThread(), boom)
        shadowOf(Looper.getMainLooper()).idle()

        assertSame(boom, received)
    }

    @Test
    fun `crash before any activity still reaches the handler`() {
        val handler = CrashHandler()
        var received: Throwable? = null
        handler.onCrash = { received = it }
        val boom = RuntimeException("early boom")

        handler.uncaughtException(Thread.currentThread(), boom)
        shadowOf(Looper.getMainLooper()).idle()

        assertSame(boom, received)
    }

    @Test
    fun `crash without a handler does not throw`() {
        CrashHandler().uncaughtException(Thread.currentThread(), RuntimeException("ignored"))
    }
}
