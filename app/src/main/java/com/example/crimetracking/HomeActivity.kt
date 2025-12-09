package com.example.crimetracking

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.crimetracking.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityHomeBinding
    private lateinit var smsHelper: SmsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        smsHelper = SmsHelper(this)

        // Display user's email or name in welcome message
        val user = auth.currentUser
        if (user != null) {
            val displayName = user.displayName ?: user.email?.substringBefore("@") ?: "User"
            binding.tvWelcome.text = displayName
        }

        // Map button click handler
        binding.btnMap.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Crime Reports button click handler
        binding.btnReports.setOnClickListener {
            Toast.makeText(this, "Crime Reports - Coming Soon", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to ReportsActivity
            // startActivity(Intent(this, ReportsActivity::class.java))
        }

        // Community Feed button click handler
        binding.btnCommunity.setOnClickListener {
            Toast.makeText(this, "Community Feed - Coming Soon", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to CommunityActivity
            // startActivity(Intent(this, CommunityActivity::class.java))
        }

        // Emergency SOS button click handler
        binding.btnSOS.setOnClickListener {
            handleSOSButtonClick()
        }

        // Logout button click handler
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Profile button click handler
        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    /**
     * Handle SOS button click with confirmation dialog
     */
    private fun handleSOSButtonClick() {
        // Show confirmation dialog
        AlertDialog.Builder(this)
            .setTitle("Emergency SOS")
            .setMessage("Send emergency alert to all your emergency contacts?")
            .setPositiveButton("Send Alert") { _, _ ->
                sendSOSAlert()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    /**
     * Send SOS alert to emergency contacts
     */
    private fun sendSOSAlert() {
        // Check if SMS permission is granted
        if (!smsHelper.hasSmsPermission()) {
            // Request permission
            AlertDialog.Builder(this)
                .setTitle("SMS Permission Required")
                .setMessage("CrimeTracking needs SMS permission to send emergency alerts to your contacts. Grant permission?")
                .setPositiveButton("Grant") { _, _ ->
                    smsHelper.requestSmsPermission(this)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    Toast.makeText(
                        this,
                        "SMS permission is required to send emergency alerts",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .show()
            return
        }

        // Show loading message
        Toast.makeText(this, "Sending emergency alerts...", Toast.LENGTH_SHORT).show()

        // Send SOS messages
        smsHelper.sendSOSToEmergencyContacts(
            onSuccess = { count ->
                AlertDialog.Builder(this)
                    .setTitle("Alert Sent")
                    .setMessage("Emergency alert sent to $count contact(s) successfully!")
                    .setPositiveButton("OK", null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show()
            },
            onError = { error ->
                AlertDialog.Builder(this)
                    .setTitle("Alert Failed")
                    .setMessage(error)
                    .setPositiveButton("OK", null)
                    .setNegativeButton("Manage Contacts") { _, _ ->
                        startActivity(Intent(this, ProfileActivity::class.java))
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            }
        )
    }

    /**
     * Handle permission request results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            SmsHelper.SMS_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "SMS permission granted. You can now send emergency alerts.", Toast.LENGTH_SHORT).show()
                    // Optionally, automatically send the alert now
                    // sendSOSAlert()
                } else {
                    Toast.makeText(
                        this,
                        "SMS permission denied. Emergency alerts won't work.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val user = auth.currentUser
        // Check if user is logged in. If not, redirect to LoginActivity.
        if (user == null || !user.isEmailVerified) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}


