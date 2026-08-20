package com.bdaey.voluamup.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.bdaey.voluamup.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object PermissionHelper {

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    fun showRationaleDialog(
        activity: Activity,
        title: String,
        message: String,
        onPositive: () -> Unit,
        onNegative: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.btn_allow) { _, _ -> onPositive() }
            .setNegativeButton(R.string.btn_cancel) { _, _ -> onNegative?.invoke() }
            .setCancelable(false)
            .show()
    }

    fun showSettingsRedirectDialog(
        activity: Activity,
        title: String,
        message: String
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.btn_open_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                activity.startActivity(intent)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}
