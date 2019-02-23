package com.osama.firecrasher

import android.app.Activity

interface CrashInterface {
    fun onCrash(throwable: Throwable, activity: Activity)
}
