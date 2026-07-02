package com.osama.firecrasher

import org.junit.Assert.assertEquals
import org.junit.Test

class CrashLevelEvaluationTest {

    @Test
    fun `first crash restarts the activity`() {
        assertEquals(CrashLevel.LEVEL_ONE, FireCrasher.evaluate(retryCount = 0, backStackCount = 0))
        assertEquals(CrashLevel.LEVEL_ONE, FireCrasher.evaluate(retryCount = 0, backStackCount = 5))
    }

    @Test
    fun `second crash still restarts the activity`() {
        assertEquals(CrashLevel.LEVEL_ONE, FireCrasher.evaluate(retryCount = 1, backStackCount = 0))
        assertEquals(CrashLevel.LEVEL_ONE, FireCrasher.evaluate(retryCount = 1, backStackCount = 3))
    }

    @Test
    fun `repeated crashes go back when a back stack exists`() {
        assertEquals(CrashLevel.LEVEL_TWO, FireCrasher.evaluate(retryCount = 2, backStackCount = 1))
        assertEquals(CrashLevel.LEVEL_TWO, FireCrasher.evaluate(retryCount = 5, backStackCount = 3))
    }

    @Test
    fun `repeated crashes with nothing to go back to restart the app`() {
        assertEquals(CrashLevel.LEVEL_THREE, FireCrasher.evaluate(retryCount = 2, backStackCount = 0))
        assertEquals(CrashLevel.LEVEL_THREE, FireCrasher.evaluate(retryCount = 10, backStackCount = 0))
    }
}
