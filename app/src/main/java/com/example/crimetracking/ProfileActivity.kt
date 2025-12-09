package com.example.crimetracking

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {

    companion object {
        private const val MAX_EMERGENCY_CONTACTS = 3
    }

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private lateinit var emergencyContactsAdapter: EmergencyContactsAdapter
    private val emergencyContacts = mutableListOf<EmergencyContact>()

    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnAddEmergencyContact: Button
    private lateinit var rvEmergencyContacts: RecyclerView
    private lateinit var tvNoContacts: TextView
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        // Initialize views
        tvFirstName = findViewById(R.id.tvFirstName)
        tvLastName = findViewById(R.id.tvLastName)
        tvEmail = findViewById(R.id.tvEmail)
        btnAddEmergencyContact = findViewById(R.id.btnAddEmergencyContact)
        rvEmergencyContacts = findViewById(R.id.rvEmergencyContacts)
        tvNoContacts = findViewById(R.id.tvNoContacts)
        btnBack = findViewById(R.id.btnBack)

        // Setup RecyclerView
        emergencyContactsAdapter = EmergencyContactsAdapter(
            emergencyContacts,
            onEditClick = { contact -> showEditEmergencyContactDialog(contact) },
            onDeleteClick = { contact -> deleteEmergencyContact(contact) }
        )
        rvEmergencyContacts.layoutManager = LinearLayoutManager(this)
        rvEmergencyContacts.adapter = emergencyContactsAdapter

        // Load user data
        loadUserProfile()
        loadEmergencyContacts()

        // Set click listeners
        btnBack.setOnClickListener {
            finish()
        }

        btnAddEmergencyContact.setOnClickListener {
            if (emergencyContacts.size >= MAX_EMERGENCY_CONTACTS) {
                AlertDialog.Builder(this)
                    .setTitle("Maximum Contacts Reached")
                    .setMessage("You can only have up to $MAX_EMERGENCY_CONTACTS emergency contacts. Please delete a contact before adding a new one.")
                    .setPositiveButton("OK", null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show()
            } else {
                showAddEmergencyContactDialog()
            }
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            // Display email
            tvEmail.text = user.email

            // Load full name from Firestore
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val fullName = document.getString("fullName") ?: ""
                        val nameParts = fullName.split(" ", limit = 2)

                        if (nameParts.isNotEmpty()) {
                            tvFirstName.text = nameParts[0]
                            if (nameParts.size > 1) {
                                tvLastName.text = nameParts[1]
                            } else {
                                tvLastName.text = ""
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadEmergencyContacts() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid)
                .collection("emergencyContacts")
                .get()
                .addOnSuccessListener { documents ->
                    emergencyContacts.clear()
                    android.util.Log.d("ProfileActivity", "Loaded ${documents.size()} emergency contacts")
                    for (document in documents) {
                        val contact = EmergencyContact(
                            id = document.id,
                            name = document.getString("name") ?: "",
                            relation = document.getString("relation") ?: "",
                            phoneNumber = document.getString("phoneNumber") ?: ""
                        )
                        emergencyContacts.add(contact)
                        android.util.Log.d("ProfileActivity", "Contact: ${contact.name}, ${contact.relation}, ${contact.phoneNumber}")
                    }
                    emergencyContactsAdapter.notifyDataSetChanged()
                    updateEmptyState()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("ProfileActivity", "Error loading contacts", e)
                    Toast.makeText(this, "Error loading contacts: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateEmptyState() {
        android.util.Log.d("ProfileActivity", "updateEmptyState called. Contact count: ${emergencyContacts.size}")
        if (emergencyContacts.isEmpty()) {
            rvEmergencyContacts.visibility = View.GONE
            tvNoContacts.visibility = View.VISIBLE
            android.util.Log.d("ProfileActivity", "Showing empty state")
        } else {
            rvEmergencyContacts.visibility = View.VISIBLE
            tvNoContacts.visibility = View.GONE
            android.util.Log.d("ProfileActivity", "Showing RecyclerView with ${emergencyContacts.size} contacts")
        }

        // Update button state based on contact limit
        if (emergencyContacts.size >= MAX_EMERGENCY_CONTACTS) {
            btnAddEmergencyContact.isEnabled = false
            btnAddEmergencyContact.alpha = 0.5f
            android.util.Log.d("ProfileActivity", "Maximum contacts reached ($MAX_EMERGENCY_CONTACTS), add button disabled")
        } else {
            btnAddEmergencyContact.isEnabled = true
            btnAddEmergencyContact.alpha = 1.0f
        }
    }

    private fun showAddEmergencyContactDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_emergency_contact, null)
        val etContactName = dialogView.findViewById<EditText>(R.id.etContactName)
        val etContactRelation = dialogView.findViewById<EditText>(R.id.etContactRelation)
        val etContactPhone = dialogView.findViewById<EditText>(R.id.etContactPhone)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Add", null) // Set to null initially
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Override the positive button to prevent auto-dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = etContactName.text.toString().trim()
            val relation = etContactRelation.text.toString().trim()
            val phone = etContactPhone.text.toString().trim()

            if (name.isEmpty()) {
                etContactName.error = "Name is required"
                return@setOnClickListener
            }

            if (relation.isEmpty()) {
                etContactRelation.error = "Relation is required"
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                etContactPhone.error = "Phone number is required"
                return@setOnClickListener
            }

            addEmergencyContact(name, relation, phone)
            dialog.dismiss()
        }
    }

    private fun addEmergencyContact(name: String, relation: String, phoneNumber: String) {
        val user = auth.currentUser
        if (user != null) {
            val contactData = hashMapOf(
                "name" to name,
                "relation" to relation,
                "phoneNumber" to phoneNumber,
                "createdAt" to System.currentTimeMillis()
            )

            android.util.Log.d("ProfileActivity", "Adding contact: $name, $relation, $phoneNumber")

            db.collection("users").document(user.uid)
                .collection("emergencyContacts")
                .add(contactData)
                .addOnSuccessListener { documentReference ->
                    android.util.Log.d("ProfileActivity", "Contact added successfully with ID: ${documentReference.id}")
                    val newContact = EmergencyContact(
                        id = documentReference.id,
                        name = name,
                        relation = relation,
                        phoneNumber = phoneNumber
                    )
                    emergencyContacts.add(newContact)
                    emergencyContactsAdapter.notifyItemInserted(emergencyContacts.size - 1)
                    updateEmptyState()
                    android.util.Log.d("ProfileActivity", "Total contacts now: ${emergencyContacts.size}")
                    Toast.makeText(this, "Emergency contact added", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("ProfileActivity", "Error adding contact", e)
                    Toast.makeText(this, "Error adding contact: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            android.util.Log.e("ProfileActivity", "User is null, cannot add contact")
        }
    }

    private fun showEditEmergencyContactDialog(contact: EmergencyContact) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_emergency_contact, null)
        val etContactName = dialogView.findViewById<EditText>(R.id.etContactName)
        val etContactRelation = dialogView.findViewById<EditText>(R.id.etContactRelation)
        val etContactPhone = dialogView.findViewById<EditText>(R.id.etContactPhone)

        // Pre-fill with existing data
        etContactName.setText(contact.name)
        etContactRelation.setText(contact.relation)
        etContactPhone.setText(contact.phoneNumber)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Override the positive button to prevent auto-dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = etContactName.text.toString().trim()
            val relation = etContactRelation.text.toString().trim()
            val phone = etContactPhone.text.toString().trim()

            if (name.isEmpty()) {
                etContactName.error = "Name is required"
                return@setOnClickListener
            }

            if (relation.isEmpty()) {
                etContactRelation.error = "Relation is required"
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                etContactPhone.error = "Phone number is required"
                return@setOnClickListener
            }

            updateEmergencyContact(contact.id, name, relation, phone)
            dialog.dismiss()
        }
    }

    private fun updateEmergencyContact(contactId: String, name: String, relation: String, phoneNumber: String) {
        val user = auth.currentUser
        if (user != null) {
            val contactData = hashMapOf(
                "name" to name,
                "relation" to relation,
                "phoneNumber" to phoneNumber,
                "updatedAt" to System.currentTimeMillis()
            )

            android.util.Log.d("ProfileActivity", "Updating contact: $contactId")

            db.collection("users").document(user.uid)
                .collection("emergencyContacts")
                .document(contactId)
                .update(contactData as Map<String, Any>)
                .addOnSuccessListener {
                    android.util.Log.d("ProfileActivity", "Contact updated successfully")
                    // Update the contact in the list
                    val position = emergencyContacts.indexOfFirst { it.id == contactId }
                    if (position != -1) {
                        emergencyContacts[position] = EmergencyContact(
                            id = contactId,
                            name = name,
                            relation = relation,
                            phoneNumber = phoneNumber
                        )
                        emergencyContactsAdapter.notifyItemChanged(position)
                        Toast.makeText(this, "Contact updated", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("ProfileActivity", "Error updating contact", e)
                    Toast.makeText(this, "Error updating contact: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            android.util.Log.e("ProfileActivity", "User is null, cannot update contact")
        }
    }

    private fun deleteEmergencyContact(contact: EmergencyContact) {
        AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete ${contact.name}?")
            .setPositiveButton("Delete") { _, _ ->
                val user = auth.currentUser
                if (user != null) {
                    db.collection("users").document(user.uid)
                        .collection("emergencyContacts")
                        .document(contact.id)
                        .delete()
                        .addOnSuccessListener {
                            val position = emergencyContacts.indexOf(contact)
                            if (position != -1) {
                                emergencyContacts.removeAt(position)
                                emergencyContactsAdapter.notifyItemRemoved(position)
                                updateEmptyState()
                                Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error deleting contact: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

// RecyclerView Adapter for Emergency Contacts
class EmergencyContactsAdapter(
    private val contacts: MutableList<EmergencyContact>,
    private val onEditClick: (EmergencyContact) -> Unit,
    private val onDeleteClick: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<EmergencyContactsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvContactName)
        val tvRelation: TextView = view.findViewById(R.id.tvContactRelation)
        val tvPhone: TextView = view.findViewById(R.id.tvContactPhone)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditContact)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteContact)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.tvName.text = contact.name
        holder.tvRelation.text = contact.relation
        holder.tvPhone.text = contact.phoneNumber

        holder.btnEdit.setOnClickListener {
            onEditClick(contact)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(contact)
        }
    }

    override fun getItemCount() = contacts.size
}
