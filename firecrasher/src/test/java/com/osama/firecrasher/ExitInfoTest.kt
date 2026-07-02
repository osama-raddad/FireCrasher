package com.osama.firecrasher

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivityManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30, 36])
class ExitInfoTest {

    private class RecordingListener : CrashListener() {
        var exitInfo: ApplicationExitInfo? = null

        override fun onCrash(throwable: Throwable) = Unit

        override fun onPreviousProcessExit(exitInfo: ApplicationExitInfo) {
            this.exitInfo = exitInfo
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun addExitRecord(
        reason: Int,
        timestamp: Long = System.currentTimeMillis(),
        summary: ByteArray? = null,
    ): ApplicationExitInfo {
        val builder = ShadowActivityManager.ApplicationExitInfoBuilder.newBuilder()
            .setReason(reason)
            .setTimestamp(timestamp)
        summary?.let { builder.setProcessStateSummary(it) }
        val info = builder.build()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        shadowOf(activityManager).addApplicationExitInfo(info)
        return info
    }

    @After
    fun tearDown() {
        FireCrasher.resetForTest()
    }

    @Test
    fun `no records means no exit reasons`() {
        assertTrue(FireCrasher.getHistoricalExitReasons(context).isEmpty())
        assertNull(FireCrasher.getLastAbnormalExit(context))
    }

    @Test
    fun `returns recorded exits`() {
        addExitRecord(ApplicationExitInfo.REASON_CRASH)
        val exits = FireCrasher.getHistoricalExitReasons(context)
        assertEquals(1, exits.size)
        assertEquals(ApplicationExitInfo.REASON_CRASH, exits.first().reason)
    }

    @Test
    fun `normal exits are not abnormal`() {
        addExitRecord(ApplicationExitInfo.REASON_EXIT_SELF)
        addExitRecord(ApplicationExitInfo.REASON_USER_REQUESTED)
        assertNull(FireCrasher.getLastAbnormalExit(context))
    }

    @Test
    fun `finds crashes native crashes and anrs`() {
        addExitRecord(ApplicationExitInfo.REASON_ANR)
        assertEquals(ApplicationExitInfo.REASON_ANR, FireCrasher.getLastAbnormalExit(context)?.reason)
    }

    @Test
    fun `recent recovery state is restored after process death`() {
        val listener = RecordingListener()
        addExitRecord(
            reason = ApplicationExitInfo.REASON_CRASH,
            summary = RecoveryStateCodec.encode(CrashLevel.LEVEL_THREE, 3),
        )

        FireCrasher.restorePreviousRecoveryState(context, listener)

        assertEquals(3, FireCrasher.retryCount)
        assertNotNull(listener.exitInfo)
    }

    @Test
    fun `restoring past level one escalates beyond activity restart`() {
        addExitRecord(
            reason = ApplicationExitInfo.REASON_CRASH,
            summary = RecoveryStateCodec.encode(CrashLevel.LEVEL_TWO, 0),
        )

        FireCrasher.restorePreviousRecoveryState(context, RecordingListener())

        // A restored LEVEL_TWO+ recovery must not evaluate back to LEVEL_ONE.
        assertTrue(FireCrasher.retryCount >= 2)
    }

    @Test
    fun `stale recovery state is reported but not restored`() {
        val listener = RecordingListener()
        addExitRecord(
            reason = ApplicationExitInfo.REASON_CRASH,
            timestamp = System.currentTimeMillis() - 60_000L,
            summary = RecoveryStateCodec.encode(CrashLevel.LEVEL_THREE, 3),
        )

        FireCrasher.restorePreviousRecoveryState(context, listener)

        assertEquals(0, FireCrasher.retryCount)
        assertNotNull(listener.exitInfo)
    }

    @Test
    fun `foreign process state summaries are ignored`() {
        val listener = RecordingListener()
        addExitRecord(
            reason = ApplicationExitInfo.REASON_CRASH,
            summary = byteArrayOf(9, 9, 9, 9, 9),
        )

        FireCrasher.restorePreviousRecoveryState(context, listener)

        assertEquals(0, FireCrasher.retryCount)
        assertNotNull(listener.exitInfo)
    }
}
