package com.bdaey.voluamup.ui.contacts

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bdaey.voluamup.R
import com.bdaey.voluamup.databinding.FragmentContactsBinding
import com.bdaey.voluamup.model.Contact
import com.bdaey.voluamup.util.PermissionHelper

class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var adapter: ContactsAdapter
    private var allContacts: List<Contact> = emptyList()

    private val requestContactsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.loadContacts()
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_CONTACTS)) {
                    PermissionHelper.showSettingsRedirectDialog(
                        requireActivity(),
                        getString(R.string.permission_contacts_title),
                        getString(R.string.permission_contacts_permanent_denial)
                    )
                }
                binding.tvEmptyContacts.visibility = View.VISIBLE
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ContactsAdapter(emptyList()) { contact ->
            makeCall(contact.phoneNumber)
        }

        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvContacts.adapter = adapter

        setupSearch()
        observeViewModel()
        checkAndRequestContactsPermission()
    }

    private fun checkAndRequestContactsPermission() {
        if (PermissionHelper.hasPermission(requireContext(), Manifest.permission.READ_CONTACTS)) {
            viewModel.loadContacts()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_CONTACTS)) {
                PermissionHelper.showRationaleDialog(
                    requireActivity(),
                    getString(R.string.permission_contacts_title),
                    getString(R.string.permission_contacts_rationale),
                    onPositive = {
                        requestContactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                    },
                    onNegative = {
                        binding.tvEmptyContacts.visibility = View.VISIBLE
                    }
                )
            } else {
                requestContactsLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.contacts.observe(viewLifecycleOwner) { contacts ->
            allContacts = contacts
            updateUI(contacts)
        }
    }

    private fun setupSearch() {
        binding.searchViewContacts.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterContacts(newText.orEmpty())
                return true
            }
        })
    }

    private fun filterContacts(query: String) {
        if (query.isEmpty()) {
            updateUI(allContacts)
            return
        }
        val filtered = allContacts.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.phoneNumber.contains(query) ||
                    (it.emailAddress?.contains(query, ignoreCase = true) == true)
        }
        updateUI(filtered)
    }

    private fun updateUI(list: List<Contact>) {
        if (list.isEmpty()) {
            binding.tvEmptyContacts.visibility = View.VISIBLE
            binding.rvContacts.visibility = View.GONE
        } else {
            binding.tvEmptyContacts.visibility = View.GONE
            binding.rvContacts.visibility = View.VISIBLE
            adapter.updateData(list)
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
