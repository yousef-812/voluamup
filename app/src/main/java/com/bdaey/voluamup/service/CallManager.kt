package com.bdaey.voluamup.service

import android.telecom.Call
import android.telecom.CallAudioState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CallManager {

    private val _currentCall = MutableStateFlow<Call?>(null)
    val currentCall: StateFlow<Call?> = _currentCall.asStateFlow()

    private val _callState = MutableStateFlow(Call.STATE_DISCONNECTED)
    val callState: StateFlow<Int> = _callState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerphoneOn = MutableStateFlow(false)
    val isSpeakerphoneOn: StateFlow<Boolean> = _isSpeakerphoneOn.asStateFlow()

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            _callState.value = state
        }

        @Deprecated("Deprecated in Java")
        override fun onCallAudioStateChanged(call: Call, audioState: CallAudioState) {
            super.onCallAudioStateChanged(call, audioState)
            _isMuted.value = audioState.isMuted
            _isSpeakerphoneOn.value = audioState.route == CallAudioState.ROUTE_SPEAKER
        }
    }

    fun setCall(call: Call?) {
        _currentCall.value?.unregisterCallback(callCallback)
        _currentCall.value = call
        if (call != null) {
            call.registerCallback(callCallback)
            _callState.value = call.state
        } else {
            _callState.value = Call.STATE_DISCONNECTED
        }
    }

    fun answerCall() {
        _currentCall.value?.answer(0)
    }

    fun disconnectCall() {
        _currentCall.value?.disconnect()
    }

    fun toggleMute(service: CustomInCallService) {
        val newMuteState = !_isMuted.value
        service.setMuted(newMuteState)
        _isMuted.value = newMuteState
    }

    fun toggleSpeaker(service: CustomInCallService) {
        val newRoute = if (_isSpeakerphoneOn.value) {
            CallAudioState.ROUTE_EARPIECE
        } else {
            CallAudioState.ROUTE_SPEAKER
        }
        service.setAudioRoute(newRoute)
        _isSpeakerphoneOn.value = (newRoute == CallAudioState.ROUTE_SPEAKER)
    }
}
