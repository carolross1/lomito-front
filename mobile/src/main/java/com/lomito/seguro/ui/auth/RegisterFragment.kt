// Paquete: com.lomito.seguro.ui.auth
package com.lomito.seguro.ui.auth

// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: LayoutInflater
import android.view.LayoutInflater
// Importa componentes de la interfaz gráfica
import android.view.View
// Importa componentes de la interfaz gráfica
import android.view.ViewGroup
// Importa la dependencia necesaria: Toast
import android.widget.Toast
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa componente de navegación
import androidx.navigation.fragment.findNavController
// Importa la dependencia necesaria: MainActivity
import com.lomito.seguro.MainActivity
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentRegisterBinding
// Importa la dependencia necesaria: SessionManager
import com.lomito.seguro.util.SessionManager

// Fragment RegisterFragment: componente de UI que representa una sección de la pantalla
class RegisterFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentRegisterBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnRegistrar.setOnClickListener {
            // Constante nombre: valor inmutable que no cambia tras su asignación
            val nombre = binding.etNombre.text.toString()
            // Constante correo: valor inmutable que no cambia tras su asignación
            val correo = binding.etCorreo.text.toString()
            // Constante telefono: valor inmutable que no cambia tras su asignación
            val telefono = binding.etTelefono.text.toString()
            // Constante contrasena: valor inmutable que no cambia tras su asignación
            val contrasena = binding.etContrasena.text.toString()

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (nombre.isEmpty() || correo.isEmpty() || contrasena.isEmpty()) {
                // Muestra un mensaje emergente breve al usuario
                Toast.makeText(requireContext(), "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Actualiza el componente de UI a través del View Binding
            binding.progressBar.visibility = View.VISIBLE
            // Actualiza el componente de UI a través del View Binding
            binding.btnRegistrar.isEnabled = false
            // Actualiza el componente de UI a través del View Binding
            binding.btnRegistrar.text = ""

            viewModel.register(nombre, correo, telefono, contrasena)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            when (state) {
                is AuthState.Loading -> {
                    // Ya mostramos loading
                }
                is AuthState.Success -> {
                    // Actualiza el componente de UI a través del View Binding
                    binding.progressBar.visibility = View.GONE
                    // Actualiza el componente de UI a través del View Binding
                    binding.btnRegistrar.isEnabled = true
                    // Actualiza el componente de UI a través del View Binding
                    binding.btnRegistrar.text = "Crear cuenta"

                    // Constante usuario: valor inmutable que no cambia tras su asignación
                    val usuario = state.usuario

                    // ✅ Guardar en SessionManager
                    sessionManager.saveUser(
                        id = usuario.id,
                        nombre = usuario.nombre,
                        correo = usuario.correo
                    )

                    // ✅ Enviar userId al watch
                    // Constante activity: valor inmutable que no cambia tras su asignación
                    val activity = requireActivity() as? MainActivity
                    activity?.actualizarUserIdEnWatch(usuario.id)

                    // Muestra un mensaje emergente breve al usuario
                    Toast.makeText(requireContext(), "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()

                    // ✅ Usar el ID correcto del nav_graph (ir al home directamente)
                    // Navega hacia el destino especificado en el grafo de navegación
                    findNavController().navigate(R.id.action_register_to_home)
                }
                is AuthState.Error -> {
                    // Actualiza el componente de UI a través del View Binding
                    binding.progressBar.visibility = View.GONE
                    // Actualiza el componente de UI a través del View Binding
                    binding.btnRegistrar.isEnabled = true
                    // Actualiza el componente de UI a través del View Binding
                    binding.btnRegistrar.text = "Crear cuenta"
                    // Muestra un mensaje emergente breve al usuario
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Accede a un componente de UI a través del View Binding type-safe
        binding.tvLogin.setOnClickListener {
            // ✅ Ir al login
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    // Sobreescribe la función onDestroyView de la clase padre
    override fun onDestroyView() {
        // Invoca la implementación del método en la clase padre
        super.onDestroyView()
        _binding = null
    }
}