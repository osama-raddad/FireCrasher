package com.osama.firecrasher

import android.app.Activity

/**
 * Everything known about a caught crash, delivered to the lambda passed to
 * [FireCrasher.install]. Report [throwable] to your crash reporter, then call
 * [recover].
 *
 * The constructor is internal so new context can be added in future releases
 * without breaking binary compatibility.
 */
public class CrashEvent internal constructor(
    /** The uncaught exception. */
    public val throwable: Throwable,
    /** The thread the exception was raised on. */
    public val thread: Thread,
    /** The activity in the foreground when the crash happened, if any. */
    public val activity: Activity?,
    /** How many times recovery has already retried the current crash. */
    public val retryCount: Int,
    /** The recovery level [recover] would use right now. */
    public val suggestedLevel: CrashLevel,
) {

    /** Run recovery at [suggestedLevel]. */
    public fun recover() {
        FireCrasher.recover()
    }

    /** Run recovery at a forced [level]. */
    public fun recover(level: CrashLevel) {
        FireCrasher.recover(level)
    }
}
