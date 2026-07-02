package com.osama.firecrasher

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoBackDispatchTest {

    class LegacyBackActivity : Activity() {
        var legacyBackPressed = false

        @Deprecated("Deprecated in Java")
        override fun onBackPressed() {
            legacyBackPressed = true
        }
    }

    @After
    fun tearDown() {
        FireCrasher.resetForTest()
    }

    @Test
    fun `component activity recovery goes through the back dispatcher`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        var dispatcherInvoked = false
        activity.onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                dispatcherInvoked = true
            }
        })

        FireCrasher.goBack(Pair(activity, null))

        assertTrue(dispatcherInvoked)
    }

    @Test
    fun `component activity without callbacks finishes via dispatcher fallback`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()

        FireCrasher.goBack(Pair(activity, null))

        assertTrue(activity.isFinishing)
    }

    @Test
    fun `plain activity recovery falls back to the legacy back press`() {
        val activity = Robolectric.buildActivity(LegacyBackActivity::class.java).setup().get()
        assertFalse(activity.legacyBackPressed)

        FireCrasher.goBack(Pair(activity, null))

        assertTrue(activity.legacyBackPressed)
    }

    @Test
    fun `null activity is a no-op`() {
        FireCrasher.goBack(Pair(null, null))
    }
}
