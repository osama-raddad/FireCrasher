package com.osama.firecrasher

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UninstallTest {

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
    fun `uninstall restores the previous default handler`() {
        val previous = Thread.UncaughtExceptionHandler { _, _ -> }
        Thread.setDefaultUncaughtExceptionHandler(previous)
        val application = ApplicationProvider.getApplicationContext<Application>()
        FireCrasher.install(application, FireCrasherConfig.DEFAULT, FireCrasher.asListener { }, hookMainLooper = false)

        FireCrasher.uninstall()

        assertSame(previous, Thread.getDefaultUncaughtExceptionHandler())
    }

    @Test
    fun `uninstall when not installed does not throw`() {
        FireCrasher.uninstall()
    }

    @Test
    fun `crashes after uninstall no longer reach the listener`() {
        var received: Throwable? = null
        var delegated: Throwable? = null
        Thread.setDefaultUncaughtExceptionHandler { _, throwable -> delegated = throwable }
        val application = ApplicationProvider.getApplicationContext<Application>()
        FireCrasher.install(application, FireCrasherConfig.DEFAULT, FireCrasher.asListener { received = it.throwable }, hookMainLooper = false)
        Robolectric.buildActivity(Activity::class.java).setup()

        FireCrasher.uninstall()
        val boom = RuntimeException("after uninstall")
        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), boom)

        assertNull(received)
        assertSame(boom, delegated)
    }

    @Test
    fun `reinstall after uninstall delivers crashes again`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        FireCrasher.install(application, FireCrasherConfig.DEFAULT, FireCrasher.asListener { }, hookMainLooper = false)
        FireCrasher.uninstall()

        var received: Throwable? = null
        FireCrasher.install(application, FireCrasherConfig.DEFAULT, FireCrasher.asListener { received = it.throwable }, hookMainLooper = false)
        Robolectric.buildActivity(Activity::class.java).setup()
        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), RuntimeException("boom"))

        assertNotNull(received)
    }
}
