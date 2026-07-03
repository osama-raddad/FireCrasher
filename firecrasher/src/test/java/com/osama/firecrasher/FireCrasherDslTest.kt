package com.osama.firecrasher

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FireCrasherDslTest {

    @After
    fun tearDown() {
        FireCrasher.resetForTest()
    }

    @Test
    fun `dispatch populates the crash scope`() {
        val callbacks = FireCrasher.crashHandler.lifecycleCallbacks
        val behind = Robolectric.buildActivity(Activity::class.java).setup().get()
        val current = Robolectric.buildActivity(Activity::class.java).setup().get()
        callbacks.onActivityCreated(behind, null)
        callbacks.onActivityCreated(current, null)
        callbacks.onActivityResumed(current)
        var scope: CrashScope? = null
        FireCrasher.onCrashHandler = { scope = this }
        val boom = RuntimeException("boom")

        FireCrasher.dispatchCrash(boom)

        assertSame(boom, scope?.throwable)
        assertSame(current, scope?.activity)
        assertEquals(RecoveryLevel.RESTART_ACTIVITY, scope?.level)
        assertEquals(0, scope?.retryCount)
    }

    @Test
    fun `default handler recovers automatically`() {
        // No activity yet: the RESTART_ACTIVITY path still counts the retry.
        FireCrasher.dispatchCrash(RuntimeException("boom"))

        assertEquals(1, FireCrasher.retryCount)
    }

    @Test
    fun `last onCrash registration wins`() {
        val config = FireCrasherConfig().apply {
            onCrash { error("first registration must be replaced") }
            onCrash { }
        }

        val scope = CrashScope(RuntimeException(), null, RecoveryLevel.RESTART_ACTIVITY, 0)
        config.onCrash.invoke(scope)
    }

    @Test
    fun `scope recover at a forced level runs that recovery`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val callbacks = FireCrasher.crashHandler.lifecycleCallbacks
        callbacks.onActivityCreated(activity, null)
        callbacks.onActivityResumed(activity)
        var dispatcherInvoked = false
        activity.onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                dispatcherInvoked = true
            }
        })
        val scope = CrashScope(RuntimeException(), activity, RecoveryLevel.RESTART_ACTIVITY, 0)

        scope.recover(RecoveryLevel.GO_BACK)

        assertTrue(dispatcherInvoked)
    }

    @Test
    fun `scope recover reports the recovered activity`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val callbacks = FireCrasher.crashHandler.lifecycleCallbacks
        callbacks.onActivityCreated(activity, null)
        callbacks.onActivityResumed(activity)
        var recovered: Activity? = null
        val scope = CrashScope(RuntimeException(), activity, RecoveryLevel.GO_BACK, 0)

        scope.recover { recovered = it }

        assertSame(activity, recovered)
    }
}
