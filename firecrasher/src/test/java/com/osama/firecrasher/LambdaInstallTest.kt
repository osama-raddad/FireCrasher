package com.osama.firecrasher

import android.app.Activity
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LambdaInstallTest {

    private var handlerBeforeTest: Thread.UncaughtExceptionHandler? = null

    @Before
    fun rememberDefaultHandler() {
        handlerBeforeTest = Thread.getDefaultUncaughtExceptionHandler()
    }

    @After
    fun tearDown() {
        FireCrasher.resetForTest()
        Thread.setDefaultUncaughtExceptionHandler(handlerBeforeTest)
    }

    @Test
    fun `lambda install delivers a complete crash event`() {
        var event: CrashEvent? = null
        val application = ApplicationProvider.getApplicationContext<Application>()
        FireCrasher.install(application, FireCrasherConfig.DEFAULT, FireCrasher.asListener { event = it }, hookMainLooper = false)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val boom = RuntimeException("boom")

        // install() made FireCrasher's handler the process default; delivery is
        // inline because an activity is resumed and we are on the main thread.
        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), boom)

        val received = event
        assertNotNull(received)
        assertSame(boom, received!!.throwable)
        assertSame(Thread.currentThread(), received.thread)
        assertSame(activity, received.activity)
        assertEquals(0, received.retryCount)
        assertEquals(CrashLevel.LEVEL_ONE, received.suggestedLevel)
    }

    @Test
    fun `crash event recover runs recovery at the forced level`() {
        var event: CrashEvent? = null
        var backPressed = false
        val application = ApplicationProvider.getApplicationContext<Application>()
        FireCrasher.install(application, FireCrasherConfig.DEFAULT, FireCrasher.asListener { event = it }, hookMainLooper = false)
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backPressed = true
            }
        })

        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), RuntimeException("boom"))
        event!!.recover(CrashLevel.LEVEL_TWO)

        assertTrue(backPressed)
    }

    @Test
    fun `back stack depth provider drives level selection`() {
        var event: CrashEvent? = null
        val application = ApplicationProvider.getApplicationContext<Application>()
        val config = FireCrasherConfig.Builder()
            .setLevelOneRetries(0)
            // Single-activity app reporting its own navigation depth.
            .setBackStackDepthProvider { 3 }
            .build()
        FireCrasher.install(application, config, FireCrasher.asListener { event = it }, hookMainLooper = false)
        Robolectric.buildActivity(Activity::class.java).setup()

        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), RuntimeException("boom"))

        // One live activity means backStackCount would be 0 (LEVEL_THREE);
        // the provider's depth makes going back viable instead.
        assertEquals(CrashLevel.LEVEL_TWO, event!!.suggestedLevel)
        assertEquals(CrashLevel.LEVEL_TWO, FireCrasher.evaluate())
    }

    @Test
    fun `install captures the previously installed default handler`() {
        var delegated: Throwable? = null
        val previous = Thread.UncaughtExceptionHandler { _, throwable -> delegated = throwable }
        Thread.setDefaultUncaughtExceptionHandler(previous)
        val application = ApplicationProvider.getApplicationContext<Application>()

        FireCrasher.install(
            application,
            FireCrasherConfig.Builder().setShouldRecover { false }.build(),
            FireCrasher.asListener { },
            hookMainLooper = false,
        )
        Robolectric.buildActivity(Activity::class.java).setup()
        val boom = RuntimeException("not recoverable")
        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), boom)

        assertSame(boom, delegated)
    }
}
