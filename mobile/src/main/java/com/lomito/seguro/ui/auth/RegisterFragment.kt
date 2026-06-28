package com.lomito.seguro.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.lomito.seguro.MainActivity
import com.lomito.seguro.R
import com.lomito.seguro.databinding.FragmentRegisterBinding
import com.lomito.seguro.util.SessionManager

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        binding.btnRegistrar.setOnClickListener {
            val nombre = binding.etNombre.text.toString()
            val correo = binding.etCorreo.text.toString()
            val telefono = binding.etTelefono.text.toString()
            val contrasena = binding.etContrasena.text.toString()

            if (nombre.isEmpty() || correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.btnRegistrar.isEnabled = false
            binding.btnRegistrar.text = ""

            viewModel.register(nombre, correo, telefono, contrasena)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    // Ya mostramos loading
                }
                is AuthState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegistrar.isEnabled = true
                    binding.btnRegistrar.text = "Crear cuenta"

                    val usuario = state.usuario

                    // ✅ Guardar en SessionManager
                    sessionManager.saveUser(
                        id = usuario.id,
                        nombre = usuario.nombre,
                        correo = usuario.correo
                    )

                    // ✅ Enviar userId al watch
                    val activity = requireActivity() as? MainActivity
                    activity?.actualizarUserIdEnWatch(usuario.id)

                    Toast.makeText(requireContext(), "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()

                    // ✅ Usar el ID correcto del nav_graph (ir al home directamente)
                    findNavController().navigate(R.id.action_register_to_home)
                }
                is AuthState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegistrar.isEnabled = true
                    binding.btnRegistrar.text = "Crear cuenta"
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.tvLogin.setOnClickListener {
            // ✅ Ir al login
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}