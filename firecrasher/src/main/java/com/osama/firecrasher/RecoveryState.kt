package com.osama.firecrasher

/**
 * Recovery progress captured just before the process might die, so a fresh
 * process can tell whether it is relaunching after a failed recovery.
 */
internal data class RecoveryState(val level: RecoveryLevel, val retryCount: Int)

/**
 * Encodes [RecoveryState] into the small byte array that
 * `ActivityManager.setProcessStateSummary` stores with the process's
 * `ApplicationExitInfo` record (limit 128 bytes).
 */
internal object RecoveryStateCodec {
    private const val VERSION: Byte = 1
    private val MAGIC = byteArrayOf('F'.code.toByte(), 'C'.code.toByte())
    private const val SIZE = 5

    fun encode(level: RecoveryLevel, retryCount: Int): ByteArray = byteArrayOf(
        MAGIC[0],
        MAGIC[1],
        VERSION,
        level.ordinal.toByte(),
        retryCount.coerceIn(0, Byte.MAX_VALUE.toInt()).toByte(),
    )

    fun decode(bytes: ByteArray?): RecoveryState? {
        if (bytes == null || bytes.size < SIZE) return null
        if (bytes[0] != MAGIC[0] || bytes[1] != MAGIC[1] || bytes[2] != VERSION) return null
        val level = RecoveryLevel.entries.getOrNull(bytes[3].toInt()) ?: return null
        return RecoveryState(level, bytes[4].toInt())
    }
}
