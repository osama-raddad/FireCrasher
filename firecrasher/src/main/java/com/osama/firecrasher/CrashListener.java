package com.osama.firecrasher;

import android.app.Activity;

/**
 * Created by Osama Rsaddad.
 */
public abstract class CrashListener {
    public abstract void onCrash(Throwable throwable, Activity activity);

    protected void recover(RecoverListener recoverListener) {
        recoverListener.OnRecover();
    }

    protected interface RecoverListener {
        void OnRecover();
    }
}
