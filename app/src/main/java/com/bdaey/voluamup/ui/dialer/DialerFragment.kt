package com.bdaey.voluamup.ui.dialer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bdaey.voluamup.databinding.FragmentDialerBinding
import com.bdaey.voluamup.util.PermissionHelper

class DialerFragment : Fragment() {

    private var _binding: FragmentDialerBinding? = null
    private val binding get() = _binding!!

    private var phoneAccountHandles: List<PhoneAccountHandle> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupKeypad()
        loadSimAccounts()
    }

    /**
     * Reads actual SIM subscriptions from SubscriptionManager.
     * - Shows each Chip only if that SIM slot is actually occupied.
     * - Displays the real phone number next to the SIM label (if available).
     * - Hides the entire ChipGroup when the device has ≤1 SIM.
     */
    @SuppressLint("MissingPermission")
    private fun loadSimAccounts() {
        val context = requireContext()

        // Load TelecomManager phone account handles (used when placing calls)
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null &&
                PermissionHelper.hasPermission(context, Manifest.permission.READ_PHONE_STATE)
            ) {
                phoneAccountHandles = telecomManager.callCapablePhoneAccounts
            }
        } catch (e: Exception) {
            Log.w("DialerFragment", "Could not query phone accounts: ${e.message}")
        }

        // Read SIM subscription info to get real labels & numbers
        val subscriptions = try {
            if (PermissionHelper.hasPermission(context, Manifest.permission.READ_PHONE_STATE)) {
                val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                        as? SubscriptionManager
                subManager?.activeSubscriptionInfoList ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.w("DialerFragment", "Could not read subscription info: ${e.message}")
            emptyList()
        }

        val simCount = subscriptions.size

        when {
            simCount <= 1 -> {
                // Single-SIM or no SIM — hide the entire selector
                binding.cgSimSelector.visibility = View.GONE
            }
            simCount == 2 -> {
                // Dual-SIM: show both chips with real number
                binding.cgSimSelector.visibility = View.VISIBLE
                binding.chipSim1.visibility = View.VISIBLE
                binding.chipSim2.visibility = View.VISIBLE

                val sub1 = subscriptions[0]
                val sub2 = subscriptions[1]

                // Build label: "شريحة 1 • 01XXXXXXXXX" — fallback to slot index if no number
                val label1 = buildSimLabel(1, sub1.number)
                val label2 = buildSimLabel(2, sub2.number)

                binding.chipSim1.text = label1
                binding.chipSim2.text = label2
                binding.chipSim1.isChecked = true
            }
            else -> {
                // More than 2 SIMs (rare) — show first two only
                binding.cgSimSelector.visibility = View.VISIBLE
                binding.chipSim1.visibility = View.VISIBLE
                binding.chipSim2.visibility = View.VISIBLE

                val label1 = buildSimLabel(1, subscriptions[0].number)
                val label2 = buildSimLabel(2, subscriptions[1].number)

                binding.chipSim1.text = label1
                binding.chipSim2.text = label2
                binding.chipSim1.isChecked = true
            }
        }
    }

    /** Builds a chip label like "شريحة 1 • 0100xxxxxxx" or just "شريحة 1" if no number. */
    private fun buildSimLabel(slotIndex: Int, phoneNumber: String?): String {
        val base = "شريحة $slotIndex"
        return if (!phoneNumber.isNullOrBlank()) "$base  •  $phoneNumber" else base
    }

    private fun setupKeypad() {
        val numberBuilder = StringBuilder()

        val appendDigit = { digit: String ->
            numberBuilder.append(digit)
            binding.etPhoneNumber.setText(numberBuilder.toString())
        }

        binding.btn0.setOnClickListener { appendDigit("0") }
        binding.btn1.setOnClickListener { appendDigit("1") }
        binding.btn2.setOnClickListener { appendDigit("2") }
        binding.btn3.setOnClickListener { appendDigit("3") }
        binding.btn4.setOnClickListener { appendDigit("4") }
        binding.btn5.setOnClickListener { appendDigit("5") }
        binding.btn6.setOnClickListener { appendDigit("6") }
        binding.btn7.setOnClickListener { appendDigit("7") }
        binding.btn8.setOnClickListener { appendDigit("8") }
        binding.btn9.setOnClickListener { appendDigit("9") }
        binding.btnStar.setOnClickListener { appendDigit("*") }
        binding.btnHash.setOnClickListener { appendDigit("#") }

        binding.btn0.setOnLongClickListener {
            appendDigit("+")
            true
        }

        binding.btnBackspace.setOnClickListener {
            if (numberBuilder.isNotEmpty()) {
                numberBuilder.deleteCharAt(numberBuilder.length - 1)
                binding.etPhoneNumber.setText(numberBuilder.toString())
            }
        }

        binding.btnBackspace.setOnLongClickListener {
            numberBuilder.clear()
            binding.etPhoneNumber.setText("")
            true
        }

        binding.btnCall.setOnClickListener {
            val phone = numberBuilder.toString().trim()
            if (phone.isNotEmpty()) {
                makeCall(phone)
            }
        }
    }

    private fun makeCall(phone: String) {
        val context = requireContext()
        val uri = Uri.parse("tel:${Uri.encode(phone)}")
        val selectedHandle: PhoneAccountHandle? = if (binding.chipSim2.isChecked && phoneAccountHandles.size > 1) {
            phoneAccountHandles[1]
        } else if (phoneAccountHandles.isNotEmpty()) {
            phoneAccountHandles[0]
        } else null

        if (PermissionHelper.hasPermission(context, Manifest.permission.CALL_PHONE)) {
            val intent = Intent(Intent.ACTION_CALL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (selectedHandle != null) {
                    putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, selectedHandle)
                }
            }
            try {
                startActivity(intent)
                return
            } catch (e: Exception) {
                Log.e("DialerFragment", "ACTION_CALL failed: ${e.message}")
            }
        }

        val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(dialIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

