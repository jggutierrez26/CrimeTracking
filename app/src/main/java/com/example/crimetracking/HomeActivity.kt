package com.example.crimetracking

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.crimetracking.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

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
            Toast.makeText(this, "Emergency SOS - Coming Soon", Toast.LENGTH_SHORT).show()
            // TODO: Implement SOS functionality
            // This could trigger emergency contacts, location sharing, etc.
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