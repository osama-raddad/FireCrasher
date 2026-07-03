package com.osama.firecrasher

import android.app.ApplicationExitInfo

/**
 * Scopes the FireCrasher DSL so [FireCrasherConfig] methods cannot be called
 * implicitly from inside a [CrashScope] handler and vice versa.
 */
@DslMarker
@Target(AnnotationTarget.CLASS)
public annotation class FireCrasherDsl

/**
 * Configuration collected by [FireCrasher.install] / [installFireCrasher].
 */
@FireCrasherDsl
public class FireCrasherConfig internal constructor() {

    internal var onCrash: CrashScope.() -> Unit = { recover() }
        private set

    internal var onPreviousProcessExit: (ApplicationExitInfo) -> Unit = {}
        private set

    /**
     * Called on the main thread when an uncaught exception is caught. The last
     * registration wins. If never called, the default handler is `{ recover() }`,
     * so a bare install already recovers automatically.
     */
    public fun onCrash(handler: CrashScope.() -> Unit) {
        onCrash = handler
    }

    /**
     * Called from install on API 30+ when the system has a record of this app
     * dying from a crash, native crash, or ANR — including failures the
     * in-process handler can never intercept. The record may predate the
     * previous launch; check [ApplicationExitInfo.getTimestamp] before
     * treating it as fresh.
     */
    public fun onPreviousProcessExit(handler: (exitInfo: ApplicationExitInfo) -> Unit) {
        onPreviousProcessExit = handler
    }
}
