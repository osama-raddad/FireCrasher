package com.osama.firecrasherdemo

import android.app.Application
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.osama.firecrasher.CrashLevel
import com.osama.firecrasher.CrashListener
import com.osama.firecrasher.FireCrasher


/**
 * Created by Osama Raddad.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FireCrasher.install(this, object : CrashListener() {

            override fun onCrash(throwable: Throwable) {

                evaluate { activity, crashLevel ->
                    val context = activity ?: return@evaluate
                    if (FireCrasher.retryCount <= 1 && crashLevel == CrashLevel.LEVEL_ONE) {
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
                            .setTitle("loading")
                            .setView(progress)
                            .setCancelable(false)
                            .show()
                        recover {
                            Toast.makeText(this@App, "recover", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val title = when (crashLevel) {
                            CrashLevel.LEVEL_ONE -> "Crash Level One"
                            CrashLevel.LEVEL_TWO -> "Crash Level Two"
                            CrashLevel.LEVEL_THREE -> "Crash Level Three"
                        }

                        val positiveButtonText = when (crashLevel) {
                            CrashLevel.LEVEL_ONE -> "Retry"
                            CrashLevel.LEVEL_TWO -> "Go Back"
                            CrashLevel.LEVEL_THREE -> "Restart The App"
                        }
                        val builder = MaterialAlertDialogBuilder(context)
                        builder.setTitle(title)
                        builder.setMessage(throwable.localizedMessage)
                        builder.setPositiveButton(positiveButtonText) { _, _ ->
                            recover {
                                Toast.makeText(this@App, "recover", Toast.LENGTH_LONG).show()
                            }
                        }
                        if (crashLevel != CrashLevel.LEVEL_THREE)
                            builder.setNegativeButton("Restart The App") { _, _ ->
                                recover(CrashLevel.LEVEL_THREE) {
                                    Toast.makeText(this@App, "recover", Toast.LENGTH_LONG).show()
                                }
                            }
                        builder.setIcon(android.R.drawable.ic_dialog_alert)
                        builder.show()
                    }
                }


                //you need to add your crash reporting tool here
                //Ex: Crashlytics.logException(throwable);
            }
        })
    }
}
