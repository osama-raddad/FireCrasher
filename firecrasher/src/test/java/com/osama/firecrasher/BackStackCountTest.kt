package com.osama.firecrasher

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackStackCountTest {

    private val handler = CrashHandler()

    private fun newActivity(): Activity = Robolectric.buildActivity(Activity::class.java).get()

    @Test
    fun `no activities means nothing to go back to`() {
        assertEquals(0, handler.backStackCount)
    }

    @Test
    fun `counts activities behind the current one`() {
        val callbacks = handler.lifecycleCallbacks
        callbacks.onActivityCreated(newActivity(), null)
        assertEquals(0, handler.backStackCount)
        callbacks.onActivityCreated(newActivity(), null)
        callbacks.onActivityCreated(newActivity(), null)
        assertEquals(2, handler.backStackCount)
    }

    @Test
    fun `destroying an activity shrinks the stack`() {
        val callbacks = handler.lifecycleCallbacks
        val first = newActivity()
        val second = newActivity()
        callbacks.onActivityCreated(first, null)
        callbacks.onActivityCreated(second, null)
        callbacks.onActivityDestroyed(second)
        assertEquals(0, handler.backStackCount)
    }

    @Test
    fun `recreate cycles are net zero`() {
        val callbacks = handler.lifecycleCallbacks
        callbacks.onActivityCreated(newActivity(), null)
        callbacks.onActivityCreated(newActivity(), null)

        // recreate(): the replacement is created, then the old instance dies
        val old = newActivity()
        callbacks.onActivityCreated(old, null)
        val replacement = newActivity()
        callbacks.onActivityCreated(replacement, null)
        callbacks.onActivityDestroyed(old)

        assertEquals(2, handler.backStackCount)
        assertEquals(replacement, handler.activity)
    }

    @Test
    fun `stray destroy callbacks never go negative`() {
        val callbacks = handler.lifecycleCallbacks
        callbacks.onActivityDestroyed(newActivity())
        callbacks.onActivityDestroyed(newActivity())
        assertEquals(0, handler.backStackCount)
    }

    @Test
    fun `destroying the current activity clears the reference`() {
        val callbacks = handler.lifecycleCallbacks
        val activity = newActivity()
        callbacks.onActivityCreated(activity, null)
        callbacks.onActivityDestroyed(activity)
        assertEquals(null, handler.activity)
    }
}
