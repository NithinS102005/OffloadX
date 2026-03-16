package com.example.offload

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import android.widget.EditText
import android.widget.LinearLayout
import com.google.android.material.imageview.ShapeableImageView

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            view?.findViewById<ShapeableImageView>(R.id.ivProfilePic)?.setImageURI(uri)
            try {
                // Copy the picture to internal storage so we have persistent access
                val inputStream = requireActivity().contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val prefs = requireActivity().getSharedPreferences("OffloadXPrefs", android.content.Context.MODE_PRIVATE)
                    
                    // delete old file if it exists to clean up disk space
                    val oldUri = prefs.getString("pfp_uri", null)
                    if (oldUri != null && oldUri.startsWith("/")) {
                        java.io.File(oldUri).delete()
                    }
                
                    val fileName = "profile_pic_${System.currentTimeMillis()}.jpg"
                    val file = java.io.File(requireContext().filesDir, fileName)
                    val outputStream = java.io.FileOutputStream(file)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        view?.findViewById<ShapeableImageView>(R.id.ivProfilePic)?.setImageBitmap(bitmap)
                    }
                    
                    prefs.edit().putString("pfp_uri", file.absolutePath).apply()
                } else {
                    Toast.makeText(context, "Could not open selected image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to save profile picture permanently", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvUserName  = view.findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail   = view.findViewById<TextView>(R.id.tvUserEmail)
        val switch        = view.findViewById<SwitchCompat>(R.id.switchDarkMode)
        val btnPass       = view.findViewById<LinearLayout>(R.id.btnChangePasswordLayout)
        val btnLogout     = view.findViewById<Button>(R.id.btnLogout)
        val ivProfilePic  = view.findViewById<ShapeableImageView>(R.id.ivProfilePic)
        val btnEditProfile = view.findViewById<LinearLayout>(R.id.btnEditProfileLayout)
        val btnDeleteAccount = view.findViewById<Button>(R.id.btnDeleteAccount)

        val prefs = requireActivity().getSharedPreferences("OffloadXPrefs", android.content.Context.MODE_PRIVATE)

        // --- Profile Picture Logic ---
        val savedUri = prefs.getString("pfp_uri", null)
        if (savedUri != null) {
            try {
                if (savedUri.startsWith("/")) { // It's an internal file path
                    val bitmap = android.graphics.BitmapFactory.decodeFile(savedUri)
                    if (bitmap != null) {
                        ivProfilePic.setImageBitmap(bitmap)
                    } else {
                        ivProfilePic.setImageURI(Uri.fromFile(java.io.File(savedUri)))
                    }
                } else { // Fallback for old URI style
                    ivProfilePic.setImageURI(Uri.parse(savedUri))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        ivProfilePic.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // --- Show REAL user data from SharedPreferences (Mock Auth) ---
        tvUserName.text  = prefs.getString("user_name", "User")
        tvUserEmail.text = prefs.getString("user_email", "No email")

        // --- Dark Mode Toggle ---
        val isDark = prefs.getBoolean("dark_mode", AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
        switch.isChecked = isDark

        switch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // --- Change Password (Navigate to Fragment) ---
        btnPass.setOnClickListener {
            try {
                findNavController().navigate(R.id.changePasswordFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Navigation Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // --- Edit Profile Name ---
        btnEditProfile.setOnClickListener {
            val input = EditText(requireContext())
            input.setText(tvUserName.text.toString())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            input.layoutParams = lp

            AlertDialog.Builder(requireContext())
                .setTitle("Edit Display Name")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val newName = input.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        prefs.edit().putString("user_name", newName).apply()
                        tvUserName.text = newName
                        Toast.makeText(context, "Name updated!", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // --- Delete Account (Mock) ---
        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you absolutely sure? This action is permanent and cannot be undone.")
                .setPositiveButton("Delete Forever") { _, _ ->
                    // Clear data
                    prefs.edit().clear().apply()

                    Toast.makeText(context, "Account deleted successfully.", Toast.LENGTH_LONG).show()

                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // --- Logout (Mock) ---
        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to exit the app and log out?")
                .setPositiveButton("Logout") { _, _ ->
                    // 1. Clear session state
                    prefs.edit().putBoolean("is_logged_in", false).apply()

                    // 2. Navigate to Login Activity
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}