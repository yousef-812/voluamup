package com.bdaey.voluamup.ui.incall

import android.content.Context
import android.content.Intent
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
        binding.sliderVolumeBoost.valueFrom = 100f
        binding.sliderVolumeBoost.valueTo = 200f
        binding.sliderVolumeBoost.value = 200f

        audioBoosterManager.setBoostPercentage(200)

        binding.switchBoostEnable.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val boost = binding.sliderVolumeBoost.value.toInt()
                audioBoosterManager.setBoostPercentage(boost)
                binding.tvBoostPercentage.text = "$boost%"
            } else {
                audioBoosterManager.applyVolumeBoost(100)
                binding.tvBoostPercentage.text = getString(R.string.boost_disabled)
            }
        }

        binding.sliderVolumeBoost.addOnChangeListener { _: Slider, value: Float, _: Boolean ->
            val boost = value.toInt()
            binding.tvBoostPercentage.text = "$boost%"
            if (binding.switchBoostEnable.isChecked) {
                audioBoosterManager.setBoostPercentage(boost)
            }
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
            CallManager.disconnectCall()
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

    private fun observeCallState() {
        lifecycleScope.launch {
            CallManager.callState.collectLatest { state ->
                when (state) {
                    Call.STATE_ACTIVE -> {
                        binding.tvInCallStatus.text = getString(R.string.in_call_title)
                        if (binding.switchBoostEnable.isChecked) {
                            audioBoosterManager.enableBooster()
                        }
                        acquireWakeLock()
                    }
                    Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                        binding.tvInCallStatus.text = getString(R.string.end_call)
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
