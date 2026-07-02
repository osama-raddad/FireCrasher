package com.osama.firecrasher

import android.app.Activity
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShouldRecoverTest {

    private class RecordingListener : CrashListener() {
        var received: Throwable? = null

        override fun onCrash(throwable: Throwable) {
            received = throwable
        }
    }

    private fun handlerWithActivity(listener: CrashListener, config: FireCrasherConfig): CrashHandler {
        val handler = CrashHandler()
        handler.setCrashListener(listener)
        handler.setConfig(config)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        handler.lifecycleCallbacks.onActivityResumed(activity)
        return handler
    }

    @Test
    fun `declined crash goes to the previous default handler, not the listener`() {
        val listener = RecordingListener()
        val config = FireCrasherConfig.Builder().setShouldRecover { it !is Error }.build()
        val handler = handlerWithActivity(listener, config)
        var delegated: Throwable? = null
        handler.setPreviousDefaultHandler { _, throwable -> delegated = throwable }
        val fatal = OutOfMemoryError("no heap")

        handler.uncaughtException(Thread.currentThread(), fatal)

        assertSame(fatal, delegated)
        assertNull(listener.received)
    }

    @Test
    fun `declined crash without a previous handler terminates the process`() {
        val listener = RecordingListener()
        val config = FireCrasherConfig.Builder().setShouldRecover { false }.build()
        val handler = handlerWithActivity(listener, config)
        var terminated: Throwable? = null
        handler.processTerminator = CrashHandler.ProcessTerminator { terminated = it }
        val boom = RuntimeException("boom")

        handler.uncaughtException(Thread.currentThread(), boom)

        assertSame(boom, terminated)
        assertNull(listener.received)
    }

    @Test
    fun `accepted crash is delivered to the listener as before`() {
        val listener = RecordingListener()
        val config = FireCrasherConfig.Builder().setShouldRecover { it !is Error }.build()
        val handler = handlerWithActivity(listener, config)
        var delegated: Throwable? = null
        handler.setPreviousDefaultHandler { _, throwable -> delegated = throwable }
        val boom = RuntimeException("boom")

        handler.uncaughtException(Thread.currentThread(), boom)

        assertSame(boom, listener.received)
        assertNull(delegated)
    }

    @Test
    fun `a throwing predicate falls back to recovering`() {
        val listener = RecordingListener()
        val config = FireCrasherConfig.Builder()
            .setShouldRecover { throw IllegalStateException("broken predicate") }
            .build()
        val handler = handlerWithActivity(listener, config)
        val boom = RuntimeException("boom")

        handler.uncaughtException(Thread.currentThread(), boom)

        assertSame(boom, listener.received)
    }
}
