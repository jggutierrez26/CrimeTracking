package com.example.crimetracking

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SmsHelper(private val context: Context) {

    companion object {
        const val SMS_PERMISSION_CODE = 100
        private const val TAG = "SmsHelper"
    }

    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    /**
     * Check if SMS permission is granted
     */
    fun hasSmsPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "SMS permission check: $hasPermission")
        return hasPermission
    }

    /**
     * Request SMS permission
     */
    fun requestSmsPermission(activity: Activity) {
        Log.d(TAG, "Requesting SMS permission")
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.SEND_SMS),
            SMS_PERMISSION_CODE
        )
    }

    /**
     * Send SOS messages to all emergency contacts
     */
    fun sendSOSToEmergencyContacts(
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "=== Starting SOS Alert Process ===")

        if (!hasSmsPermission()) {
            val error = "SMS permission not granted"
            Log.e(TAG, error)
            onError(error)
            return
        }

        val user = auth.currentUser
        if (user == null) {
            val error = "User not logged in"
            Log.e(TAG, error)
            onError(error)
            return
        }

        Log.d(TAG, "Fetching emergency contacts for user: ${user.uid}")

        // Fetch emergency contacts from Firestore
        db.collection("users").document(user.uid)
            .collection("emergencyContacts")
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Firestore query successful. Document count: ${documents.size()}")

                if (documents.isEmpty) {
                    val error = "No emergency contacts found. Please add contacts in your profile."
                    Log.w(TAG, error)
                    onError(error)
                    return@addOnSuccessListener
                }

                val contacts = documents.mapNotNull { doc ->
                    val contact = EmergencyContact(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        relation = doc.getString("relation") ?: "",
                        phoneNumber = doc.getString("phoneNumber") ?: ""
                    )
                    Log.d(TAG, "Loaded contact: ${contact.name} (${contact.relation}) - ${contact.phoneNumber}")
                    contact
                }

                Log.d(TAG, "Total valid contacts: ${contacts.size}")

                // Get user's name for the message
                db.collection("users").document(user.uid)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        val userName = userDoc.getString("fullName") ?: "A user"
                        Log.d(TAG, "User name: $userName")
                        sendSmsToContacts(contacts, userName, onSuccess, onError)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching user name, using default", e)
                        sendSmsToContacts(contacts, "A user", onSuccess, onError)
                    }
            }
            .addOnFailureListener { e ->
                val error = "Failed to fetch emergency contacts: ${e.message}"
                Log.e(TAG, error, e)
                onError(error)
            }
    }

    /**
     * Send SMS to multiple contacts
     */
    private fun sendSmsToContacts(
        contacts: List<EmergencyContact>,
        userName: String,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "=== Starting SMS Send Process ===")
        Log.d(TAG, "Sending to ${contacts.size} contact(s)")

        try {
            @Suppress("DEPRECATION")
            val smsManager = SmsManager.getDefault()
            var successCount = 0
            var failureCount = 0

            val message = "🚨 EMERGENCY ALERT 🚨\n\n" +
                    "$userName needs help! This is an automated emergency message from CrimeTracking app.\n\n" +
                    "Please check on them immediately!"

            Log.d(TAG, "Message to send: $message")
            Log.d(TAG, "Message length: ${message.length} characters")

            for (contact in contacts) {
                try {
                    Log.d(TAG, "Processing contact: ${contact.name} - ${contact.phoneNumber}")

                    if (contact.phoneNumber.isEmpty()) {
                        failureCount++
                        Log.w(TAG, "Empty phone number for contact: ${contact.name}")
                        continue
                    }

                    // Clean phone number (remove spaces, dashes, etc.)
                    val cleanNumber = contact.phoneNumber.replace(Regex("[^0-9+]"), "")
                    Log.d(TAG, "Cleaned phone number: $cleanNumber")

                    if (cleanNumber.isEmpty()) {
                        failureCount++
                        Log.w(TAG, "Phone number became empty after cleaning: ${contact.phoneNumber}")
                        continue
                    }

                    // Split message if it's too long (SMS limit is 160 characters)
                    val parts = smsManager.divideMessage(message)
                    Log.d(TAG, "Message split into ${parts.size} part(s)")

                    Log.d(TAG, "Attempting to send SMS to $cleanNumber")
                    smsManager.sendMultipartTextMessage(
                        cleanNumber,
                        null,
                        parts,
                        null,
                        null
                    )

                    successCount++
                    Log.d(TAG, "✓ SMS sent successfully to ${contact.name} at $cleanNumber")

                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "✗ Failed to send SMS to ${contact.name} (${contact.phoneNumber}): ${e.message}", e)
                }
            }

            Log.d(TAG, "=== SMS Send Summary ===")
            Log.d(TAG, "Total contacts: ${contacts.size}")
            Log.d(TAG, "Successful: $successCount")
            Log.d(TAG, "Failed: $failureCount")

            if (successCount > 0) {
                Log.i(TAG, "SOS Alert completed successfully!")
                onSuccess(successCount)
            } else {
                val error = "Failed to send SMS to any contacts. Check Logcat for details."
                Log.e(TAG, error)
                onError(error)
            }

            if (failureCount > 0) {
                Toast.makeText(
                    context,
                    "Some messages failed to send ($failureCount failed)",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {
            val error = "Error sending SMS: ${e.message}"
            Log.e(TAG, error, e)
            onError(error)
        }
    }

    /**
     * Send SMS to a specific phone number (for testing)
     */
    fun sendSmsToNumber(phoneNumber: String, message: String): Boolean {
        return try {
            Log.d(TAG, "Test SMS: Sending to $phoneNumber")

            if (!hasSmsPermission()) {
                Log.e(TAG, "SMS permission not granted")
                return false
            }

            @Suppress("DEPRECATION")
            val smsManager = SmsManager.getDefault()
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            Log.d(TAG, "Cleaned number: $cleanNumber")

            val parts = smsManager.divideMessage(message)
            Log.d(TAG, "Message parts: ${parts.size}")

            smsManager.sendMultipartTextMessage(
                cleanNumber,
                null,
                parts,
                null,
                null
            )
            Log.d(TAG, "Test SMS sent to $cleanNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send test SMS", e)
            false
        }
    }
}

