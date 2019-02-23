package com.osama.firecrasherdemo

import android.app.Activity
import android.app.Application
import android.widget.Toast

import com.osama.firecrasher.CrashListener
import com.osama.firecrasher.FireCrasher

/**
 * Created by Osama Raddad.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FireCrasher.install(this, object : CrashListener() {

            override fun onCrash(throwable: Throwable, activity: Activity) {
                Toast.makeText(activity, throwable.message, Toast.LENGTH_SHORT).show()
                // start the recovering process
                recover(activity)

                //you need to add your crash reporting tool here
                //Ex: Crashlytics.logException(throwable);
            }
        })
    }
}
