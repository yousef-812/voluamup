package com.bdaey.voluamup.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Handles incoming call Answer / Decline actions triggered from the
 * full-screen / lockscreen notification shown by CustomInCallService.
 */
class CallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ANSWER_CALL  = "com.bdaey.voluamup.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "com.bdaey.voluamup.ACTION_DECLINE_CALL"
        private const val TAG = "CallActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received action: ${intent.action}")
        when (intent.action) {
            ACTION_ANSWER_CALL -> {
                CallManager.answerCall()
                // Bring InCallActivity to foreground
                val activityIntent = Intent(context, Class.forName("com.bdaey.voluamup.ui.incall.InCallActivity")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                context.startActivity(activityIntent)
            }
            ACTION_DECLINE_CALL -> {
                CallManager.disconnectCall()
            }
        }
    }
}
