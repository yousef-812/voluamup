package com.bdaey.volumeup.ui.incall

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bdaey.volumeup.R
import com.bdaey.volumeup.audio.AudioBoosterManager
import com.bdaey.volumeup.databinding.ActivityInCallBinding
import com.bdaey.volumeup.service.CallManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class InCallActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityInCallBinding
    private lateinit var audioBooster: AudioBoosterManager
    private var timerJob: Job? = null
    private var isMuted = false

    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenFlags()

        binding = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioBooster = AudioBoosterManager(this)
        setupProximitySensor()

        setupBoosterSlider()
        setupControls()
        observeCallState()
    }

    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
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
            Log.w("InCallActivity", "Proximity sensor error: ${e.message}")
        }
    }

    private fun observeCallState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    CallManager.currentCallState.collect { callState ->
                        if (callState == null || callState.state == Call.STATE_DISCONNECTED) {
                            stopTimer()
                            finish()
                        } else {
                            renderCallState(callState)
                        }
                    }
                }

                launch {
                    CallManager.audioState.collect { audioState ->
                        audioState?.let { renderAudioState(it) }
                    }
                }
            }
        }
    }

    private fun renderCallState(state: CallManager.CallStateModel) {
        binding.tvInCallName.text = state.displayName

        when (state.state) {
            Call.STATE_RINGING -> {
                binding.tvInCallStatus.text = getString(R.string.status_incoming)
                stopTimer()
            }
            Call.STATE_DIALING -> {
                binding.tvInCallStatus.text = getString(R.string.status_dialing)
                stopTimer()
            }
            Call.STATE_CONNECTING -> {
                binding.tvInCallStatus.text = getString(R.string.status_connecting)
                stopTimer()
            }
            Call.STATE_ACTIVE -> {
                startTimer(state.connectTimeMillis)
            }
            Call.STATE_HOLDING -> {
                binding.tvInCallStatus.text = getString(R.string.status_on_hold)
                stopTimer()
            }
        }
    }

    private fun renderAudioState(audioState: CallAudioState) {
        isMuted = audioState.isMuted
        binding.btnToggleMute.setText(if (isMuted) R.string.unmute else R.string.mute)

        val isSpeaker = (audioState.route and CallAudioState.ROUTE_SPEAKER) != 0
        if (isSpeaker) {
            binding.btnToggleSpeaker.setText(R.string.route_earpiece)
        } else {
            binding.btnToggleSpeaker.setText(R.string.route_speaker)
            audioBooster.applyVolumeBoost(binding.sliderVolumeBoost.value.toInt())
        }
    }

    private fun setupBoosterSlider() {
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
        binding.btnToggleSpeaker.setOnClickListener {
            val currentState = CallManager.audioState.value
            val isSpeaker = currentState?.let { (it.route and CallAudioState.ROUTE_SPEAKER) != 0 } ?: false
            val newRoute = if (isSpeaker) CallAudioState.ROUTE_EARPIECE else CallAudioState.ROUTE_SPEAKER
            CallManager.setAudioRoute(newRoute)
        }

        binding.btnToggleMute.setOnClickListener {
            CallManager.setMuted(!isMuted)
        }

        binding.btnEndCall.setOnClickListener {
            CallManager.disconnectCall()
            finish()
        }
    }

    private fun startTimer(connectTimeMillis: Long) {
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            while (isActive) {
                val elapsedSeconds = if (connectTimeMillis > 0) {
                    (System.currentTimeMillis() - connectTimeMillis) / 1000
                } else 0L
                binding.tvInCallStatus.text = formatDuration(elapsedSeconds)
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun formatDuration(totalSeconds: Long): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
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
                wakeLock?.acquire(10 * 60 * 1000L)
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
        stopTimer()
        audioBooster.release()
    }
}
