package com.osama.firecrasherdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void crash(View view) throws java.lang.Exception {
        throw new Exception();
    }

    // The demo config's shouldRecover predicate declines Errors, so this kills
    // the app exactly as it would without FireCrasher installed.
    public void throwError(View view) {
        throw new OutOfMemoryError("demo error - not recovered");
    }

    // Posts itself again after every recovery; the demo config's crash-loop
    // breaker (5 crashes / 30s) gives up and lets the app die.
    public void crashLoop(View view) {
        View button = findViewById(R.id.buttonCrashLoop);
        button.postDelayed(() -> crashLoop(button), 500);
        throw new IllegalStateException("demo crash loop");
    }

    public void next(View view) {
        startActivity(new Intent(this, Main2Activity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
