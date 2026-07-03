package com.osama.firecrasher

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build

/**
 * Past terminations of this app recorded by the system, newest first.
 * Includes causes the in-process handler can never see, such as native
 * crashes, ANRs, and low-memory kills. Empty below API 30.
 */
public fun Context.historicalExitReasons(maxCount: Int = 16): List<ApplicationExitInfo> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return activityManager.getHistoricalProcessExitReasons(packageName, 0, maxCount)
}

/**
 * The most recent recorded exit caused by a crash, native crash, or ANR,
 * or null if none is recorded. The record may predate the previous launch;
 * use [ApplicationExitInfo.getTimestamp] to judge freshness. Null below API 30.
 */
public fun Context.lastAbnormalExit(): ApplicationExitInfo? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    return historicalExitReasons().firstOrNull {
        it.reason == ApplicationExitInfo.REASON_CRASH ||
                it.reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
                it.reason == ApplicationExitInfo.REASON_ANR
    }
}
