package com.bdaey.voluamup.service

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.bdaey.voluamup.ui.incall.InCallActivity

class CustomInCallService : InCallService() {

    companion object {
        private const val TAG = "CustomInCallService"
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call added: ${call.details?.handle}")
        CallManager.activeService = this
        CallManager.onCallAdded(call)

        val intent = Intent(this, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed: ${call.details?.handle}")
        CallManager.onCallRemoved(call)
        if (CallManager.currentCallState.value == null) {
            CallManager.activeService = null
        }
    }
}
