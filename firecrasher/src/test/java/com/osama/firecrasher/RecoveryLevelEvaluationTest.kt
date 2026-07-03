package com.osama.firecrasher

import org.junit.Assert.assertEquals
import org.junit.Test

class RecoveryLevelEvaluationTest {

    @Test
    fun `first crash restarts the activity`() {
        assertEquals(RecoveryLevel.RESTART_ACTIVITY, FireCrasher.evaluate(retryCount = 0, backStackCount = 0))
        assertEquals(RecoveryLevel.RESTART_ACTIVITY, FireCrasher.evaluate(retryCount = 0, backStackCount = 5))
    }

    @Test
    fun `second crash still restarts the activity`() {
        assertEquals(RecoveryLevel.RESTART_ACTIVITY, FireCrasher.evaluate(retryCount = 1, backStackCount = 0))
        assertEquals(RecoveryLevel.RESTART_ACTIVITY, FireCrasher.evaluate(retryCount = 1, backStackCount = 3))
    }

    @Test
    fun `repeated crashes go back when a back stack exists`() {
        assertEquals(RecoveryLevel.GO_BACK, FireCrasher.evaluate(retryCount = 2, backStackCount = 1))
        assertEquals(RecoveryLevel.GO_BACK, FireCrasher.evaluate(retryCount = 5, backStackCount = 3))
    }

    @Test
    fun `repeated crashes with nothing to go back to relaunch the app`() {
        assertEquals(RecoveryLevel.RELAUNCH_APP, FireCrasher.evaluate(retryCount = 2, backStackCount = 0))
        assertEquals(RecoveryLevel.RELAUNCH_APP, FireCrasher.evaluate(retryCount = 10, backStackCount = 0))
    }
}
