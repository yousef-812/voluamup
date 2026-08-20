package com.example.volumeup.ui.calllog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.volumeup.databinding.FragmentCallLogBinding
import com.example.volumeup.model.CallLogItem

class CallLogFragment : Fragment() {

    private var _binding: FragmentCallLogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CallLogViewModel by viewModels()
    private lateinit var adapter: CallLogAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCallLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CallLogAdapter(emptyList()) { logItem ->
            makeCall(logItem.number)
        }

        binding.rvCallLog.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCallLog.adapter = adapter

        observeViewModel()
        loadCallLogIfPermitted()
    }

    private fun observeViewModel() {
        viewModel.callLogs.observe(viewLifecycleOwner) { logs ->
            if (logs.isEmpty()) {
                binding.tvEmptyCallLog.visibility = View.VISIBLE
                binding.rvCallLog.visibility = View.GONE
            } else {
                binding.tvEmptyCallLog.visibility = View.GONE
                binding.rvCallLog.visibility = View.VISIBLE
                adapter.updateData(logs)
            }
        }
    }

    fun loadCallLogIfPermitted() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadCallLogs()
        } else {
            binding.tvEmptyCallLog.visibility = View.VISIBLE
        }
    }

    private fun makeCall(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: SecurityException) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
