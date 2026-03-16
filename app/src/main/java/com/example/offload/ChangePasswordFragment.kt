package com.example.offload

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.offload.databinding.FragmentChangePasswordBinding

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnUpdatePassword.setOnClickListener {
            val oldPass = binding.etOldPassword.text.toString().trim()
            val newPass = binding.etNewPassword.text.toString().trim()
            val confirmPass = binding.etConfirmPassword.text.toString().trim()

            // 1. Validation
            if (oldPass.isEmpty()) {
                binding.tilOldPassword.error = "Current password required"
                return@setOnClickListener
            } else {
                binding.tilOldPassword.error = null
            }

            if (newPass.isEmpty()) {
                binding.tilNewPassword.error = "New password required"
                return@setOnClickListener
            } else if (newPass.length < 6) {
                binding.tilNewPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            } else {
                binding.tilNewPassword.error = null
            }

            if (confirmPass != newPass) {
                binding.tilConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            } else {
                binding.tilConfirmPassword.error = null
            }

            // 2. Mock Update
            binding.btnUpdatePassword.isEnabled = false
            binding.btnUpdatePassword.text = "Processing..."

            // Simulate a brief delay
            binding.btnUpdatePassword.postDelayed({
                Toast.makeText(requireContext(), "Password updated successfully (Mock)!", Toast.LENGTH_SHORT).show()
                // Go back to profile
                requireActivity().onBackPressedDispatcher.onBackPressed()
                resetButtonState()
            }, 500)
        }
    }

    private fun resetButtonState() {
        binding.btnUpdatePassword.isEnabled = true
        binding.btnUpdatePassword.text = "Update Password"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}