package com.example.volumeup.ui.incall

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.telecom.Call
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.volumeup.R
import com.example.volumeup.audio.AudioBoosterManager
import com.example.volumeup.databinding.ActivityInCallBinding
import com.example.volumeup.service.CustomInCallService

class InCallActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityInCallBinding
    private lateinit var audioBooster: AudioBoosterManager
    private var isMuted = false

    // Proximity Sensor handling
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioBooster = AudioBoosterManager(this)
        setupProximitySensor()

        setupCallInfo()
        setupBoosterSlider()
        setupControls()
    }

    private fun setupProximitySensor() {
        try {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "VolumeUp:ProximityWakeLock"
                )
            }
        } catch (e: Exception) {
            Log.w("InCallActivity", "Proximity sensor setup error: ${e.message}")
        }
    }

    private fun setupCallInfo() {
        val call = CustomInCallService.currentCall
        if (call == null) {
            finish()
            return
        }

        val handle = call.details?.handle?.schemeSpecificPart ?: getString(R.string.in_call_title)
        binding.tvInCallName.text = handle
        binding.tvInCallStatus.text = getString(R.string.in_call_title)

        call.registerCallback(callCallback)
    }

    private fun setupBoosterSlider() {
        // Default to 200% maximum boost upon opening active call screen
        audioBooster.applyVolumeBoost(200)
        binding.sliderVolumeBoost.value = 200f
        binding.tvBoostPercentage.text = "200%"

        binding.sliderVolumeBoost.addOnChangeListener { _, value, _ ->
            val boostPercent = value.toInt()
            binding.tvBoostPercentage.text = "$boostPercent%"
            audioBooster.applyVolumeBoost(boostPercent)
        }
    }

    private fun setupControls() {
        // Toggle Speakerphone vs Earpiece
        binding.btnToggleSpeaker.setOnClickListener {
            val isSpeakerOn = audioBooster.isSpeakerphoneOn()
            val newSpeakerState = !isSpeakerOn
            audioBooster.setSpeakerphoneOn(newSpeakerState)

            if (newSpeakerState) {
                binding.btnToggleSpeaker.setText(R.string.route_earpiece)
            } else {
                binding.btnToggleSpeaker.setText(R.string.route_speaker)
                // Re-apply 200% earpiece volume boost when returning to earpiece mode
                audioBooster.applyVolumeBoost(binding.sliderVolumeBoost.value.toInt())
            }
        }

        // Mute / Unmute Mic
        binding.btnToggleMute.setOnClickListener {
            isMuted = !isMuted
            CustomInCallService.activeService?.setMuted(isMuted)
            if (isMuted) {
                binding.btnToggleMute.setText(R.string.unmute)
            } else {
                binding.btnToggleMute.setText(R.string.mute)
            }
        }

        // End Call
        binding.btnEndCall.setOnClickListener {
            val call = CustomInCallService.currentCall
            call?.disconnect()
            finish()
        }
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            if (state == Call.STATE_DISCONNECTED) {
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        proximitySensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_PROXIMITY) return
        val distance = event.values[0]
        val isNear = distance < (proximitySensor?.maximumRange ?: 5f)

        if (isNear && !audioBooster.isSpeakerphoneOn()) {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(10 * 60 * 1000L /*10 mins*/)
            }
        } else {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        CustomInCallService.currentCall?.unregisterCallback(callCallback)
        audioBooster.release()
    }
}
