package com.lomito.seguro.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.lomito.seguro.R
import com.lomito.seguro.databinding.FragmentRegisterBinding
import com.lomito.seguro.util.SessionManager
import com.lomito.seguro.util.gone
import com.lomito.seguro.util.toast
import com.lomito.seguro.util.visible

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var session: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        binding.btnRegistrar.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            val correo = binding.etCorreo.text.toString().trim()
            val telefono = binding.etTelefono.text.toString().trim()
            val contrasena = binding.etContrasena.text.toString().trim()
            if (nombre.isEmpty() || correo.isEmpty() || contrasena.isEmpty()) {
                toast("Nombre, correo y contraseña son requeridos")
                return@setOnClickListener
            }
            viewModel.register(nombre, correo, telefono, contrasena)
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.progressBar.visible()
                    binding.btnRegistrar.isEnabled = false
                }
                is AuthState.Success -> {
                    binding.progressBar.gone()
                    session.saveUser(state.usuario)
                    findNavController().navigate(R.id.action_register_to_home)
                }
                is AuthState.Error -> {
                    binding.progressBar.gone()
                    binding.btnRegistrar.isEnabled = true
                    toast(state.message)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
