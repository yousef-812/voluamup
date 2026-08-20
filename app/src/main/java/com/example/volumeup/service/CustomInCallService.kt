package com.example.volumeup.service

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.example.volumeup.ui.incall.InCallActivity

/**
 * Custom InCallService to interface with Android Telecom framework for cellular calls.
 */
class CustomInCallService : InCallService() {

    companion object {
        private const val TAG = "CustomInCallService"
        var currentCall: Call? = null
            private set

        var activeService: CustomInCallService? = null
            private set
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call added: ${call.details?.handle}")
        currentCall = call
        activeService = this

        call.registerCallback(callCallback)

        // Launch In-Call Activity UI
        val intent = Intent(this, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed: ${call.details?.handle}")
        call.unregisterCallback(callCallback)
        if (currentCall == call) {
            currentCall = null
        }
        activeService = null
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            Log.d(TAG, "Call state changed: $state")
            if (state == Call.STATE_DISCONNECTED) {
                currentCall = null
            }
        }
    }
}
