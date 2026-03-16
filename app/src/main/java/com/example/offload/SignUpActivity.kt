package com.example.offload

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val etUsername    = findViewById<TextInputEditText>(R.id.etNewUser)
        val etEmail       = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword    = findViewById<TextInputEditText>(R.id.etNewPass)
        val etConfirmPass = findViewById<TextInputEditText>(R.id.etConfirmPass)
        val btnRegister   = findViewById<Button>(R.id.btnRegister)
        val btnGoogleUp   = findViewById<Button>(R.id.btnGoogleSignUp)

        // Hide Google SignUp
        btnGoogleUp.visibility = android.view.View.GONE

        btnRegister.setOnClickListener {
            val username    = etUsername.text.toString().trim()
            val email       = etEmail.text.toString().trim()
            val password    = etPassword.text.toString()
            val confirmPass = etConfirmPass.text.toString()

            // --- VALIDATION CHECKS ---
            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (username.length < 3) {
                etUsername.error = "Username must be at least 3 characters"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Enter a valid email address"
                return@setOnClickListener
            }
            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (!password.any { it.isDigit() }) {
                etPassword.error = "Password must contain at least one number"
                return@setOnClickListener
            }
            if (password != confirmPass) {
                etConfirmPass.error = "Passwords do not match"
                return@setOnClickListener
            }

            // --- Mock Registration ---
            btnRegister.isEnabled = false
            btnRegister.text = "Creating Account..."

            btnRegister.postDelayed({
                val prefs = getSharedPreferences("OffloadXPrefs", MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_registered", true)
                    .putString("user_name", username)
                    .putString("user_email", email)
                    .apply()

                Toast.makeText(this,"Account created successfully (Mock)! Please sign in.", Toast.LENGTH_LONG).show()

                // Go back to Login
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }, 500)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}