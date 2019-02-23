package com.osama.firecrasherdemo

import android.app.Activity
import android.os.Bundle
import android.view.View

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    @Throws(java.lang.Exception::class)
    fun crash(view: View) {
        throw Exception()
    }
}
