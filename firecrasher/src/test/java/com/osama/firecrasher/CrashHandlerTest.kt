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

    private class RecordingListener : CrashListener() {
        var received: Throwable? = null

        override fun onCrash(throwable: Throwable) {
            received = throwable
        }
    }

    @Test
    fun `delivers the crash to the listener`() {
        val handler = CrashHandler()
        val listener = RecordingListener()
        handler.setCrashListener(listener)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        handler.lifecycleCallbacks.onActivityResumed(activity)
        val boom = RuntimeException("boom")

        handler.uncaughtException(Thread.currentThread(), boom)
        shadowOf(Looper.getMainLooper()).idle()

        assertSame(boom, listener.received)
    }

    @Test
    fun `crash before any activity still reaches the listener`() {
        val handler = CrashHandler()
        val listener = RecordingListener()
        handler.setCrashListener(listener)
        val boom = RuntimeException("early boom")

        handler.uncaughtException(Thread.currentThread(), boom)
        shadowOf(Looper.getMainLooper()).idle()

        assertSame(boom, listener.received)
    }

    @Test
    fun `crash without a listener does not throw`() {
        CrashHandler().uncaughtException(Thread.currentThread(), RuntimeException("ignored"))
    }
}
