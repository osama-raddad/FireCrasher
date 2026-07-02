package com.osama.firecrasher

import android.app.Activity
import android.app.ApplicationExitInfo

/**
 * Created by Osama Raddad.
 */
public abstract class CrashListener {
    public abstract fun onCrash(throwable: Throwable)

    /**
     * Called from [FireCrasher.install] on API 30+ when the system has a
     * record of this app dying from a crash, native crash, or ANR — including
     * failures the in-process handler can never intercept. The record may
     * predate the previous launch; check [ApplicationExitInfo.getTimestamp]
     * before treating it as fresh.
     */
    public open fun onPreviousProcessExit(exitInfo: ApplicationExitInfo) {}

    protected fun recover() {
        FireCrasher.recover(onRecover = null)
    }

    protected fun recover(level: CrashLevel) {
        FireCrasher.recover(level, onRecover = null)
    }

    protected fun recover(level: CrashLevel, onRecover: ((activity: Activity?) -> Unit)?) {
        FireCrasher.recover(level, onRecover = onRecover)
    }

    protected fun recover(onRecover: ((activity: Activity?) -> Unit)?) {
        FireCrasher.recover(onRecover = onRecover)
    }

    protected fun evaluate() {
        FireCrasher.evaluate()
    }

    protected fun evaluate(onEvaluate: ((activity: Activity?, level: CrashLevel) -> Unit)?) {
        FireCrasher.evaluateAsync(onEvaluate)
    }
}
