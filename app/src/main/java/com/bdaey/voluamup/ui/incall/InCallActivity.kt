package com.bdaey.voluamup.ui.incall

import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.telecom.Call
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bdaey.voluamup.R
import com.bdaey.voluamup.audio.AudioBoosterManager
import com.bdaey.voluamup.databinding.ActivityInCallBinding
import com.bdaey.voluamup.service.CallManager
import com.bdaey.voluamup.service.CustomInCallService
import com.google.android.material.slider.Slider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInCallBinding
    private lateinit var audioBoosterManager: AudioBoosterManager
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioBoosterManager = AudioBoosterManager(this)
        setupProximitySensor()
        setupUI()
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
        binding.sliderVolumeBoost.valueFrom = 100f
        binding.sliderVolumeBoost.valueTo = 200f
        binding.sliderVolumeBoost.value = 200f

        audioBoosterManager.setBoostPercentage(200)

        binding.sliderVolumeBoost.addOnChangeListener { _: Slider, value: Float, _: Boolean ->
            val boost = value.toInt()
            binding.tvBoostPercentage.text = "$boost%"
            audioBoosterManager.setBoostPercentage(boost)
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
            CallManager.disconnectCall()
        }
    }

    private fun observeCallState() {
        lifecycleScope.launch {
            CallManager.callState.collectLatest { state ->
                when (state) {
                    Call.STATE_ACTIVE -> {
                        binding.chronometerCallTimer.base = SystemClock.elapsedRealtime()
                        binding.chronometerCallTimer.start()
                        audioBoosterManager.enableBooster()
                        acquireWakeLock()
                    }
                    Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                        binding.chronometerCallTimer.stop()
                        audioBoosterManager.release()
                        releaseWakeLock()
                        finish()
                    }
                    else -> {}
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
        releaseWakeLock()
        audioBoosterManager.release()
    }
}
