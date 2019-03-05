package com.osama.firecrasher

import android.app.Activity

/**
 * Created by Osama Raddad.
 */
abstract class CrashListener {
    abstract fun onCrash(throwable: Throwable)

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
