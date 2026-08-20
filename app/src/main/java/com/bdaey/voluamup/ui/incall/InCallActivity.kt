package com.bdaey.voluamup.ui.incall

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.PowerManager
import android.telecom.Call
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bdaey.voluamup.R
import com.bdaey.voluamup.audio.AudioBoosterManager
import com.bdaey.voluamup.databinding.ActivityInCallBinding
import com.bdaey.voluamup.service.CallManager
import com.bdaey.voluamup.service.CustomInCallService
import com.bdaey.voluamup.ui.main.MainActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInCallBinding
    private lateinit var audioBoosterManager: AudioBoosterManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var ringtone: Ringtone? = null
    private var isBoosterActive: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioBoosterManager = AudioBoosterManager(this)
        setupProximitySensor()
        setupUI()
        setupDtmfKeypad()
        observeCallState()
    }

    private fun setupProximitySensor() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "VoluamUp::ProximityWakeLock"
            )
        }
    }

    private fun setupUI() {
        updateBoosterState(true)

        binding.btnToggleBooster.setOnClickListener {
            updateBoosterState(!isBoosterActive)
        }

        binding.btnToggleKeypad.setOnClickListener {
            if (binding.layoutDtmfKeypad.visibility == View.VISIBLE) {
                binding.layoutDtmfKeypad.visibility = View.GONE
            } else {
                binding.layoutDtmfKeypad.visibility = View.VISIBLE
            }
        }

        binding.btnAddCall.setOnClickListener {
            CallManager.holdCall()
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }

        binding.btnToggleSpeaker.setOnClickListener {
            CustomInCallService.instance?.let { service ->
                CallManager.toggleSpeaker(service)
            }
        }

        binding.btnToggleMute.setOnClickListener {
            CustomInCallService.instance?.let { service ->
                CallManager.toggleMute(service)
            }
        }

        binding.btnEndCall.setOnClickListener {
            stopRingtone()
            CallManager.disconnectCall()
        }

        binding.btnAnswerCall.setOnClickListener {
            stopRingtone()
            CallManager.answerCall()
        }

        binding.btnRejectCall.setOnClickListener {
            stopRingtone()
            CallManager.disconnectCall()
            finish()
        }
    }

    private fun updateBoosterState(active: Boolean) {
        isBoosterActive = active
        if (active) {
            audioBoosterManager.applyVolumeBoost(200)
            binding.btnToggleBooster.text = getString(R.string.earpiece_booster_on)
            binding.btnToggleBooster.setStrokeColorResource(R.color.boost_gold)
        } else {
            audioBoosterManager.applyVolumeBoost(100)
            binding.btnToggleBooster.text = getString(R.string.earpiece_booster_off)
            binding.btnToggleBooster.setStrokeColorResource(R.color.surface_dark)
        }
    }

    private fun setupDtmfKeypad() {
        val dtmfMap = mapOf(
            binding.btnDtmf0 to '0',
            binding.btnDtmf1 to '1',
            binding.btnDtmf2 to '2',
            binding.btnDtmf3 to '3',
            binding.btnDtmf4 to '4',
            binding.btnDtmf5 to '5',
            binding.btnDtmf6 to '6',
            binding.btnDtmf7 to '7',
            binding.btnDtmf8 to '8',
            binding.btnDtmf9 to '9',
            binding.btnDtmfStar to '*',
            binding.btnDtmfHash to '#'
        )

        for ((btn, digit) in dtmfMap) {
            btn.setOnClickListener {
                CallManager.playDtmf(digit)
                btn.postDelayed({ CallManager.stopDtmf() }, 200)
            }
        }
    }

    private fun startRingtone() {
        if (ringtone == null) {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
        }
        if (ringtone?.isPlaying == false) {
            ringtone?.play()
        }
    }

    private fun stopRingtone() {
        if (ringtone?.isPlaying == true) {
            ringtone?.stop()
        }
    }

    private fun observeCallState() {
        lifecycleScope.launch {
            CallManager.currentCall.collectLatest { call ->
                if (call != null) {
                    val handle = call.details?.handle
                    val phoneNumber = handle?.schemeSpecificPart ?: ""
                    val callerName = call.details?.callerDisplayName

                    if (!callerName.isNullOrEmpty()) {
                        binding.tvInCallName.text = callerName
                        binding.tvInCallNumber.text = phoneNumber
                    } else if (phoneNumber.isNotEmpty()) {
                        binding.tvInCallName.text = phoneNumber
                        binding.tvInCallNumber.text = ""
                    } else {
                        binding.tvInCallName.text = getString(R.string.unknown_caller)
                        binding.tvInCallNumber.text = ""
                    }
                }
            }
        }

        lifecycleScope.launch {
            CallManager.callState.collectLatest { state ->
                when (state) {
                    Call.STATE_RINGING -> {
                        binding.tvInCallStatus.text = getString(R.string.incoming_call_title)
                        binding.layoutIncomingCallActions.visibility = View.VISIBLE
                        binding.layoutActiveCallContainer.visibility = View.GONE
                        startRingtone()
                    }
                    Call.STATE_ACTIVE -> {
                        stopRingtone()
                        binding.tvInCallStatus.text = getString(R.string.in_call_title)
                        binding.layoutIncomingCallActions.visibility = View.GONE
                        binding.layoutActiveCallContainer.visibility = View.VISIBLE
                        if (isBoosterActive) {
                            audioBoosterManager.enableBooster()
                        }
                        acquireWakeLock()
                    }
                    Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                        stopRingtone()
                        binding.tvInCallStatus.text = getString(R.string.end_call)
                        audioBoosterManager.release()
                        releaseWakeLock()
                        finish()
                    }
                    else -> {
                        binding.layoutIncomingCallActions.visibility = View.GONE
                        binding.layoutActiveCallContainer.visibility = View.VISIBLE
                    }
                }
            }
        }

        lifecycleScope.launch {
            CallManager.isMuted.collectLatest { isMuted ->
                binding.btnToggleMute.text = if (isMuted) getString(R.string.unmute) else getString(R.string.mute)
            }
        }

        lifecycleScope.launch {
            CallManager.isSpeakerphoneOn.collectLatest { isSpeakerOn ->
                binding.btnToggleSpeaker.text = if (isSpeakerOn) getString(R.string.route_earpiece) else getString(R.string.route_speaker)
            }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(10 * 60 * 1000L /* 10 mins timeout */)
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
        releaseWakeLock()
        audioBoosterManager.release()
    }
}
