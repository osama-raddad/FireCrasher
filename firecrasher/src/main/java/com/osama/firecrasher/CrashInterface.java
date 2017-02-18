package com.osama.firecrasher;

import android.app.Activity;

/**
 * Created by menilv on 2/17/17.
 */

public interface CrashInterface {
  public void onCrash(Throwable throwable, Activity activity);

  public void recover(Activity activity);

}
