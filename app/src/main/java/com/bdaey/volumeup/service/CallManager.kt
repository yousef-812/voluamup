package com.bdaey.volumeup.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CallManager {
    private const val TAG = "CallManager"

    private val _currentCallState = MutableStateFlow<CallStateModel?>(null)
    val currentCallState: StateFlow<CallStateModel?> = _currentCallState.asStateFlow()

    private val _audioState = MutableStateFlow<CallAudioState?>(null)
    val audioState: StateFlow<CallAudioState?> = _audioState.asStateFlow()

    var activeService: CustomInCallService? = null
    private var activeCall: Call? = null

    data class CallStateModel(
        val call: Call,
        val state: Int,
        val phoneNumber: String,
        val displayName: String,
        val connectTimeMillis: Long,
        val canHold: Boolean,
        val isOnHold: Boolean
    )

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            Log.d(TAG, "Call onStateChanged: state=$state")
            updateCallState(call)
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            super.onDetailsChanged(call, details)
            Log.d(TAG, "Call onDetailsChanged")
            updateCallState(call)
        }
    }

    fun onCallAdded(call: Call) {
        Log.d(TAG, "onCallAdded: ${call.details?.handle}")
        activeCall?.unregisterCallback(callCallback)
        activeCall = call
        call.registerCallback(callCallback)
        updateCallState(call)
    }

    fun onCallRemoved(call: Call) {
        Log.d(TAG, "onCallRemoved: ${call.details?.handle}")
        if (activeCall == call) {
            call.unregisterCallback(callCallback)
            activeCall = null
            _currentCallState.value = null
        }
    }

    fun onAudioStateChanged(state: CallAudioState) {
        _audioState.value = state
    }

    private fun updateCallState(call: Call) {
        val details = call.details
        val handle = details?.handle?.schemeSpecificPart ?: ""
        val callerName = details?.callerDisplayName ?: handle
        val connectTime = details?.connectTimeMillis ?: 0L
        val canHold = details?.can(Call.Details.CAPABILITY_HOLD) ?: false
        val isOnHold = call.state == Call.STATE_HOLDING

        _currentCallState.value = CallStateModel(
            call = call,
            state = call.state,
            phoneNumber = handle,
            displayName = callerName.ifEmpty { "مجهول" },
            connectTimeMillis = connectTime,
            canHold = canHold,
            isOnHold = isOnHold
        )
    }

    fun answerCall(videoState: Int = VideoProfile.STATE_AUDIO_ONLY) {
        activeCall?.answer(videoState)
    }

    fun rejectCall(rejectReason: Int = Call.REJECT_REASON_DECLINED) {
        val call = activeCall ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            call.reject(rejectReason)
        } else {
            @Suppress("DEPRECATION")
            call.reject(false, null)
        }
    }

    fun disconnectCall() {
        activeCall?.disconnect()
    }

    fun toggleHold() {
        val call = activeCall ?: return
        if (call.state == Call.STATE_HOLDING) {
            call.unhold()
        } else if (call.state == Call.STATE_ACTIVE) {
            call.hold()
        }
    }

    fun playDtmfTone(digit: Char) {
        activeCall?.playDtmfTone(digit)
    }

    fun stopDtmfTone() {
        activeCall?.stopDtmfTone()
    }

    fun setMuted(muted: Boolean) {
        activeService?.setMuted(muted)
    }

    fun setAudioRoute(route: Int) {
        activeService?.setAudioRoute(route)
    }
}
