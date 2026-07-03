package com.osama.firecrasher

import android.app.Activity

/**
 * Receiver of the [FireCrasherConfig.onCrash] handler: everything known about
 * the crash being handled, plus [recover] to start the staged recovery.
 */
@FireCrasherDsl
public class CrashScope internal constructor(
    /** The uncaught exception. Report it to your crash reporter before recovering. */
    public val throwable: Throwable,
    /** The foreground activity when the crash was delivered, if any. */
    public val activity: Activity?,
    /** The recovery level FireCrasher evaluated for this crash. */
    public val level: RecoveryLevel,
    /** How many times recovery has already retried the current crash. */
    public val retryCount: Int,
) {
    /**
     * Run recovery, by default at the evaluated [level]. [onRecovered] is
     * called once recovery has started, with the activity recovered to.
     */
    public fun recover(
        level: RecoveryLevel = this.level,
        onRecovered: (activity: Activity?) -> Unit = {},
    ) {
        FireCrasher.recover(level, onRecovered)
    }
}
