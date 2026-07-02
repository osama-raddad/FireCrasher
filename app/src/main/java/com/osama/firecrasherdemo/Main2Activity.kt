package com.osama.firecrasherdemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class Main2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
    }


    override fun onResume() {
        super.onResume()
        Handler(Looper.getMainLooper()).postDelayed({
            this.runOnUiThread {
                throw Exception("Osama ex")
            }
        }, 5000)

    }

}
