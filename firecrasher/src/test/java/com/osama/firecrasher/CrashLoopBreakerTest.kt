package com.osama.firecrasher

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashLoopBreakerTest {

    private class RecordingListener : CrashListener() {
        var crashCount: Int = 0

        override fun onCrash(throwable: Throwable) {
            crashCount++
        }
    }

    private class Harness(config: FireCrasherConfig) {
        val listener = RecordingListener()
        val handler = CrashHandler()
        var now: Long = 0L
        var died: Throwable? = null

        init {
            handler.setCrashListener(listener)
            handler.setConfig(config)
            handler.clock = CrashHandler.Clock { now }
            handler.setPreviousDefaultHandler { _, throwable -> died = throwable }
            val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
            handler.lifecycleCallbacks.onActivityResumed(activity)
        }

        fun crashAt(timeMillis: Long) {
            now = timeMillis
            handler.uncaughtException(Thread.currentThread(), RuntimeException("crash@$timeMillis"))
        }
    }

    @Test
    fun `crashes below the threshold keep recovering`() {
        val harness = Harness(FireCrasherConfig.Builder().setCrashLoopBreaker(3, 1_000L).build())

        harness.crashAt(0L)
        harness.crashAt(100L)

        assertEquals(2, harness.listener.crashCount)
        assertNull(harness.died)
    }

    @Test
    fun `the threshold crash within the window dies normally`() {
        val harness = Harness(FireCrasherConfig.Builder().setCrashLoopBreaker(3, 1_000L).build())

        harness.crashAt(0L)
        harness.crashAt(100L)
        harness.crashAt(200L)

        assertEquals(2, harness.listener.crashCount)
        assertEquals("crash@200", harness.died?.message)
    }

    @Test
    fun `crashes spread beyond the window keep recovering`() {
        val harness = Harness(FireCrasherConfig.Builder().setCrashLoopBreaker(3, 1_000L).build())

        harness.crashAt(0L)
        harness.crashAt(900L)
        harness.crashAt(1_800L)
        harness.crashAt(2_700L)

        assertEquals(4, harness.listener.crashCount)
        assertNull(harness.died)
    }

    @Test
    fun `a disabled breaker never trips`() {
        val harness = Harness(
            FireCrasherConfig.Builder().disableCrashLoopBreaker().build(),
        )

        repeat(50) { harness.crashAt(it.toLong()) }

        assertEquals(50, harness.listener.crashCount)
        assertNull(harness.died)
    }
}
