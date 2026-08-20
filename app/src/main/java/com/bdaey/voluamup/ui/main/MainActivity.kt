package com.bdaey.voluamup.ui.main

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bdaey.voluamup.R
import com.bdaey.voluamup.databinding.ActivityMainBinding
import com.bdaey.voluamup.ui.calllog.CallLogFragment
import com.bdaey.voluamup.ui.contacts.ContactsFragment
import com.bdaey.voluamup.ui.dialer.DialerFragment
import com.bdaey.voluamup.util.PermissionHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestNotificationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    private val requestDefaultDialerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkDefaultDialerStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation(savedInstanceState)
        setupNotificationPermission()

        binding.btnSetDefaultDialer.setOnClickListener {
            showDefaultDialerRationaleAndPrompt()
        }
    }

    override fun onResume() {
        super.onResume()
        checkDefaultDialerStatus()
    }

    private fun setupNavigation(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            loadFragment(DialerFragment())
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dialer -> {
                    loadFragment(DialerFragment())
                    true
                }
                R.id.nav_contacts -> {
                    loadFragment(ContactsFragment())
                    true
                }
                R.id.nav_call_log -> {
                    loadFragment(CallLogFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun setupNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionHelper.hasPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
                requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkDefaultDialerStatus() {
        val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
            roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } else {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecomManager?.defaultDialerPackage == packageName
        }

        binding.bannerDefaultDialer.visibility = if (isDefault) View.GONE else View.VISIBLE
    }

    private fun showDefaultDialerRationaleAndPrompt() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_default_dialer_title)
            .setMessage(R.string.dialog_default_dialer_msg)
            .setPositiveButton(R.string.btn_continue) { _, _ ->
                promptSetDefaultDialer()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun promptSetDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                requestDefaultDialerLauncher.launch(intent)
            }
        } else {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    packageName
                )
            }
            requestDefaultDialerLauncher.launch(intent)
        }
    }
}
