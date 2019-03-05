package com.osama.firecrasherdemo

import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main2.*

class Main2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
    }


    override fun onResume() {
        super.onResume()
        Handler().postDelayed({
            this.runOnUiThread {
                throw Exception("Osama ex")
            }
        }, 5000)

    }

}
