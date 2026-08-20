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

    companion object {
        private const val TAG = "AudioBoosterManager"
        private const val MAX_GAIN_MB = 600
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
            Log.e(TAG, "Failed to initialize LoudnessEnhancer: ${e.message}", e)
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
                        setBandLevel(b.toShort(), (maxLevel * 0.75).toInt().toShort())
                    }
                }
            }
            Log.d(TAG, "Equalizer Speech Clarity initialized.")
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer initialization skipped: ${e.message}")
        }
    }

    fun applyVolumeBoost(boostPercentage: Int) {
        val clampedPercent = boostPercentage.coerceIn(100, 200)

        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0)
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
                enabled = true
            }
            equalizer?.enabled = true
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
