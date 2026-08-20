package com.bdaey.voluamup.ui.dialer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
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

    private fun loadSimAccounts() {
        try {
            val telecomManager = requireContext().getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null && PermissionHelper.hasPermission(requireContext(), Manifest.permission.READ_PHONE_STATE)) {
                phoneAccountHandles = telecomManager.callCapablePhoneAccounts
            }
        } catch (e: Exception) {
            Log.w("DialerFragment", "Could not query call-capable phone accounts: ${e.message}")
        }
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
        val uri = Uri.fromParts("tel", phone, null)
        val selectedHandle: PhoneAccountHandle? = if (binding.chipSim2.isChecked && phoneAccountHandles.size > 1) {
            phoneAccountHandles[1]
        } else if (phoneAccountHandles.isNotEmpty()) {
            phoneAccountHandles[0]
        } else null

        if (PermissionHelper.hasPermission(context, Manifest.permission.CALL_PHONE)) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null) {
                try {
                    val extras = Bundle().apply {
                        if (selectedHandle != null) {
                            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, selectedHandle)
                        }
                    }
                    telecomManager.placeCall(uri, extras)
                    return
                } catch (e: Exception) {
                    Log.e("DialerFragment", "telecomManager.placeCall failed: ${e.message}")
                }
            }

            val intent = Intent(Intent.ACTION_CALL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (selectedHandle != null) {
                    putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, selectedHandle)
                }
            }
            startActivity(intent)
        } else {
            val intent = Intent(Intent.ACTION_DIAL, uri)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
