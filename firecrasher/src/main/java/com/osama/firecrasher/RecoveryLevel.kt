package com.osama.firecrasher

/**
 * The recovery escalation ladder, cheapest first.
 *
 * Declaration order is load-bearing: [RecoveryStateCodec] persists ordinals
 * across process death, so new levels may only be appended.
 */
public enum class RecoveryLevel {
    /** Restart the crashed activity (`recreate()`, then relaunch-and-finish). */
    RESTART_ACTIVITY,

    /** Give up on the crashing screen and pop the back stack. */
    GO_BACK,

    /** Nothing to go back to: relaunch the app from its launcher activity. */
    RELAUNCH_APP,
}
