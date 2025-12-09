package com.example.crimetracking

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private lateinit var fullName: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var btnCreate: Button
    private lateinit var tvSignIn: TextView

    private var passwordVisible = false
    private var confirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()

        fullName = findViewById(R.id.etFullName)
        email = findViewById(R.id.etEmail)
        password = findViewById(R.id.etPassword)
        confirmPassword = findViewById(R.id.etConfirmPassword)
        btnCreate = findViewById(R.id.btnCreateAccount)
        tvSignIn = findViewById(R.id.tvSignIn)

        // Toggle password icons
        setupPasswordToggle(password, true)
        setupPasswordToggle(confirmPassword, false)

        btnCreate.setOnClickListener {
            validateAndCreateAccount()
        }

        tvSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    @Suppress("ClickableViewAccessibility")
    private fun setupPasswordToggle(editText: EditText, isMainPassword: Boolean) {
        editText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                if (editText.compoundDrawables[drawableEnd] != null) {
                    if (event.rawX >= (editText.right - editText.compoundDrawables[drawableEnd].bounds.width())) {
                        if (isMainPassword) passwordVisible = !passwordVisible
                        else confirmPasswordVisible = !confirmPasswordVisible

                        editText.inputType =
                            InputType.TYPE_CLASS_TEXT or
                                    if ((if (isMainPassword) passwordVisible else confirmPasswordVisible))
                                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                                    else InputType.TYPE_TEXT_VARIATION_PASSWORD

                        editText.setSelection(editText.text.length)
                        v.performClick()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }

    private fun validateAndCreateAccount() {
        val fullNameText = fullName.text.toString().trim()
        val emailText = email.text.toString().trim()
        val passwordText = password.text.toString()
        val confirmPasswordText = confirmPassword.text.toString()

        // Reset errors
        fullName.error = null
        email.error = null
        password.error = null
        confirmPassword.error = null

        when {
            fullNameText.isEmpty() -> {
                fullName.error = "Full name required"
                fullName.requestFocus()
            }

            emailText.isEmpty() -> {
                email.error = "Email required"
                email.requestFocus()
            }

            !Patterns.EMAIL_ADDRESS.matcher(emailText).matches() -> {
                email.error = "Invalid email format"
                email.requestFocus()
            }

            passwordText.length < 6 -> {
                password.error = "Password must be at least 6 characters"
                password.requestFocus()
            }

            passwordText != confirmPasswordText -> {
                confirmPassword.error = "Passwords do not match"
                confirmPassword.requestFocus()
            }

            else -> {
                // Valid form, create account
                createAccount(fullNameText, emailText, passwordText)
            }
        }
    }

    private fun createAccount(fullNameText: String, emailText: String, passwordText: String) {
        btnCreate.isEnabled = false
        btnCreate.text = "Creating..."

        auth.createUserWithEmailAndPassword(emailText, passwordText)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Send email verification
                    auth.currentUser?.sendEmailVerification()

                    // Save user profile to Firestore
                    val uid = auth.currentUser!!.uid
                    val userProfile = hashMapOf(
                        "fullName" to fullNameText,
                        "email" to emailText,
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("users").document(uid).set(userProfile)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Account created! Please verify your email.",
                                Toast.LENGTH_LONG
                            ).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Account created but profile save failed: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                } else {
                    btnCreate.isEnabled = true
                    btnCreate.text = "Create Account"
                    Toast.makeText(
                        this,
                        "Sign up failed: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
