package com.bdaey.voluamup.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bdaey.voluamup.R
import com.bdaey.voluamup.ui.incall.InCallActivity

class CustomInCallService : InCallService() {

    companion object {
        private const val TAG = "CustomInCallService"
        private const val CHANNEL_INCALL_ID  = "incall_channel"
        private const val CHANNEL_RINGING_ID = "ringing_channel"
        private const val NOTIF_ID_CALL      = 1001

        var instance: CustomInCallService? = null
    }

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
    }

    // ─── Notification Channels ────────────────────────────────────────────────

    private fun createNotificationChannels() {
        // High-priority channel for incoming calls (ringtone + headsup)
        val ringingChannel = NotificationChannel(
            CHANNEL_RINGING_ID,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming phone call alerts"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        // Lower-priority channel for active/ongoing call indicator
        val inCallChannel = NotificationChannel(
            CHANNEL_INCALL_ID,
            "Active Call",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing call status"
        }

        notificationManager.createNotificationChannel(ringingChannel)
        notificationManager.createNotificationChannel(inCallChannel)
    }

    // ─── Call lifecycle ───────────────────────────────────────────────────────

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call added: state=${call.state} handle=${call.details?.handle}")
        instance = this
        CallManager.setCall(call)

        val callerNumber = call.details?.handle?.schemeSpecificPart ?: ""
        val callerName   = call.details?.callerDisplayName?.takeIf { it.isNotEmpty() }
            ?: callerNumber.ifEmpty { "Unknown" }

        when (call.state) {
            Call.STATE_RINGING -> {
                // Show full-screen incoming call notification (wakes lockscreen)
                showIncomingCallNotification(callerName, callerNumber)
            }
            else -> {
                // Outgoing / active call - open in-call screen directly
                showActiveCallNotification(callerName)
                launchInCallActivity()
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed: ${call.details?.handle}")
        CallManager.setCall(null)
        notificationManager.cancel(NOTIF_ID_CALL)
        if (instance == this) instance = null
    }

    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        audioState?.let {
            CallManager.updateAudioState(it.isMuted, it.route == CallAudioState.ROUTE_SPEAKER)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.cancel(NOTIF_ID_CALL)
        if (instance == this) instance = null
    }

    // ─── Notification builders ────────────────────────────────────────────────

    /**
     * Shows a full-screen CATEGORY_CALL notification on the lockscreen.
     * This is what makes the phone "ring" visibly to the user instead of
     * appearing switched off / unavailable to the caller.
     */
    private fun showIncomingCallNotification(callerName: String, callerNumber: String) {
        // Full-screen intent → opens InCallActivity on lockscreen
        val fullScreenIntent = Intent(this, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Answer action
        val answerIntent = Intent(CallActionReceiver.ACTION_ANSWER_CALL).apply {
            setPackage(packageName)
        }
        val answerPi = PendingIntent.getBroadcast(
            this, 1, answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline action
        val declineIntent = Intent(CallActionReceiver.ACTION_DECLINE_CALL).apply {
            setPackage(packageName)
        }
        val declinePi = PendingIntent.getBroadcast(
            this, 2, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_RINGING_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Incoming Call")
            .setContentText("$callerName${if (callerNumber.isNotEmpty() && callerNumber != callerName) " • $callerNumber" else ""}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(60_000) // auto-dismiss after 60 s
            // Full-screen intent wakes the screen
            .setFullScreenIntent(fullScreenPendingIntent, true)
            // Notification action buttons
            .addAction(android.R.drawable.ic_menu_call, "Answer", answerPi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declinePi)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        // FLAG_INSISTENT makes the notification ring repeatedly
        notification.flags = notification.flags or Notification.FLAG_INSISTENT

        notificationManager.notify(NOTIF_ID_CALL, notification)

        // Also open InCallActivity immediately (shows on top of lockscreen)
        launchInCallActivity()
    }

    private fun showActiveCallNotification(callerName: String) {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_INCALL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Active Call")
            .setContentText(callerName)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()

        notificationManager.notify(NOTIF_ID_CALL, notification)
    }

    private fun launchInCallActivity() {
        val intent = Intent(this, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)
    }
}

