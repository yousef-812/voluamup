package com.bdaey.voluamup.ui.incall

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Call
import android.telecom.DisconnectCause
import android.view.View
import android.view.WindowManager
import android.widget.Toast
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
    private var proximityWakeLock: PowerManager.WakeLock? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var isBoosterActive: Boolean = true
    private var isFinishing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyWindowFlags()
        binding = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioBoosterManager = AudioBoosterManager(this)
        initWakeLocks()
        initVibrator()
        setupUI()
        setupDtmfKeypad()
        observeCallData()
        observeCallState()
    }

    // ─── Window / Lockscreen flags ────────────────────────────────────────────

    private fun applyWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }

    // ─── Wake locks ───────────────────────────────────────────────────────────

    private fun initWakeLocks() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            proximityWakeLock = pm.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "VoluamUp::ProximityWakeLock"
            )
        }
    }

    private fun acquireProximityWakeLock() {
        if (proximityWakeLock?.isHeld == false) {
            proximityWakeLock?.acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseProximityWakeLock() {
        if (proximityWakeLock?.isHeld == true) {
            proximityWakeLock?.release()
        }
    }

    // ─── Vibrator ─────────────────────────────────────────────────────────────

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // ─── Ringtone + Vibration ─────────────────────────────────────────────────

    private fun startRingtoneAndVibration() {
        if (ringtone == null) {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)
        }
        if (ringtone?.isPlaying == false) {
            ringtone?.play()
        }

        val pattern = longArrayOf(0, 1000, 800)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 1)
        }
    }

    private fun stopRingtoneAndVibration() {
        if (ringtone?.isPlaying == true) ringtone?.stop()
        vibrator?.cancel()
    }

    // ─── UI Setup ─────────────────────────────────────────────────────────────

    private fun setupUI() {
        updateBoosterState(true)

        binding.btnToggleBooster.setOnClickListener {
            updateBoosterState(!isBoosterActive)
        }

        binding.btnToggleKeypad.setOnClickListener {
            binding.layoutDtmfKeypad.visibility =
                if (binding.layoutDtmfKeypad.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.btnAddCall.setOnClickListener {
            CallManager.holdCall()
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
        }

        binding.btnToggleSpeaker.setOnClickListener {
            CustomInCallService.instance?.let { CallManager.toggleSpeaker(it) }
        }

        binding.btnToggleMute.setOnClickListener {
            CustomInCallService.instance?.let { CallManager.toggleMute(it) }
        }

        binding.btnEndCall.setOnClickListener {
            stopRingtoneAndVibration()
            CallManager.disconnectCall()
        }

        binding.btnAnswerCall.setOnClickListener {
            stopRingtoneAndVibration()
            dismissKeyguard()
            CallManager.answerCall()
        }

        binding.btnRejectCall.setOnClickListener {
            stopRingtoneAndVibration()
            CallManager.disconnectCall()
        }
    }

    private fun dismissKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)
                ?.requestDismissKeyguard(this, null)
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
            binding.btnDtmf0 to '0', binding.btnDtmf1 to '1',
            binding.btnDtmf2 to '2', binding.btnDtmf3 to '3',
            binding.btnDtmf4 to '4', binding.btnDtmf5 to '5',
            binding.btnDtmf6 to '6', binding.btnDtmf7 to '7',
            binding.btnDtmf8 to '8', binding.btnDtmf9 to '9',
            binding.btnDtmfStar to '*', binding.btnDtmfHash to '#'
        )
        for ((btn, digit) in dtmfMap) {
            btn.setOnClickListener {
                CallManager.playDtmf(digit)
                btn.postDelayed({ CallManager.stopDtmf() }, 200)
            }
        }
    }

    // ─── Observers ────────────────────────────────────────────────────────────

    /** Observes caller name / number and populates header text views. */
    private fun observeCallData() {
        lifecycleScope.launch {
            CallManager.currentCall.collectLatest { call ->
                if (call != null) {
                    val number = call.details?.handle?.schemeSpecificPart ?: ""
                    val name   = call.details?.callerDisplayName
                    when {
                        !name.isNullOrEmpty() -> {
                            binding.tvInCallName.text = name
                            binding.tvInCallNumber.text = number
                        }
                        number.isNotEmpty() -> {
                            binding.tvInCallName.text = number
                            binding.tvInCallNumber.text = ""
                        }
                        else -> {
                            binding.tvInCallName.text = getString(R.string.unknown_caller)
                            binding.tvInCallNumber.text = ""
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            CallManager.isMuted.collectLatest { muted ->
                binding.btnToggleMute.text =
                    if (muted) getString(R.string.unmute) else getString(R.string.mute)
            }
        }

        lifecycleScope.launch {
            CallManager.isSpeakerphoneOn.collectLatest { speaker ->
                binding.btnToggleSpeaker.text =
                    if (speaker) getString(R.string.route_earpiece) else getString(R.string.route_speaker)
            }
        }
    }

    /**
     * Observes call state and updates UI for every possible state:
     * RINGING → CONNECTING → DIALING → ACTIVE → HOLDING → DISCONNECTED
     */
    private fun observeCallState() {
        lifecycleScope.launch {
            CallManager.callState.collectLatest { state ->
                when (state) {

                    // ── Incoming call ─────────────────────────────────────
                    Call.STATE_RINGING -> {
                        binding.tvInCallStatus.text = getString(R.string.call_status_ringing)
                        binding.layoutIncomingCallActions.visibility = View.VISIBLE
                        binding.layoutActiveCallContainer.visibility = View.GONE
                        startRingtoneAndVibration()
                    }

                    // ── Outgoing – waiting for network ────────────────────
                    Call.STATE_CONNECTING -> {
                        stopRingtoneAndVibration()
                        binding.tvInCallStatus.text = getString(R.string.call_status_connecting)
                        binding.layoutIncomingCallActions.visibility = View.GONE
                        binding.layoutActiveCallContainer.visibility = View.VISIBLE
                    }

                    // ── Outgoing – remote phone ringing ───────────────────
                    Call.STATE_DIALING -> {
                        stopRingtoneAndVibration()
                        binding.tvInCallStatus.text = getString(R.string.call_status_dialing)
                        binding.layoutIncomingCallActions.visibility = View.GONE
                        binding.layoutActiveCallContainer.visibility = View.VISIBLE
                    }

                    // ── Call connected ────────────────────────────────────
                    Call.STATE_ACTIVE -> {
                        stopRingtoneAndVibration()
                        binding.tvInCallStatus.text = getString(R.string.call_status_active)
                        binding.layoutIncomingCallActions.visibility = View.GONE
                        binding.layoutActiveCallContainer.visibility = View.VISIBLE
                        if (isBoosterActive) audioBoosterManager.enableBooster()
                        acquireProximityWakeLock()
                    }

                    // ── On hold ───────────────────────────────────────────
                    Call.STATE_HOLDING -> {
                        stopRingtoneAndVibration()
                        binding.tvInCallStatus.text = getString(R.string.call_status_holding)
                        binding.layoutIncomingCallActions.visibility = View.GONE
                        binding.layoutActiveCallContainer.visibility = View.VISIBLE
                    }

                    // ── Call ended ────────────────────────────────────────
                    Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                        handleDisconnect()
                    }

                    else -> {
                        binding.layoutIncomingCallActions.visibility = View.GONE
                        binding.layoutActiveCallContainer.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    /**
     * Handles call disconnect with specific reason labels:
     * BUSY → "الخط مشغول"  |  NO_ANSWER → "لم يتم الرد"  |  REJECTED → "تم رفض المكالمة"
     */
    private fun handleDisconnect() {
        if (isFinishing) return
        isFinishing = true

        stopRingtoneAndVibration()
        releaseProximityWakeLock()
        audioBoosterManager.release()

        val causeCode = CallManager.disconnectCause.value
        val (statusRes, toastRes) = when (causeCode) {
            DisconnectCause.BUSY      -> R.string.call_status_busy      to R.string.call_status_busy
            DisconnectCause.NO_ANSWER -> R.string.call_status_no_answer to R.string.call_status_no_answer
            DisconnectCause.REJECTED  -> R.string.call_status_rejected  to R.string.call_status_rejected
            DisconnectCause.MISSED    -> R.string.call_status_missed    to null
            DisconnectCause.ERROR     -> R.string.call_status_failed    to null
            DisconnectCause.CANCELED  -> R.string.call_status_canceled  to null
            else                      -> R.string.call_status_ended     to null
        }

        binding.tvInCallStatus.text = getString(statusRes)
        binding.layoutIncomingCallActions.visibility = View.GONE
        binding.layoutActiveCallContainer.visibility = View.GONE

        // Show toast for noteworthy disconnect reasons
        if (toastRes != null) {
            Toast.makeText(this, getString(toastRes), Toast.LENGTH_SHORT).show()
        }

        // Brief delay so user can read the status before screen closes
        binding.root.postDelayed({ finish() }, 1800)
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        stopRingtoneAndVibration()
        releaseProximityWakeLock()
        audioBoosterManager.release()
    }
}

