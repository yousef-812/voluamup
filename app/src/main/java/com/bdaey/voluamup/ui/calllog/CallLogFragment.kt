package com.bdaey.voluamup.ui.calllog

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bdaey.voluamup.R
import com.bdaey.voluamup.databinding.FragmentCallLogBinding
import com.bdaey.voluamup.model.CallLogItem
import com.bdaey.voluamup.util.PermissionHelper

class CallLogFragment : Fragment() {

    private var _binding: FragmentCallLogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CallLogViewModel by viewModels()
    private lateinit var adapter: CallLogAdapter

    private val requestCallLogLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.loadCallLogs()
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_CALL_LOG)) {
                    PermissionHelper.showSettingsRedirectDialog(
                        requireActivity(),
                        getString(R.string.permission_call_log_title),
                        getString(R.string.permission_call_log_permanent_denial)
                    )
                }
                binding.tvEmptyCallLog.visibility = View.VISIBLE
            }
        }

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
        checkAndRequestCallLogPermission()
    }

    private fun checkAndRequestCallLogPermission() {
        if (PermissionHelper.hasPermission(requireContext(), Manifest.permission.READ_CALL_LOG)) {
            viewModel.loadCallLogs()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_CALL_LOG)) {
                PermissionHelper.showRationaleDialog(
                    requireActivity(),
                    getString(R.string.permission_call_log_title),
                    getString(R.string.permission_call_log_rationale),
                    onPositive = {
                        requestCallLogLauncher.launch(Manifest.permission.READ_CALL_LOG)
                    },
                    onNegative = {
                        binding.tvEmptyCallLog.visibility = View.VISIBLE
                    }
                )
            } else {
                requestCallLogLauncher.launch(Manifest.permission.READ_CALL_LOG)
            }
        }
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
