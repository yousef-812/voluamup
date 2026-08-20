package com.bdaey.voluamup.audio

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log

class AudioBoosterManager(private val context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var currentBoostPercentage: Int = 200

    companion object {
        private const val TAG = "AudioBoosterManager"
        private const val MAX_GAIN_MB = 2000 // +20dB Gain Boost
    }

    init {
        initAudioEffects()
    }

    private fun initAudioEffects() {
        try {
            loudnessEnhancer = LoudnessEnhancer(0).apply {
                enabled = true
            }
            Log.d(TAG, "LoudnessEnhancer initialized.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LoudnessEnhancer: ${e.message}")
        }

        try {
            equalizer = Equalizer(0, 0).apply {
                enabled = true
                val numberOfBands = numberOfBands
                val bandRange = bandLevelRange
                val maxLevel = bandRange[1]
                for (b in 0 until numberOfBands) {
                    val freq = getCenterFreq(b.toShort()) / 1000
                    if (freq in 1000..3500) {
                        setBandLevel(b.toShort(), maxLevel)
                    }
                }
            }
            Log.d(TAG, "Equalizer Speech Clarity initialized.")
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer initialization skipped: ${e.message}")
        }
    }

    fun enableBooster() {
        applyVolumeBoost(currentBoostPercentage)
    }

    fun applyVolumeBoost(boostPercentage: Int) {
        val clampedPercent = boostPercentage.coerceIn(100, 200)
        currentBoostPercentage = clampedPercent

        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) {
            Log.w(TAG, "Could not set max volume for STREAM_VOICE_CALL: ${e.message}")
        }

        val gainMilliBel = ((clampedPercent - 100) / 100.0 * MAX_GAIN_MB).toInt()

        try {
            if (loudnessEnhancer == null) {
                initAudioEffects()
            }
            loudnessEnhancer?.apply {
                setTargetGain(gainMilliBel)
                enabled = (clampedPercent > 100)
            }
            equalizer?.enabled = (clampedPercent > 100)
            Log.d(TAG, "Applied gain boost: $clampedPercent% ($gainMilliBel mB).")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying gain: ${e.message}")
        }
    }

    fun setSpeakerphoneOn(on: Boolean) {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = on
        } catch (e: Exception) {
            Log.e(TAG, "Error setting speakerphone: ${e.message}")
        }
    }

    fun isSpeakerphoneOn(): Boolean {
        return audioManager.isSpeakerphoneOn
    }

    fun release() {
        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null

            equalizer?.enabled = false
            equalizer?.release()
            equalizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio effects: ${e.message}")
        }
    }
}

