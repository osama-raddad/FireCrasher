package com.osama.firecrasher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryStateCodecTest {

    @Test
    fun `round trips every level and retry count`() {
        for (level in CrashLevel.entries) {
            for (retryCount in intArrayOf(0, 1, 2, 64, 127)) {
                val decoded = RecoveryStateCodec.decode(RecoveryStateCodec.encode(level, retryCount))
                assertEquals(RecoveryState(level, retryCount), decoded)
            }
        }
    }

    @Test
    fun `clamps retry count into byte range`() {
        assertEquals(127, RecoveryStateCodec.decode(RecoveryStateCodec.encode(CrashLevel.LEVEL_ONE, 500))?.retryCount)
        assertEquals(0, RecoveryStateCodec.decode(RecoveryStateCodec.encode(CrashLevel.LEVEL_ONE, -3))?.retryCount)
    }

    @Test
    fun `stays within the 128 byte process state summary limit`() {
        assertTrue(RecoveryStateCodec.encode(CrashLevel.LEVEL_THREE, 127).size <= 128)
    }

    @Test
    fun `rejects null short foreign and wrong version payloads`() {
        assertNull(RecoveryStateCodec.decode(null))
        assertNull(RecoveryStateCodec.decode(byteArrayOf()))
        assertNull(RecoveryStateCodec.decode(byteArrayOf(1, 2)))
        assertNull(RecoveryStateCodec.decode("not ours".toByteArray()))
        val wrongVersion = RecoveryStateCodec.encode(CrashLevel.LEVEL_ONE, 1).apply { this[2] = 99 }
        assertNull(RecoveryStateCodec.decode(wrongVersion))
        val badLevel = RecoveryStateCodec.encode(CrashLevel.LEVEL_ONE, 1).apply { this[3] = 42 }
        assertNull(RecoveryStateCodec.decode(badLevel))
    }
}
