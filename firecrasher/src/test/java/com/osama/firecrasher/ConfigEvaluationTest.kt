package com.osama.firecrasher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ConfigEvaluationTest {

    @Test
    fun `default threshold matches the historical two-retry behavior`() {
        val default = FireCrasherConfig.DEFAULT.levelOneRetries
        assertEquals(CrashLevel.LEVEL_ONE, FireCrasher.evaluate(retryCount = 0, backStackCount = 0, levelOneRetries = default))
        assertEquals(CrashLevel.LEVEL_ONE, FireCrasher.evaluate(retryCount = 1, backStackCount = 3, levelOneRetries = default))
        assertEquals(CrashLevel.LEVEL_TWO, FireCrasher.evaluate(retryCount = 2, backStackCount = 1, levelOneRetries = default))
        assertEquals(CrashLevel.LEVEL_THREE, FireCrasher.evaluate(retryCount = 2, backStackCount = 0, levelOneRetries = default))
    }

    @Test
    fun `zero retries skips activity restart entirely`() {
        assertEquals(CrashLevel.LEVEL_TWO, FireCrasher.evaluate(retryCount = 0, backStackCount = 1, levelOneRetries = 0))
        assertEquals(CrashLevel.LEVEL_THREE, FireCrasher.evaluate(retryCount = 0, backStackCount = 0, levelOneRetries = 0))
    }

    @Test
    fun `a large threshold keeps restarting the activity`() {
        assertEquals(CrashLevel.LEVEL_ONE, FireCrasher.evaluate(retryCount = 9, backStackCount = 4, levelOneRetries = 10))
        assertEquals(CrashLevel.LEVEL_TWO, FireCrasher.evaluate(retryCount = 10, backStackCount = 4, levelOneRetries = 10))
    }

    @Test
    fun `builder rejects invalid values`() {
        assertThrows(IllegalArgumentException::class.java) {
            FireCrasherConfig.Builder().setLevelOneRetries(-1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FireCrasherConfig.Builder().setCrashLoopBreaker(0, 1_000L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FireCrasherConfig.Builder().setCrashLoopBreaker(3, 0L)
        }
    }

    @Test
    fun `builder defaults match the documented policy`() {
        val config = FireCrasherConfig.Builder().build()
        assertEquals(2, config.levelOneRetries)
        assertEquals(10, config.crashLoopThreshold)
        assertEquals(60_000L, config.crashLoopWindowMillis)
        assertEquals(true, config.shouldRecover.shouldRecover(RuntimeException()))
        assertEquals(null, config.backStackDepthProvider)
    }

    @Test
    fun `disabling the crash loop breaker zeroes the threshold`() {
        val config = FireCrasherConfig.Builder()
            .setCrashLoopBreaker(3, 1_000L)
            .disableCrashLoopBreaker()
            .build()
        assertEquals(0, config.crashLoopThreshold)
    }
}
