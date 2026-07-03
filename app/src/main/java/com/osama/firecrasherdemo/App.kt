package com.osama.firecrasherdemo

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.osama.firecrasher.RecoveryLevel
import com.osama.firecrasher.installFireCrasher


/**
 * Created by Osama Raddad.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        installFireCrasher {
            onCrash {
                //you need to add your crash reporting tool here
                //Ex: Crashlytics.logException(throwable);

                val context = activity ?: return@onCrash
                if (retryCount <= 1 && level == RecoveryLevel.RESTART_ACTIVITY) {
                    showProgressDialog(context)
                    recover {
                        Toast.makeText(this@App, "recover", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val title = when (level) {
                        RecoveryLevel.RESTART_ACTIVITY -> "Restart the screen?"
                        RecoveryLevel.GO_BACK -> "Go back?"
                        RecoveryLevel.RELAUNCH_APP -> "Restart the app?"
                    }

                    val positiveButtonText = when (level) {
                        RecoveryLevel.RESTART_ACTIVITY -> "Retry"
                        RecoveryLevel.GO_BACK -> "Go Back"
                        RecoveryLevel.RELAUNCH_APP -> "Restart The App"
                    }
                    val builder = MaterialAlertDialogBuilder(context)
                    builder.setTitle(title)
                    builder.setMessage(throwable.localizedMessage)
                    builder.setPositiveButton(positiveButtonText) { _, _ ->
                        recover {
                            Toast.makeText(this@App, "recover", Toast.LENGTH_LONG).show()
                        }
                    }
                    if (level != RecoveryLevel.RELAUNCH_APP)
                        builder.setNegativeButton("Restart The App") { _, _ ->
                            recover(RecoveryLevel.RELAUNCH_APP) {
                                Toast.makeText(this@App, "recover", Toast.LENGTH_LONG).show()
                            }
                        }
                    builder.setIcon(android.R.drawable.ic_dialog_alert)
                    builder.show()
                }
            }

            onPreviousProcessExit { exitInfo ->
                // Only ever invoked on API 30+, but lint can't see that
                // guarantee through the lambda, so guard for it.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Reaches your reporter even for native crashes and ANRs.
                    Log.i("FireCrasherDemo", "previous exit: ${exitInfo.reason} ${exitInfo.description}")
                }
            }
        }
    }

    private fun showProgressDialog(context: Context) {
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
    }
}
