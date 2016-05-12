package com.osama.firecrasherdemo;

import android.app.Activity;
import android.app.Application;
import android.widget.Toast;

import com.osama.firecrasher.CrashListener;
import com.osama.firecrasher.FireCrasher;

/**
 * Created by Osama Rsaddad.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        FireCrasher.install(this, new CrashListener() {

            @Override
            public void onCrash(Throwable throwable, Activity activity) {
                Toast.makeText(activity,throwable.getMessage(),Toast.LENGTH_SHORT).show();
                //you need to your crash reporting tool here
                //Ex: Crashlytics.logException(throwable);
            }
        });
        super.onCreate();
    }
}
