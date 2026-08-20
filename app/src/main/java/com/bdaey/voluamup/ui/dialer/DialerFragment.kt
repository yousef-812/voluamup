package com.bdaey.voluamup.ui.dialer

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bdaey.voluamup.databinding.FragmentDialerBinding
import com.bdaey.voluamup.util.PermissionHelper

class DialerFragment : Fragment() {

    private var _binding: FragmentDialerBinding? = null
    private val binding get() = _binding!!

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
        if (PermissionHelper.hasPermission(requireContext(), Manifest.permission.CALL_PHONE)) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } else {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
