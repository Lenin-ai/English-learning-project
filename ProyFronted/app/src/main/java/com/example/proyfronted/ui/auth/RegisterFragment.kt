package com.example.proyfronted.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.proyfronted.R
import androidx.navigation.fragment.findNavController
import com.example.proyfronted.backend.Auth.Client.RetrofitClient
import com.example.proyfronted.backend.Auth.Model.RegisterRequest
import com.example.proyfronted.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {
    private lateinit var binding: FragmentRegisterBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Volver a Login
        binding.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        // Registrar usuario
        binding.btnRegister.setOnClickListener {
            if (validateFields()) {
                registerUser()
            }
        }
    }
    private fun registerUser() {
        val username = binding.etUsername.text.toString().replace(" ", "").lowercase() // O puedes pedir el username aparte
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        val name = binding.etName.text.toString()
        val firstName = name.split(" ").firstOrNull() ?: ""
        val lastName = name.split(" ").drop(1).joinToString(" ") // Lo que sobra del nombre

        val request = RegisterRequest(username, email, firstName, lastName, password)

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.register(request)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Registro exitoso", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                } else {
                    Toast.makeText(requireContext(), "Error al registrar", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error de red: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    private fun validateFields(): Boolean {
        val name = binding.etName.text.toString()
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (name.isEmpty()) {
            binding.etName.error = "Nombre requerido"
            return false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Email requerido"
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Contraseña requerida"
            return false
        }

        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Las contraseñas no coinciden"
            return false
        }

        return true
    }
}