package com.bdaey.volumeup.ui.dialer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.bdaey.volumeup.databinding.FragmentDialerBinding

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
        val numberButtons = listOf<Button>(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9, binding.btnStar, binding.btnHash
        )

        for (button in numberButtons) {
            button.setOnClickListener {
                appendDigit(button.text.toString())
            }
        }

        binding.btnBackspace.setOnClickListener {
            removeLastDigit()
        }

        binding.btnBackspace.setOnLongClickListener {
            binding.etPhoneNumber.setText("")
            true
        }

        binding.btnCall.setOnClickListener {
            val phone = binding.etPhoneNumber.text.toString().trim()
            if (phone.isNotEmpty()) {
                placeCall(phone)
            }
        }
    }

    private fun appendDigit(digit: String) {
        val currentText = binding.etPhoneNumber.text.toString()
        binding.etPhoneNumber.setText(currentText + digit)
    }

    private fun removeLastDigit() {
        val currentText = binding.etPhoneNumber.text.toString()
        if (currentText.isNotEmpty()) {
            binding.etPhoneNumber.setText(currentText.substring(0, currentText.length - 1))
        }
    }

    fun placeCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: SecurityException) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
