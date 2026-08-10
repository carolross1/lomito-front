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
import com.lomito.seguro.databinding.FragmentLoginBinding
import com.lomito.seguro.util.SessionManager

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        binding.btnLogin.setOnClickListener {
            val correo = binding.etCorreo.text.toString()
            val contrasena = binding.etContrasena.text.toString()

            if (correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = ""

            viewModel.login(correo, contrasena)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    // Ya mostramos loading
                }
                is AuthState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Iniciar sesión"

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

                    Toast.makeText(requireContext(), "Bienvenido ${usuario.nombre}", Toast.LENGTH_SHORT).show()

                    // ✅ Usar el ID correcto del nav_graph
                    findNavController().navigate(R.id.action_login_to_home)
                }
                is AuthState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Iniciar sesión"
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.tvRegistro.setOnClickListener {
            // ✅ Usar el ID correcto del nav_graph
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}