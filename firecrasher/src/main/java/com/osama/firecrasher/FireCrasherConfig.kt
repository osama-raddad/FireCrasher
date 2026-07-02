package com.osama.firecrasher

/**
 * Decides whether FireCrasher should recover from a caught throwable. Return
 * `false` to let the crash proceed as if FireCrasher were not installed: the
 * previously installed default handler (e.g. a crash reporter's) runs and the
 * process dies normally.
 *
 * The default recovers everything. A common stricter policy is to never absorb
 * JVM errors: `RecoveryPredicate { it !is VirtualMachineError }` (Kotlin) or
 * `t -> !(t instanceof VirtualMachineError)` (Java).
 */
public fun interface RecoveryPredicate {
    public fun shouldRecover(throwable: Throwable): Boolean
}

/**
 * Supplies the logical back-stack depth used to pick between going back
 * ([CrashLevel.LEVEL_TWO]) and restarting the app ([CrashLevel.LEVEL_THREE]).
 *
 * By default FireCrasher counts live activities, which is right for
 * multi-activity apps but always reports zero depth in a single-activity
 * (e.g. Jetpack Compose + Navigation) app. Provide the depth of your own
 * navigation stack instead — for Navigation Compose:
 * `BackStackDepthProvider { if (navController.previousBackStackEntry != null) 1 else 0 }`.
 * Going back is dispatched through `OnBackPressedDispatcher`, which pops the
 * Compose destination.
 */
public fun interface BackStackDepthProvider {
    public fun getDepth(): Int
}

/**
 * Tuning knobs for the recovery policy. Obtain via [Builder]; pass to
 * [FireCrasher.install]. [DEFAULT] preserves FireCrasher's historical
 * behavior except for the crash-loop breaker, which is on by default.
 */
public class FireCrasherConfig private constructor(
    /** Crashes absorbed by restarting the same activity before escalating past [CrashLevel.LEVEL_ONE]. */
    public val levelOneRetries: Int,
    /** Consulted before recovery; `false` hands the crash to the previous default handler. */
    public val shouldRecover: RecoveryPredicate,
    /** Crashes within [crashLoopWindowMillis] that trip the breaker; `0` disables it. */
    public val crashLoopThreshold: Int,
    /** Sliding window for the crash-loop breaker. */
    public val crashLoopWindowMillis: Long,
    /** Custom back-stack depth source; `null` counts live activities. */
    public val backStackDepthProvider: BackStackDepthProvider?,
) {

    public class Builder {
        private var levelOneRetries: Int = DEFAULT_LEVEL_ONE_RETRIES
        private var shouldRecover: RecoveryPredicate = RECOVER_EVERYTHING
        private var crashLoopThreshold: Int = DEFAULT_CRASH_LOOP_THRESHOLD
        private var crashLoopWindowMillis: Long = DEFAULT_CRASH_LOOP_WINDOW_MILLIS
        private var backStackDepthProvider: BackStackDepthProvider? = null

        /** How many crashes restart the same activity before escalating. Must be >= 0. */
        public fun setLevelOneRetries(count: Int): Builder = apply {
            require(count >= 0) { "levelOneRetries must be >= 0, was $count" }
            levelOneRetries = count
        }

        public fun setShouldRecover(predicate: RecoveryPredicate): Builder = apply {
            shouldRecover = predicate
        }

        /**
         * Stop recovering once [maxCrashes] crashes have been caught within
         * [windowMillis]; the crash then reaches the previous default handler
         * and the process dies as it would without FireCrasher.
         */
        public fun setCrashLoopBreaker(maxCrashes: Int, windowMillis: Long): Builder = apply {
            require(maxCrashes > 0) { "maxCrashes must be > 0, was $maxCrashes" }
            require(windowMillis > 0) { "windowMillis must be > 0, was $windowMillis" }
            crashLoopThreshold = maxCrashes
            crashLoopWindowMillis = windowMillis
        }

        /** Restore the pre-2.2.0 behavior of always recovering, however often the app crashes. */
        public fun disableCrashLoopBreaker(): Builder = apply {
            crashLoopThreshold = 0
        }

        public fun setBackStackDepthProvider(provider: BackStackDepthProvider): Builder = apply {
            backStackDepthProvider = provider
        }

        public fun build(): FireCrasherConfig = FireCrasherConfig(
            levelOneRetries = levelOneRetries,
            shouldRecover = shouldRecover,
            crashLoopThreshold = crashLoopThreshold,
            crashLoopWindowMillis = crashLoopWindowMillis,
            backStackDepthProvider = backStackDepthProvider,
        )
    }

    public companion object {
        private const val DEFAULT_LEVEL_ONE_RETRIES: Int = 2
        private const val DEFAULT_CRASH_LOOP_THRESHOLD: Int = 10
        private const val DEFAULT_CRASH_LOOP_WINDOW_MILLIS: Long = 60_000L
        private val RECOVER_EVERYTHING: RecoveryPredicate = RecoveryPredicate { true }

        @JvmField
        public val DEFAULT: FireCrasherConfig = Builder().build()
    }
}
