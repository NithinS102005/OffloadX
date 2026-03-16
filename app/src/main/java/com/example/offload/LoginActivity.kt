package com.example.offload

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // --- Auto-login: Mock check ---
        val prefs = getSharedPreferences("OffloadXPrefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (isLoggedIn) {
            goToMain()
            return
        }

        // --- Link Views ---
        val tilUsername = findViewById<TextInputLayout>(R.id.tilUsername)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val etUsername  = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword  = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin    = findViewById<Button>(R.id.btnLogin)
        val btnGoogle   = findViewById<Button>(R.id.btnGoogleSignIn)
        val tvSignUp    = findViewById<android.widget.TextView>(R.id.tvSignUp)
        val tvForgot    = findViewById<android.widget.TextView>(R.id.tvForgot)

        // Hide Google Sign-In as it's no longer used
        btnGoogle.visibility = android.view.View.GONE

        // --- Navigation links ---
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        tvForgot.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // --- Email/Password Mock Sign In Button ---
        btnLogin.setOnClickListener {
            // Clear previous errors
            tilUsername.error = null
            tilPassword.error = null

            val email    = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            // 1. Empty field checks
            if (email.isEmpty()) {
                tilUsername.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                tilPassword.error = "Password is required"
                return@setOnClickListener
            }

            // 2. Mock Sign In (Accept any credentials)
            btnLogin.isEnabled = false
            btnLogin.text = "Signing In..."

            // Simulate a brief delay then log in
            btnLogin.postDelayed({
                Toast.makeText(this, "Welcome back (Guest)!", Toast.LENGTH_SHORT).show()
                
                // Save login state locally
                prefs.edit().apply {
                    putBoolean("is_logged_in", true)
                    putString("user_email", email)
                    putString("user_name", email.substringBefore("@"))
                    apply()
                }
                
                goToMain()
                btnLogin.isEnabled = true
                btnLogin.text = "Sign In"
            }, 500)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}