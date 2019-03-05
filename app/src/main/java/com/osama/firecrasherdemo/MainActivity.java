package com.osama.firecrasherdemo;

import android.app.Activity;
import android.app.AlertDialog;
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

    public void next(View view) {
        startActivity(new Intent(this, Main2Activity.class));
    }
}
