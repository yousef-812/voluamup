package com.example.volumeup.audio

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log

/**
 * Advanced Audio Booster Manager:
 * Handles 200% LoudnessEnhancer gain boost, Equalizer speech clarity enhancement (1kHz-3kHz),
 * and AudioManager voice call stream scaling.
 */
class AudioBoosterManager(private val context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var isBoostEnabled = false

    companion object {
        private const val TAG = "AudioBoosterManager"
        // Target gain range: 0 mB (+0dB) at 100% -> 600 mB (+6dB) at 200% volume
        private const val MAX_GAIN_MB = 600
    }

    init {
        initAudioEffects()
    }

    private fun initAudioEffects() {
        try {
            // Audio Session 0 applies to global audio output pipeline
            loudnessEnhancer = LoudnessEnhancer(0).apply {
                enabled = true
            }
            Log.d(TAG, "LoudnessEnhancer initialized.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LoudnessEnhancer: ${e.message}", e)
        }

        try {
            equalizer = Equalizer(0, 0).apply {
                enabled = true
                // Boost human voice clarity band (~1000Hz - ~3000Hz) if supported
                val numberOfBands = numberOfBands
                val bandRange = bandLevelRange
                val maxLevel = bandRange[1]
                for (b in 0 until numberOfBands) {
                    val freq = getCenterFreq(b.toShort()) / 1000 // In Hz
                    if (freq in 1000..3500) {
                        setBandLevel(b.toShort(), (maxLevel * 0.75).toInt().toShort())
                    }
                }
            }
            Log.d(TAG, "Equalizer Speech Clarity initialized.")
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer initialization skipped: ${e.message}")
        }
    }

    /**
     * Sets STREAM_VOICE_CALL to max and applies digital mB gain.
     * @param boostPercentage 100 to 200 percentage value.
     */
    fun applyVolumeBoost(boostPercentage: Int) {
        val clampedPercent = boostPercentage.coerceIn(100, 200)

        // 1. Maximize physical STREAM_VOICE_CALL
        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Could not set max volume for STREAM_VOICE_CALL: ${e.message}")
        }

        // 2. Apply digital gain boost
        val gainMilliBel = ((clampedPercent - 100) / 100.0 * MAX_GAIN_MB).toInt()

        try {
            if (loudnessEnhancer == null) {
                initAudioEffects()
            }
            loudnessEnhancer?.apply {
                setTargetGain(gainMilliBel)
                enabled = true
            }
            equalizer?.enabled = true
            isBoostEnabled = true
            Log.d(TAG, "Applied gain boost: $clampedPercent% ($gainMilliBel mB).")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying gain: ${e.message}")
        }
    }

    /**
     * Toggle Speakerphone / Earpiece route.
     */
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

    /**
     * Release audio effects.
     */
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
