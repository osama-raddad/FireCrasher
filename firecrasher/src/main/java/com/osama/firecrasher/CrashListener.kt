package com.osama.firecrasher

import android.app.Activity

/**
 * Created by Osama Raddad.
 */
abstract class CrashListener {
    abstract fun onCrash(throwable: Throwable, activity: Activity)

    protected fun recover(activity: Activity) {
        FireCrasher.recover(activity)
    }


}
