package com.osama.firecrasherdemo

import android.app.Application
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.osama.firecrasher.CrashEvent
import com.osama.firecrasher.CrashLevel
import com.osama.firecrasher.FireCrasher
import com.osama.firecrasher.FireCrasherConfig


/**
 * Created by Osama Raddad.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = FireCrasherConfig.Builder()
            // Never absorb JVM errors: the "Throw Error" button kills the app
            // normally, exactly as it would without FireCrasher installed.
            .setShouldRecover { it !is Error }
            // The "Crash loop" button trips this and lets the app die instead
            // of recovering forever.
            .setCrashLoopBreaker(5, 30_000L)
            .build()

        FireCrasher.install(this, config) { crash ->
            // Report to your crash reporting tool here, e.g.
            // FirebaseCrashlytics.getInstance().recordException(crash.throwable)
            showRecoveryUx(crash)
        }

        // The pre-2.2.0 CrashListener API still works unchanged:
        //
        // FireCrasher.install(this, object : CrashListener() {
        //     override fun onCrash(throwable: Throwable) {
        //         recover()
        //     }
        // })
    }

    private fun showRecoveryUx(crash: CrashEvent) {
        val context = crash.activity ?: return
        if (crash.retryCount <= 1 && crash.suggestedLevel == CrashLevel.LEVEL_ONE) {
            val padding = (24 * context.resources.displayMetrics.density).toInt()
            val progress = FrameLayout(context).apply {
                setPadding(padding, padding, padding, padding)
                addView(
                    CircularProgressIndicator(context).apply { isIndeterminate = true },
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                )
            }
            MaterialAlertDialogBuilder(context)
                .setTitle("Recovering…")
                .setView(progress)
                .setCancelable(false)
                .show()
            crash.recover()
            Toast.makeText(this, "Recovered on this screen", Toast.LENGTH_LONG).show()
        } else {
            val title = when (crash.suggestedLevel) {
                CrashLevel.LEVEL_ONE -> "Crash Level One"
                CrashLevel.LEVEL_TWO -> "Crash Level Two"
                CrashLevel.LEVEL_THREE -> "Crash Level Three"
            }

            val positiveButtonText = when (crash.suggestedLevel) {
                CrashLevel.LEVEL_ONE -> "Retry"
                CrashLevel.LEVEL_TWO -> "Go Back"
                CrashLevel.LEVEL_THREE -> "Restart The App"
            }
            val builder = MaterialAlertDialogBuilder(context)
            builder.setTitle(title)
            builder.setMessage(crash.throwable.localizedMessage)
            builder.setPositiveButton(positiveButtonText) { _, _ ->
                crash.recover()
                Toast.makeText(this, "Recovered", Toast.LENGTH_LONG).show()
            }
            if (crash.suggestedLevel != CrashLevel.LEVEL_THREE)
                builder.setNegativeButton("Restart The App") { _, _ ->
                    crash.recover(CrashLevel.LEVEL_THREE)
                    Toast.makeText(this, "Restarted the app", Toast.LENGTH_LONG).show()
                }
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.show()
        }
    }
}
